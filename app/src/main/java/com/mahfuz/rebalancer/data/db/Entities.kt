package com.mahfuz.rebalancer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Log Entry Entity
@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String, // INFO, WARNING, ERROR, DEBUG
    val tag: String,
    val message: String,
    val details: String? = null
)

// Portfolio Coin Entity
@Entity(tableName = "portfolio_coins")
data class PortfolioCoinEntity(
    @PrimaryKey val coin: String,
    val targetPercentage: Double,
    val isActive: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)

// Portfolio Settings Entity
@Entity(tableName = "portfolio_settings")
data class PortfolioSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val threshold: Double = 1.0,
    val isRebalancingActive: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

// Trade History Entity
@Entity(tableName = "trade_history")
@TypeConverters(Converters::class)
data class TradeHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val orderId: String,
    val symbol: String,
    val side: String, // BUY or SELL
    val quantity: Double,
    val price: Double,
    val usdtValue: Double,
    val fee: Double?,
    val status: String,

    // Portfolio state before trade
    val portfolioBefore: String, // JSON

    // Portfolio state after trade
    val portfolioAfter: String?, // JSON (null until trade completes)

    // Reason for trade
    val reason: String
)

// Price History Entity (for analytics)
@Entity(tableName = "price_history")
data class PriceHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val price: Double,
    val timestamp: Long = System.currentTimeMillis()
)

// Rebalance Event Entity
@Entity(tableName = "rebalance_events")
@TypeConverters(Converters::class)
data class RebalanceEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val totalValueBefore: Double,
    val totalValueAfter: Double?,
    val trades: String, // JSON array of trade IDs
    val status: String, // PENDING, COMPLETED, FAILED
    val errorMessage: String? = null
)

// Type Converters for Room
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        if (value == null) return null
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromDoubleMap(value: String?): Map<String, Double>? {
        if (value == null) return null
        val mapType = object : TypeToken<Map<String, Double>>() {}.type
        return gson.fromJson(value, mapType)
    }

    @TypeConverter
    fun toDoubleMap(map: Map<String, Double>?): String? {
        return gson.toJson(map)
    }
}
