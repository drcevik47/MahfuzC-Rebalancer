package com.mahfuz.rebalancer.data.model

import java.math.BigDecimal

// Portfolio Configuration
data class PortfolioConfig(
    val coins: Map<String, BigDecimal>, // coin -> target percentage (0-100)
    val threshold: BigDecimal = BigDecimal("1.0"), // rebalance threshold percentage
    val isActive: Boolean = false
) {
    companion object {
        val EMPTY = PortfolioConfig(emptyMap())
    }
}

// Current Portfolio State
data class PortfolioState(
    val holdings: List<CoinHolding>,
    val totalValueUsdt: BigDecimal,
    val timestamp: Long = System.currentTimeMillis()
)

data class CoinHolding(
    val coin: String,
    val balance: BigDecimal,
    val usdtValue: BigDecimal,
    val currentPercentage: BigDecimal,
    val targetPercentage: BigDecimal,
    val deviation: BigDecimal, // currentPercentage - targetPercentage
    val priceUsdt: BigDecimal
)

// Rebalance Action
data class RebalanceAction(
    val coin: String,
    val action: TradeAction,
    val amount: BigDecimal, // in coin units
    val usdtValue: BigDecimal,
    val symbol: String // trading pair e.g., "BTCUSDT"
)

enum class TradeAction {
    BUY, SELL
}

// Price Cache
data class PriceInfo(
    val symbol: String,
    val price: BigDecimal,
    val bidPrice: BigDecimal?,
    val askPrice: BigDecimal?,
    val timestamp: Long = System.currentTimeMillis()
)

// App Settings
data class AppSettings(
    val apiKey: String = "",
    val apiSecret: String = "",
    val isTestnet: Boolean = false,
    val autoStartOnBoot: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val logRetentionDays: Int = 30
)

// Service State
enum class ServiceState {
    STOPPED,
    STARTING,
    RUNNING,
    PAUSED,
    ERROR
}

data class RebalancerState(
    val serviceState: ServiceState = ServiceState.STOPPED,
    val isConnected: Boolean = false,
    val lastCheck: Long = 0,
    val lastRebalance: Long = 0,
    val errorMessage: String? = null,
    val portfolioState: PortfolioState? = null
)
