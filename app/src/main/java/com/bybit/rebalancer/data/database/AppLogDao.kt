package com.bybit.rebalancer.data.database

import androidx.room.*
import com.bybit.rebalancer.data.model.AppLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLogDao {

    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AppLog>>

    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 500): Flow<List<AppLog>>

    @Query("SELECT * FROM app_logs WHERE level = :level ORDER BY timestamp DESC")
    fun getLogsByLevel(level: String): Flow<List<AppLog>>

    @Query("SELECT * FROM app_logs WHERE tag = :tag ORDER BY timestamp DESC")
    fun getLogsByTag(tag: String): Flow<List<AppLog>>

    @Query("SELECT * FROM app_logs WHERE level IN ('ERROR', 'WARNING') ORDER BY timestamp DESC LIMIT :limit")
    fun getErrorsAndWarnings(limit: Int = 100): Flow<List<AppLog>>

    @Query("SELECT * FROM app_logs WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getLogsByDateRange(startTime: Long, endTime: Long): Flow<List<AppLog>>

    @Query("SELECT * FROM app_logs WHERE message LIKE '%' || :searchQuery || '%' ORDER BY timestamp DESC")
    fun searchLogs(searchQuery: String): Flow<List<AppLog>>

    @Insert
    suspend fun insertLog(log: AppLog): Long

    @Insert
    suspend fun insertLogs(logs: List<AppLog>)

    @Delete
    suspend fun deleteLog(log: AppLog)

    @Query("DELETE FROM app_logs")
    suspend fun deleteAllLogs()

    @Query("DELETE FROM app_logs WHERE timestamp < :beforeTime")
    suspend fun deleteOldLogs(beforeTime: Long)

    @Query("SELECT COUNT(*) FROM app_logs")
    suspend fun getLogCount(): Int

    @Query("SELECT COUNT(*) FROM app_logs WHERE level = 'ERROR'")
    suspend fun getErrorCount(): Int

    // Export için tüm logları al (suspend)
    @Query("SELECT * FROM app_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsForExport(): List<AppLog>
}
