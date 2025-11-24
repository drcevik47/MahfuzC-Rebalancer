package com.bybit.rebalancer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * Portföydeki bir coin'i temsil eder
 */
@Entity(tableName = "portfolio_coins")
data class PortfolioCoin(
    @PrimaryKey
    val coin: String,                    // Coin sembolü (BTC, ETH, USDT vb.)
    val targetPercentage: Double,        // Hedef yüzde (0-100)
    val isEnabled: Boolean = true,       // Coin aktif mi
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Cüzdandaki coin bakiyesi (API'den gelen)
 */
data class CoinBalance(
    val coin: String,
    val walletBalance: String,           // Toplam bakiye
    val availableToWithdraw: String,     // Çekilebilir bakiye
    val usdValue: String                 // USD değeri
)

/**
 * İşlem kaydı
 */
@Entity(tableName = "trade_logs")
data class TradeLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,                  // BUY, SELL
    val symbol: String,                  // İşlem çifti (BTCUSDT)
    val coin: String,                    // Coin (BTC)
    val quantity: String,                // Miktar
    val price: String,                   // Fiyat
    val usdtAmount: String,              // USDT tutarı
    val portfolioBefore: String,         // JSON: İşlem öncesi portföy durumu
    val portfolioAfter: String,          // JSON: İşlem sonrası portföy durumu
    val orderId: String?,                // Bybit order ID
    val status: String                   // SUCCESS, FAILED, PENDING
)

/**
 * Genel uygulama logları
 */
@Entity(tableName = "app_logs")
data class AppLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String,                   // INFO, WARNING, ERROR, DEBUG
    val tag: String,                     // Log kaynağı
    val message: String,
    val details: String? = null          // Ek detaylar (JSON veya stack trace)
)

/**
 * Portföy durumu anlık görüntüsü
 */
data class PortfolioSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val totalValueUsdt: Double,
    val coins: List<CoinSnapshot>
)

data class CoinSnapshot(
    val coin: String,
    val balance: Double,
    val usdtValue: Double,
    val currentPercentage: Double,
    val targetPercentage: Double,
    val deviation: Double                // currentPercentage - targetPercentage
)

/**
 * Rebalance işlemi için hesaplanan trade
 */
data class RebalanceTrade(
    val coin: String,
    val symbol: String,                  // Trading pair (BTCUSDT)
    val action: TradeAction,
    val quantity: Double,
    val estimatedUsdtAmount: Double,
    val currentPrice: Double
)

enum class TradeAction {
    BUY, SELL
}

/**
 * Uygulama ayarları
 */
data class AppSettings(
    val apiKey: String = "",
    val apiSecret: String = "",
    val isTestnet: Boolean = false,
    val rebalanceThreshold: Double = 1.0,  // Yüzde sapma eşiği
    val minTradeUsdt: Double = 10.0,        // Minimum işlem tutarı
    val isServiceEnabled: Boolean = false,
    val checkIntervalSeconds: Int = 30      // Fiyat kontrol aralığı
)

/**
 * WebSocket ticker verisi
 */
data class TickerData(
    val symbol: String,
    val lastPrice: String,
    val price24hPcnt: String,
    val highPrice24h: String,
    val lowPrice24h: String,
    val volume24h: String,
    val turnover24h: String
)

/**
 * Bağlantı durumu
 */
enum class ConnectionStatus {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
    ERROR
}

/**
 * Servis durumu
 */
data class ServiceState(
    val isRunning: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val lastCheckTime: Long? = null,
    val lastRebalanceTime: Long? = null,
    val errorMessage: String? = null
)

/**
 * API Response wrapper
 */
data class BybitResponse<T>(
    val retCode: Int,
    val retMsg: String,
    val result: T?,
    val time: Long
)

/**
 * Wallet balance response
 */
data class WalletBalanceResult(
    val list: List<AccountInfo>
)

data class AccountInfo(
    val accountType: String,
    val coin: List<CoinInfo>
)

data class CoinInfo(
    val coin: String,
    val walletBalance: String,
    val availableToWithdraw: String,
    val usdValue: String
)

/**
 * Instruments info response
 */
data class InstrumentsResult(
    val category: String,
    val list: List<InstrumentInfo>
)

data class InstrumentInfo(
    val symbol: String,
    val baseCoin: String,
    val quoteCoin: String,
    val status: String,
    val lotSizeFilter: LotSizeFilter,
    val priceFilter: PriceFilter
)

data class LotSizeFilter(
    val basePrecision: String,
    val quotePrecision: String,
    val minOrderQty: String,
    val maxOrderQty: String,
    val minOrderAmt: String,
    val maxOrderAmt: String
)

data class PriceFilter(
    val tickSize: String
)

/**
 * Order response
 */
data class OrderResult(
    val orderId: String,
    val orderLinkId: String
)

/**
 * Tickers response
 */
data class TickersResult(
    val category: String,
    val list: List<TickerInfo>
)

data class TickerInfo(
    val symbol: String,
    val lastPrice: String,
    val indexPrice: String?,
    val markPrice: String?,
    val prevPrice24h: String,
    val price24hPcnt: String,
    val highPrice24h: String,
    val lowPrice24h: String,
    val volume24h: String,
    val turnover24h: String
)
