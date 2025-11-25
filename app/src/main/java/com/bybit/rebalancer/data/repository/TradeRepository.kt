package com.bybit.rebalancer.data.repository

import com.bybit.rebalancer.api.BybitApiService
import com.bybit.rebalancer.api.BybitWebSocket
import com.bybit.rebalancer.api.OrderRequest
import com.bybit.rebalancer.data.model.*
import com.google.gson.Gson
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * İşlem (trade) yönetimi ve rebalancing algoritması
 */
@Singleton
class TradeRepository @Inject constructor(
    private val apiService: BybitApiService,
    private val webSocket: BybitWebSocket,
    private val portfolioRepository: PortfolioRepository,
    private val logRepository: LogRepository
) {
    private val gson = Gson()

    // Instrument bilgileri cache
    private var instrumentsCache: Map<String, InstrumentInfo> = emptyMap()

    /**
     * Instrument bilgilerini yükle ve cache'le
     */
    suspend fun loadInstruments(): Result<Unit> {
        return try {
            val result = portfolioRepository.getAvailableInstruments()
            if (result.isSuccess) {
                instrumentsCache = result.getOrNull()!!.associateBy { it.symbol }
                logRepository.logInfo("Trade", "Instrument bilgileri yüklendi: ${instrumentsCache.size} çift")
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull()!!)
            }
        } catch (e: Exception) {
            logRepository.logError("Trade", "Instrument yükleme hatası: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Portföyü analiz et ve gerekli trade'leri hesapla
     * Bu fonksiyon USDT kullanımını da dikkate alarak tam hedefe ulaşmayı sağlar
     */
    suspend fun calculateRebalanceTrades(
        threshold: Double,
        minTradeUsdt: Double
    ): Result<List<RebalanceTrade>> {
        return try {
            val snapshotResult = portfolioRepository.calculatePortfolioSnapshot()
            if (snapshotResult.isFailure) {
                return Result.failure(snapshotResult.exceptionOrNull()!!)
            }

            val snapshot = snapshotResult.getOrNull()!!
            val totalValue = snapshot.totalValueUsdt

            if (totalValue <= 0) {
                logRepository.logWarning("Rebalance", "Portföy değeri 0 veya negatif")
                return Result.success(emptyList())
            }

            val trades = mutableListOf<RebalanceTrade>()

            // Her coin için sapma kontrolü yap
            for (coinSnapshot in snapshot.coins) {
                val deviation = Math.abs(coinSnapshot.deviation)

                // USDT için işlem yapmıyoruz, sadece diğer coinler için hesaplama
                if (coinSnapshot.coin == "USDT") continue

                // Eşik değerini aşıyorsa işlem gerekli
                if (deviation >= threshold) {
                    val symbol = "${coinSnapshot.coin}USDT"
                    val instrument = instrumentsCache[symbol]

                    if (instrument == null) {
                        logRepository.logWarning("Rebalance", "$symbol için instrument bulunamadı")
                        continue
                    }

                    // Hedef USDT değerini hesapla
                    val targetUsdt = totalValue * (coinSnapshot.targetPercentage / 100.0)
                    val currentUsdt = coinSnapshot.usdtValue
                    val differenceUsdt = targetUsdt - currentUsdt

                    // Minimum işlem tutarı kontrolü
                    if (Math.abs(differenceUsdt) < minTradeUsdt) {
                        logRepository.logDebug("Rebalance",
                            "${coinSnapshot.coin}: Fark (${"%.2f".format(differenceUsdt)} USDT) minimum işlem tutarından küçük")
                        continue
                    }

                    // Fiyat al
                    val priceResult = portfolioRepository.getTickerPrice(symbol)
                    if (priceResult.isFailure) {
                        logRepository.logError("Rebalance", "$symbol için fiyat alınamadı")
                        continue
                    }

                    val currentPrice = priceResult.getOrNull()!!.toDoubleOrNull() ?: continue

                    // Miktar hesapla
                    var quantity = Math.abs(differenceUsdt) / currentPrice

                    // Precision ayarla
                    val basePrecision = instrument.lotSizeFilter.basePrecision.toIntOrNull() ?: 8
                    quantity = roundToDecimalPlaces(quantity, basePrecision)

                    // Minimum miktar kontrolü
                    val minQty = instrument.lotSizeFilter.minOrderQty.toDoubleOrNull() ?: 0.0
                    if (quantity < minQty) {
                        logRepository.logDebug("Rebalance",
                            "${coinSnapshot.coin}: Miktar ($quantity) minimum miktardan ($minQty) küçük")
                        continue
                    }

                    val action = if (differenceUsdt > 0) TradeAction.BUY else TradeAction.SELL

                    trades.add(
                        RebalanceTrade(
                            coin = coinSnapshot.coin,
                            symbol = symbol,
                            action = action,
                            quantity = quantity,
                            estimatedUsdtAmount = Math.abs(differenceUsdt),
                            currentPrice = currentPrice
                        )
                    )

                    logRepository.logInfo("Rebalance",
                        "${action.name} ${coinSnapshot.coin}: ${"%.8f".format(quantity)} @ ${"%.4f".format(currentPrice)} " +
                        "(Hedef: %${"%.2f".format(coinSnapshot.targetPercentage)}, " +
                        "Mevcut: %${"%.2f".format(coinSnapshot.currentPercentage)}, " +
                        "Sapma: %${"%.2f".format(deviation)})")
                }
            }

            // İşlemleri optimize et - önce SELL, sonra BUY
            // Bu sayede USDT likidite sağlanır
            val sortedTrades = trades.sortedBy { if (it.action == TradeAction.SELL) 0 else 1 }

            Result.success(sortedTrades)
        } catch (e: Exception) {
            logRepository.logError("Rebalance", "Trade hesaplama hatası: ${e.message}", e.stackTraceToString())
            Result.failure(e)
        }
    }

    /**
     * Tek bir trade'i gerçekleştir
     * BUY: quoteCoin (USDT) cinsinden miktar gönderilir
     * SELL: baseCoin (coin) cinsinden miktar gönderilir
     */
    suspend fun executeTrade(trade: RebalanceTrade): Result<TradeLog> {
        return try {
            // İşlem öncesi portföy durumu
            val beforeSnapshot = portfolioRepository.calculatePortfolioSnapshot().getOrNull()
            val beforeJson = beforeSnapshot?.let { portfolioRepository.snapshotToJson(it) } ?: "{}"

            val orderLinkId = "rebal_${UUID.randomUUID().toString().take(8)}"

            // BUY için USDT miktarı (quoteCoin), SELL için coin miktarı (baseCoin) kullan
            val isBuy = trade.action == TradeAction.BUY
            val marketUnit = if (isBuy) "quoteCoin" else "baseCoin"
            val qty = if (isBuy) {
                // BUY: USDT miktarını gönder (2 decimal)
                formatUsdtAmount(trade.estimatedUsdtAmount)
            } else {
                // SELL: Coin miktarını gönder
                formatQuantity(trade.quantity, trade.symbol)
            }

            val orderRequest = OrderRequest(
                category = "spot",
                symbol = trade.symbol,
                side = if (isBuy) "Buy" else "Sell",
                orderType = "Market",
                qty = qty,
                marketUnit = marketUnit,
                orderLinkId = orderLinkId
            )

            logRepository.logInfo("Trade",
                "Order gönderiliyor: ${orderRequest.side} ${trade.symbol} qty=$qty ($marketUnit)")

            val response = apiService.createOrder(orderRequest)

            if (response.isSuccessful && response.body()?.retCode == 0) {
                val orderId = response.body()?.result?.orderId ?: ""

                // Order durumunu kontrol et (kısa bekleme ile)
                delay(1000)
                val orderStatus = checkOrderStatus(orderId)

                // İşlem sonrası portföy durumu
                delay(500)
                val afterSnapshot = portfolioRepository.calculatePortfolioSnapshot().getOrNull()
                val afterJson = afterSnapshot?.let { portfolioRepository.snapshotToJson(it) } ?: "{}"

                val tradeLog = TradeLog(
                    action = trade.action.name,
                    symbol = trade.symbol,
                    coin = trade.coin,
                    quantity = trade.quantity.toString(),
                    price = trade.currentPrice.toString(),
                    usdtAmount = trade.estimatedUsdtAmount.toString(),
                    portfolioBefore = beforeJson,
                    portfolioAfter = afterJson,
                    orderId = orderId,
                    status = if (orderStatus == "Filled") "SUCCESS" else orderStatus
                )

                logRepository.insertTrade(tradeLog)

                logRepository.logInfo("Trade",
                    "Order tamamlandı: ${trade.action.name} ${trade.coin} - OrderID: $orderId - Status: $orderStatus")

                Result.success(tradeLog)
            } else {
                val errorMsg = response.body()?.retMsg ?: response.message()
                logRepository.logError("Trade", "Order hatası: $errorMsg")

                val tradeLog = TradeLog(
                    action = trade.action.name,
                    symbol = trade.symbol,
                    coin = trade.coin,
                    quantity = trade.quantity.toString(),
                    price = trade.currentPrice.toString(),
                    usdtAmount = trade.estimatedUsdtAmount.toString(),
                    portfolioBefore = beforeJson,
                    portfolioAfter = beforeJson,
                    orderId = null,
                    status = "FAILED"
                )

                logRepository.insertTrade(tradeLog)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            logRepository.logError("Trade", "Trade execution hatası: ${e.message}", e.stackTraceToString())
            Result.failure(e)
        }
    }

    /**
     * Tüm trade'leri sırayla gerçekleştir
     * Önce SELL, sonra BUY yaparak USDT likidite sağla
     */
    suspend fun executeAllTrades(trades: List<RebalanceTrade>): Result<List<TradeLog>> {
        val results = mutableListOf<TradeLog>()
        var hasError = false

        logRepository.logInfo("Rebalance", "Toplam ${trades.size} işlem yapılacak")

        for ((index, trade) in trades.withIndex()) {
            logRepository.logInfo("Rebalance", "İşlem ${index + 1}/${trades.size}: ${trade.action.name} ${trade.coin}")

            val result = executeTrade(trade)
            if (result.isSuccess) {
                results.add(result.getOrNull()!!)
            } else {
                hasError = true
                logRepository.logError("Rebalance",
                    "İşlem ${index + 1} başarısız: ${result.exceptionOrNull()?.message}")
            }

            // API rate limit için bekleme
            if (index < trades.size - 1) {
                delay(500)
            }
        }

        return if (hasError && results.isEmpty()) {
            Result.failure(Exception("Tüm işlemler başarısız"))
        } else {
            logRepository.logInfo("Rebalance",
                "Rebalance tamamlandı: ${results.size}/${trades.size} işlem başarılı")
            Result.success(results)
        }
    }

    /**
     * Order durumunu kontrol et
     */
    private suspend fun checkOrderStatus(orderId: String): String {
        return try {
            val response = apiService.getOrderStatus(orderId = orderId)
            if (response.isSuccessful && response.body()?.retCode == 0) {
                response.body()?.result?.list?.firstOrNull()?.orderStatus ?: "Unknown"
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Coin miktarını uygun formata çevir (SELL için baseCoin)
     */
    private fun formatQuantity(quantity: Double, symbol: String): String {
        val instrument = instrumentsCache[symbol]
        val precision = instrument?.lotSizeFilter?.basePrecision?.toIntOrNull() ?: 8
        return BigDecimal(quantity)
            .setScale(precision, RoundingMode.DOWN)
            .stripTrailingZeros()
            .toPlainString()
    }

    /**
     * USDT miktarını uygun formata çevir (BUY için quoteCoin)
     */
    private fun formatUsdtAmount(amount: Double): String {
        return BigDecimal(amount)
            .setScale(2, RoundingMode.DOWN)
            .stripTrailingZeros()
            .toPlainString()
    }

    /**
     * Sayıyı belirli decimal place'e yuvarla
     */
    private fun roundToDecimalPlaces(value: Double, decimalPlaces: Int): Double {
        return BigDecimal(value)
            .setScale(decimalPlaces, RoundingMode.DOWN)
            .toDouble()
    }

    /**
     * Rebalance gerekliliğini kontrol et
     */
    suspend fun needsRebalancing(threshold: Double): Result<Boolean> {
        return try {
            val snapshotResult = portfolioRepository.calculatePortfolioSnapshot()
            if (snapshotResult.isFailure) {
                return Result.failure(snapshotResult.exceptionOrNull()!!)
            }

            val snapshot = snapshotResult.getOrNull()!!
            val needsRebalance = snapshot.coins.any { coin ->
                coin.coin != "USDT" && Math.abs(coin.deviation) >= threshold
            }

            Result.success(needsRebalance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
