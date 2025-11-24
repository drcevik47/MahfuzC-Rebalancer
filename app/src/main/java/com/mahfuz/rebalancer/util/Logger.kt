package com.mahfuz.rebalancer.util

import android.util.Log
import com.mahfuz.rebalancer.data.db.LogDao
import com.mahfuz.rebalancer.data.db.LogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLogger @Inject constructor(
    private val logDao: LogDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val LEVEL_DEBUG = "DEBUG"
        const val LEVEL_INFO = "INFO"
        const val LEVEL_WARNING = "WARNING"
        const val LEVEL_ERROR = "ERROR"

        // Common tags
        const val TAG_API = "API"
        const val TAG_WEBSOCKET = "WEBSOCKET"
        const val TAG_REBALANCER = "REBALANCER"
        const val TAG_SERVICE = "SERVICE"
        const val TAG_TRADE = "TRADE"
        const val TAG_PORTFOLIO = "PORTFOLIO"
        const val TAG_SYSTEM = "SYSTEM"
    }

    fun debug(tag: String, message: String, details: String? = null) {
        log(LEVEL_DEBUG, tag, message, details)
    }

    fun info(tag: String, message: String, details: String? = null) {
        log(LEVEL_INFO, tag, message, details)
    }

    fun warning(tag: String, message: String, details: String? = null) {
        log(LEVEL_WARNING, tag, message, details)
    }

    fun error(tag: String, message: String, details: String? = null) {
        log(LEVEL_ERROR, tag, message, details)
    }

    fun error(tag: String, message: String, throwable: Throwable) {
        log(LEVEL_ERROR, tag, message, throwable.stackTraceToString())
    }

    private fun log(level: String, tag: String, message: String, details: String?) {
        // Log to Android logcat
        when (level) {
            LEVEL_DEBUG -> Log.d(tag, message)
            LEVEL_INFO -> Log.i(tag, message)
            LEVEL_WARNING -> Log.w(tag, message)
            LEVEL_ERROR -> Log.e(tag, message)
        }

        // Save to database
        scope.launch {
            try {
                logDao.insert(
                    LogEntry(
                        level = level,
                        tag = tag,
                        message = message,
                        details = details
                    )
                )
            } catch (e: Exception) {
                Log.e("AppLogger", "Failed to save log to database", e)
            }
        }
    }

    fun getRecentLogs(limit: Int = 500): Flow<List<LogEntry>> = logDao.getRecentLogs(limit)

    fun getLogsByLevel(level: String, limit: Int = 100): Flow<List<LogEntry>> =
        logDao.getLogsByLevel(level, limit)

    fun getLogsByTag(tag: String, limit: Int = 100): Flow<List<LogEntry>> =
        logDao.getLogsByTag(tag, limit)

    fun getLogsSince(since: Long): Flow<List<LogEntry>> = logDao.getLogsSince(since)

    suspend fun cleanupOldLogs(retentionDays: Int) {
        val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        val deleted = logDao.deleteOldLogs(cutoffTime)
        info(TAG_SYSTEM, "Cleaned up $deleted old log entries")
    }

    suspend fun clearAllLogs() {
        logDao.deleteAllLogs()
    }

    suspend fun getLogCount(): Int = logDao.getLogCount()
}
