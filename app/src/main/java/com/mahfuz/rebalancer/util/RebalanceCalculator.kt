package com.mahfuz.rebalancer.util

import com.mahfuz.rebalancer.data.model.*
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Portfolio Rebalancing Calculator
 *
 * This class calculates the trades needed to rebalance a portfolio
 * to match target allocations, taking into account:
 * - Current holdings and their USDT values
 * - Target percentages for each coin
 * - Threshold for triggering rebalance
 * - USDT usage during trades
 * - Minimum order amounts
 */
class RebalanceCalculator(
    private val minOrderValueUsdt: BigDecimal = BigDecimal("1.0") // Minimum order value in USDT
) {
    companion object {
        private val HUNDRED = BigDecimal("100")
        private val MATH_CONTEXT = MathContext(16, RoundingMode.HALF_UP)
    }

    /**
     * Calculate current portfolio state with deviations from targets
     */
    fun calculatePortfolioState(
        balances: Map<String, BigDecimal>, // coin -> balance amount
        prices: Map<String, BigDecimal>,   // coin -> USDT price (USDT price = 1)
        targetPercentages: Map<String, BigDecimal> // coin -> target percentage
    ): PortfolioState {
        // Calculate total portfolio value in USDT
        var totalValue = BigDecimal.ZERO
        val holdings = mutableListOf<CoinHolding>()

        for ((coin, balance) in balances) {
            val price = if (coin == "USDT") BigDecimal.ONE else prices[coin] ?: continue
            val usdtValue = balance.multiply(price, MATH_CONTEXT)
            totalValue = totalValue.add(usdtValue)
        }

        if (totalValue <= BigDecimal.ZERO) {
            return PortfolioState(emptyList(), BigDecimal.ZERO)
        }

        // Calculate current percentages and deviations
        for ((coin, balance) in balances) {
            val price = if (coin == "USDT") BigDecimal.ONE else prices[coin] ?: continue
            val usdtValue = balance.multiply(price, MATH_CONTEXT)
            val currentPercentage = usdtValue.divide(totalValue, MATH_CONTEXT)
                .multiply(HUNDRED, MATH_CONTEXT)
                .setScale(4, RoundingMode.HALF_UP)

            val targetPercentage = targetPercentages[coin] ?: BigDecimal.ZERO
            val deviation = currentPercentage.subtract(targetPercentage)

            holdings.add(
                CoinHolding(
                    coin = coin,
                    balance = balance.setScale(8, RoundingMode.HALF_UP),
                    usdtValue = usdtValue.setScale(4, RoundingMode.HALF_UP),
                    currentPercentage = currentPercentage,
                    targetPercentage = targetPercentage,
                    deviation = deviation.setScale(4, RoundingMode.HALF_UP),
                    priceUsdt = price.setScale(8, RoundingMode.HALF_UP)
                )
            )
        }

        return PortfolioState(
            holdings = holdings.sortedByDescending { it.usdtValue },
            totalValueUsdt = totalValue.setScale(4, RoundingMode.HALF_UP)
        )
    }

    /**
     * Check if rebalancing is needed based on threshold
     */
    fun needsRebalancing(
        portfolioState: PortfolioState,
        threshold: BigDecimal
    ): Boolean {
        return portfolioState.holdings.any { holding ->
            holding.targetPercentage > BigDecimal.ZERO &&
                    holding.deviation.abs() >= threshold
        }
    }

    /**
     * Calculate trades needed to rebalance portfolio
     * Returns a list of trades that should be executed
     *
     * Algorithm:
     * 1. Calculate the USDT value each coin should have (target)
     * 2. Calculate the difference between current and target values
     * 3. Sort: sell overweight coins first, then buy underweight coins
     * 4. The USDT from sales will be used for purchases
     */
    fun calculateRebalanceTrades(
        portfolioState: PortfolioState,
        tradingPairs: Map<String, InstrumentInfo> // coin -> trading pair info
    ): List<RebalanceAction> {
        val trades = mutableListOf<RebalanceAction>()
        val totalValue = portfolioState.totalValueUsdt

        if (totalValue <= BigDecimal.ZERO) return emptyList()

        // Calculate target values and differences
        data class CoinDiff(
            val coin: String,
            val currentValue: BigDecimal,
            val targetValue: BigDecimal,
            val diff: BigDecimal, // positive = need to sell, negative = need to buy
            val price: BigDecimal
        )

        val diffs = portfolioState.holdings
            .filter { it.coin != "USDT" && it.targetPercentage > BigDecimal.ZERO }
            .map { holding ->
                val targetValue = totalValue.multiply(holding.targetPercentage, MATH_CONTEXT)
                    .divide(HUNDRED, MATH_CONTEXT)
                val diff = holding.usdtValue.subtract(targetValue)

                CoinDiff(
                    coin = holding.coin,
                    currentValue = holding.usdtValue,
                    targetValue = targetValue,
                    diff = diff,
                    price = holding.priceUsdt
                )
            }

        // Process sells first (positive diff means overweight, need to sell)
        val sellOrders = diffs
            .filter { it.diff > minOrderValueUsdt }
            .sortedByDescending { it.diff }

        for (order in sellOrders) {
            val symbol = "${order.coin}USDT"
            val pairInfo = tradingPairs[order.coin]

            // Calculate quantity to sell
            var sellAmount = order.diff.divide(order.price, MATH_CONTEXT)

            // Apply lot size filter if available
            pairInfo?.lotSizeFilter?.let { filter ->
                val minQty = filter.minOrderQty?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val basePrecision = filter.basePrecision?.toIntOrNull() ?: 8

                if (sellAmount < minQty) return@let

                // Round down to base precision
                sellAmount = sellAmount.setScale(basePrecision, RoundingMode.DOWN)
            }

            if (sellAmount > BigDecimal.ZERO) {
                trades.add(
                    RebalanceAction(
                        coin = order.coin,
                        action = TradeAction.SELL,
                        amount = sellAmount,
                        usdtValue = sellAmount.multiply(order.price, MATH_CONTEXT)
                            .setScale(4, RoundingMode.HALF_UP),
                        symbol = symbol
                    )
                )
            }
        }

        // Process buys (negative diff means underweight, need to buy)
        val buyOrders = diffs
            .filter { it.diff < minOrderValueUsdt.negate() }
            .sortedBy { it.diff }

        for (order in buyOrders) {
            val symbol = "${order.coin}USDT"
            val pairInfo = tradingPairs[order.coin]

            // Calculate quantity to buy (diff is negative, so negate it)
            val buyValueUsdt = order.diff.abs()
            var buyAmount = buyValueUsdt.divide(order.price, MATH_CONTEXT)

            // Apply lot size filter if available
            pairInfo?.lotSizeFilter?.let { filter ->
                val minQty = filter.minOrderQty?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val basePrecision = filter.basePrecision?.toIntOrNull() ?: 8

                if (buyAmount < minQty) return@let

                // Round down to base precision
                buyAmount = buyAmount.setScale(basePrecision, RoundingMode.DOWN)
            }

            if (buyAmount > BigDecimal.ZERO) {
                trades.add(
                    RebalanceAction(
                        coin = order.coin,
                        action = TradeAction.BUY,
                        amount = buyAmount,
                        usdtValue = buyAmount.multiply(order.price, MATH_CONTEXT)
                            .setScale(4, RoundingMode.HALF_UP),
                        symbol = symbol
                    )
                )
            }
        }

        // Also handle USDT if it has a target
        val usdtHolding = portfolioState.holdings.find { it.coin == "USDT" }
        if (usdtHolding != null && usdtHolding.targetPercentage > BigDecimal.ZERO) {
            val targetUsdtValue = totalValue.multiply(usdtHolding.targetPercentage, MATH_CONTEXT)
                .divide(HUNDRED, MATH_CONTEXT)
            val usdtDiff = usdtHolding.usdtValue.subtract(targetUsdtValue)

            // If USDT is overweight, we need to buy other coins
            // If USDT is underweight, sells have already happened
            // The trading algorithm above handles this implicitly
        }

        return trades.sortedWith(compareBy(
            { if (it.action == TradeAction.SELL) 0 else 1 }, // Sells first
            { -it.usdtValue.toDouble() } // Larger trades first
        ))
    }

    /**
     * Validate that total target percentages equal 100%
     */
    fun validateTargetPercentages(targets: Map<String, BigDecimal>): ValidationResult {
        val total = targets.values.fold(BigDecimal.ZERO) { acc, value -> acc.add(value) }

        return when {
            total < HUNDRED -> ValidationResult(
                isValid = false,
                message = "Toplam hedef yüzde 100'den az: ${total.setScale(2, RoundingMode.HALF_UP)}%"
            )
            total > HUNDRED -> ValidationResult(
                isValid = false,
                message = "Toplam hedef yüzde 100'ü aşıyor: ${total.setScale(2, RoundingMode.HALF_UP)}%"
            )
            else -> ValidationResult(isValid = true, message = "Geçerli")
        }
    }

    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}
