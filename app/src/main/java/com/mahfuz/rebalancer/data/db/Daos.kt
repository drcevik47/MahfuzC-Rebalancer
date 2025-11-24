package com.mahfuz.rebalancer.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert
    suspend fun insert(log: LogEntry): Long

    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 500): Flow<List<LogEntry>>

    @Query("SELECT * FROM logs WHERE level = :level ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsByLevel(level: String, limit: Int = 100): Flow<List<LogEntry>>

    @Query("SELECT * FROM logs WHERE tag = :tag ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsByTag(tag: String, limit: Int = 100): Flow<List<LogEntry>>

    @Query("SELECT * FROM logs WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getLogsSince(since: Long): Flow<List<LogEntry>>

    @Query("DELETE FROM logs WHERE timestamp < :before")
    suspend fun deleteOldLogs(before: Long): Int

    @Query("DELETE FROM logs")
    suspend fun deleteAllLogs()

    @Query("SELECT COUNT(*) FROM logs")
    suspend fun getLogCount(): Int
}

@Dao
interface PortfolioCoinDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(coin: PortfolioCoinEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(coins: List<PortfolioCoinEntity>)

    @Update
    suspend fun update(coin: PortfolioCoinEntity)

    @Delete
    suspend fun delete(coin: PortfolioCoinEntity)

    @Query("DELETE FROM portfolio_coins WHERE coin = :coinName")
    suspend fun deleteByCoin(coinName: String)

    @Query("SELECT * FROM portfolio_coins WHERE isActive = 1")
    fun getActiveCoins(): Flow<List<PortfolioCoinEntity>>

    @Query("SELECT * FROM portfolio_coins")
    fun getAllCoins(): Flow<List<PortfolioCoinEntity>>

    @Query("SELECT * FROM portfolio_coins WHERE coin = :coinName")
    suspend fun getCoin(coinName: String): PortfolioCoinEntity?

    @Query("DELETE FROM portfolio_coins")
    suspend fun deleteAll()

    @Query("SELECT SUM(targetPercentage) FROM portfolio_coins WHERE isActive = 1")
    suspend fun getTotalPercentage(): Double?
}

@Dao
interface PortfolioSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: PortfolioSettingsEntity)

    @Update
    suspend fun update(settings: PortfolioSettingsEntity)

    @Query("SELECT * FROM portfolio_settings WHERE id = 1")
    fun getSettings(): Flow<PortfolioSettingsEntity?>

    @Query("SELECT * FROM portfolio_settings WHERE id = 1")
    suspend fun getSettingsOnce(): PortfolioSettingsEntity?

    @Query("UPDATE portfolio_settings SET isRebalancingActive = :active, updatedAt = :timestamp WHERE id = 1")
    suspend fun setRebalancingActive(active: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE portfolio_settings SET threshold = :threshold, updatedAt = :timestamp WHERE id = 1")
    suspend fun setThreshold(threshold: Double, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface TradeHistoryDao {
    @Insert
    suspend fun insert(trade: TradeHistoryEntity): Long

    @Update
    suspend fun update(trade: TradeHistoryEntity)

    @Query("SELECT * FROM trade_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTrades(limit: Int = 100): Flow<List<TradeHistoryEntity>>

    @Query("SELECT * FROM trade_history WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getTradesSince(since: Long): Flow<List<TradeHistoryEntity>>

    @Query("SELECT * FROM trade_history WHERE symbol = :symbol ORDER BY timestamp DESC LIMIT :limit")
    fun getTradesBySymbol(symbol: String, limit: Int = 50): Flow<List<TradeHistoryEntity>>

    @Query("SELECT * FROM trade_history WHERE id = :id")
    suspend fun getTradeById(id: Long): TradeHistoryEntity?

    @Query("SELECT * FROM trade_history WHERE orderId = :orderId")
    suspend fun getTradeByOrderId(orderId: String): TradeHistoryEntity?

    @Query("DELETE FROM trade_history WHERE timestamp < :before")
    suspend fun deleteOldTrades(before: Long): Int
}

@Dao
interface PriceHistoryDao {
    @Insert
    suspend fun insert(price: PriceHistoryEntity)

    @Insert
    suspend fun insertAll(prices: List<PriceHistoryEntity>)

    @Query("SELECT * FROM price_history WHERE symbol = :symbol ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPrice(symbol: String): PriceHistoryEntity?

    @Query("SELECT * FROM price_history WHERE symbol = :symbol AND timestamp >= :since ORDER BY timestamp ASC")
    fun getPriceHistory(symbol: String, since: Long): Flow<List<PriceHistoryEntity>>

    @Query("DELETE FROM price_history WHERE timestamp < :before")
    suspend fun deleteOldPrices(before: Long): Int
}

@Dao
interface RebalanceEventDao {
    @Insert
    suspend fun insert(event: RebalanceEventEntity): Long

    @Update
    suspend fun update(event: RebalanceEventEntity)

    @Query("SELECT * FROM rebalance_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 50): Flow<List<RebalanceEventEntity>>

    @Query("SELECT * FROM rebalance_events WHERE id = :id")
    suspend fun getEventById(id: Long): RebalanceEventEntity?

    @Query("SELECT * FROM rebalance_events WHERE status = :status ORDER BY timestamp DESC")
    fun getEventsByStatus(status: String): Flow<List<RebalanceEventEntity>>

    @Query("DELETE FROM rebalance_events WHERE timestamp < :before")
    suspend fun deleteOldEvents(before: Long): Int
}
