package com.bybit.rebalancer.data.database

import androidx.room.*
import com.bybit.rebalancer.data.model.TradeLog
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeLogDao {

    @Query("SELECT * FROM trade_logs ORDER BY timestamp DESC")
    fun getAllTrades(): Flow<List<TradeLog>>

    @Query("SELECT * FROM trade_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTrades(limit: Int = 100): Flow<List<TradeLog>>

    @Query("SELECT * FROM trade_logs WHERE coin = :coin ORDER BY timestamp DESC")
    fun getTradesByCoin(coin: String): Flow<List<TradeLog>>

    @Query("SELECT * FROM trade_logs WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTradesByDateRange(startTime: Long, endTime: Long): Flow<List<TradeLog>>

    @Query("SELECT * FROM trade_logs WHERE status = :status ORDER BY timestamp DESC")
    fun getTradesByStatus(status: String): Flow<List<TradeLog>>

    @Query("SELECT * FROM trade_logs WHERE id = :id")
    suspend fun getTradeById(id: Long): TradeLog?

    @Insert
    suspend fun insertTrade(trade: TradeLog): Long

    @Insert
    suspend fun insertTrades(trades: List<TradeLog>)

    @Update
    suspend fun updateTrade(trade: TradeLog)

    @Delete
    suspend fun deleteTrade(trade: TradeLog)

    @Query("DELETE FROM trade_logs")
    suspend fun deleteAllTrades()

    @Query("DELETE FROM trade_logs WHERE timestamp < :beforeTime")
    suspend fun deleteOldTrades(beforeTime: Long)

    @Query("SELECT COUNT(*) FROM trade_logs")
    suspend fun getTradeCount(): Int

    @Query("SELECT COUNT(*) FROM trade_logs WHERE status = 'SUCCESS'")
    suspend fun getSuccessfulTradeCount(): Int

    @Query("SELECT COUNT(*) FROM trade_logs WHERE status = 'FAILED'")
    suspend fun getFailedTradeCount(): Int
}
