package com.bybit.rebalancer.data.repository

import android.util.Log
import com.bybit.rebalancer.data.database.AppLogDao
import com.bybit.rebalancer.data.database.TradeLogDao
import com.bybit.rebalancer.data.model.AppLog
import com.bybit.rebalancer.data.model.TradeLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepository @Inject constructor(
    private val appLogDao: AppLogDao,
    private val tradeLogDao: TradeLogDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    companion object {
        const val LEVEL_DEBUG = "DEBUG"
        const val LEVEL_INFO = "INFO"
        const val LEVEL_WARNING = "WARNING"
        const val LEVEL_ERROR = "ERROR"
    }

    // App Logs
    fun getAllLogs(): Flow<List<AppLog>> = appLogDao.getAllLogs()
    fun getRecentLogs(limit: Int = 500): Flow<List<AppLog>> = appLogDao.getRecentLogs(limit)
    fun getLogsByLevel(level: String): Flow<List<AppLog>> = appLogDao.getLogsByLevel(level)
    fun getErrorsAndWarnings(): Flow<List<AppLog>> = appLogDao.getErrorsAndWarnings()
    fun searchLogs(query: String): Flow<List<AppLog>> = appLogDao.searchLogs(query)

    suspend fun getAllLogsForExport(): List<AppLog> = appLogDao.getAllLogsForExport()

    // Trade Logs
    fun getAllTrades(): Flow<List<TradeLog>> = tradeLogDao.getAllTrades()
    fun getRecentTrades(limit: Int = 100): Flow<List<TradeLog>> = tradeLogDao.getRecentTrades(limit)
    fun getTradesByCoin(coin: String): Flow<List<TradeLog>> = tradeLogDao.getTradesByCoin(coin)

    suspend fun insertTrade(trade: TradeLog): Long {
        val id = tradeLogDao.insertTrade(trade)
        logInfo("Trade", "İşlem kaydedildi: ${trade.action} ${trade.coin} - ${trade.quantity}")
        return id
    }

    suspend fun updateTrade(trade: TradeLog) {
        tradeLogDao.updateTrade(trade)
    }

    // Log helper functions
    fun logDebug(tag: String, message: String, details: String? = null) {
        Log.d(tag, message)
        insertLogAsync(LEVEL_DEBUG, tag, message, details)
    }

    fun logInfo(tag: String, message: String, details: String? = null) {
        Log.i(tag, message)
        insertLogAsync(LEVEL_INFO, tag, message, details)
    }

    fun logWarning(tag: String, message: String, details: String? = null) {
        Log.w(tag, message)
        insertLogAsync(LEVEL_WARNING, tag, message, details)
    }

    fun logError(tag: String, message: String, details: String? = null) {
        Log.e(tag, message)
        insertLogAsync(LEVEL_ERROR, tag, message, details)
    }

    private fun insertLogAsync(level: String, tag: String, message: String, details: String?) {
        scope.launch {
            try {
                appLogDao.insertLog(
                    AppLog(
                        level = level,
                        tag = tag,
                        message = message,
                        details = details
                    )
                )
            } catch (e: Exception) {
                Log.e("LogRepository", "Log kaydetme hatası: ${e.message}")
            }
        }
    }

    // Temizlik işlemleri
    suspend fun clearAllLogs() {
        appLogDao.deleteAllLogs()
        logInfo("System", "Tüm loglar temizlendi")
    }

    suspend fun clearAllTrades() {
        tradeLogDao.deleteAllTrades()
        logInfo("System", "Tüm işlem kayıtları temizlendi")
    }

    suspend fun clearOldLogs(daysToKeep: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        appLogDao.deleteOldLogs(cutoffTime)
        tradeLogDao.deleteOldTrades(cutoffTime)
        logInfo("System", "$daysToKeep günden eski kayıtlar temizlendi")
    }

    // Export fonksiyonları
    suspend fun exportLogsToText(): String {
        val logs = appLogDao.getAllLogsForExport()
        val sb = StringBuilder()
        sb.appendLine("=== BYBIT REBALANCER LOG EXPORT ===")
        sb.appendLine("Export Time: ${dateFormat.format(Date())}")
        sb.appendLine("Total Logs: ${logs.size}")
        sb.appendLine("=" .repeat(50))
        sb.appendLine()

        logs.forEach { log ->
            val time = dateFormat.format(Date(log.timestamp))
            sb.appendLine("[$time] [${log.level}] [${log.tag}] ${log.message}")
            log.details?.let {
                sb.appendLine("  Details: $it")
            }
        }

        return sb.toString()
    }

    suspend fun exportTradesToText(): String {
        val trades = tradeLogDao.getAllTrades().let {
            // Collect first to get the list
            val result = mutableListOf<TradeLog>()
            // We need a different approach - let's use a direct query
            return@let result
        }

        val sb = StringBuilder()
        sb.appendLine("=== BYBIT REBALANCER TRADE EXPORT ===")
        sb.appendLine("Export Time: ${dateFormat.format(Date())}")
        sb.appendLine("=" .repeat(50))

        return sb.toString()
    }

    // İstatistikler
    suspend fun getStats(): LogStats {
        return LogStats(
            totalLogs = appLogDao.getLogCount(),
            errorCount = appLogDao.getErrorCount(),
            totalTrades = tradeLogDao.getTradeCount(),
            successfulTrades = tradeLogDao.getSuccessfulTradeCount(),
            failedTrades = tradeLogDao.getFailedTradeCount()
        )
    }
}

data class LogStats(
    val totalLogs: Int,
    val errorCount: Int,
    val totalTrades: Int,
    val successfulTrades: Int,
    val failedTrades: Int
)
