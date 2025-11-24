package com.bybit.rebalancer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bybit.rebalancer.data.model.AppLog
import com.bybit.rebalancer.data.model.TradeLog
import com.bybit.rebalancer.data.repository.LogRepository
import com.bybit.rebalancer.data.repository.LogStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogUiState(
    val appLogs: List<AppLog> = emptyList(),
    val tradeLogs: List<TradeLog> = emptyList(),
    val stats: LogStats? = null,
    val filterLevel: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val exportedText: String? = null
)

@HiltViewModel
class LogViewModel @Inject constructor(
    private val logRepository: LogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    init {
        observeLogs()
        observeTrades()
        loadStats()
    }

    private fun observeLogs() {
        viewModelScope.launch {
            logRepository.getRecentLogs(500).collect { logs ->
                _uiState.update { it.copy(appLogs = logs) }
            }
        }
    }

    private fun observeTrades() {
        viewModelScope.launch {
            logRepository.getRecentTrades(100).collect { trades ->
                _uiState.update { it.copy(tradeLogs = trades) }
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            try {
                val stats = logRepository.getStats()
                _uiState.update { it.copy(stats = stats) }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun setFilterLevel(level: String?) {
        _uiState.update { it.copy(filterLevel = level) }
        viewModelScope.launch {
            if (level != null) {
                logRepository.getLogsByLevel(level).collect { logs ->
                    _uiState.update { it.copy(appLogs = logs) }
                }
            } else {
                logRepository.getRecentLogs(500).collect { logs ->
                    _uiState.update { it.copy(appLogs = logs) }
                }
            }
        }
    }

    fun searchLogs(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            observeLogs()
        } else {
            viewModelScope.launch {
                logRepository.searchLogs(query).collect { logs ->
                    _uiState.update { it.copy(appLogs = logs) }
                }
            }
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            logRepository.clearAllLogs()
            loadStats()
        }
    }

    fun clearAllTrades() {
        viewModelScope.launch {
            logRepository.clearAllTrades()
            loadStats()
        }
    }

    fun clearOldLogs(daysToKeep: Int = 30) {
        viewModelScope.launch {
            logRepository.clearOldLogs(daysToKeep)
            loadStats()
        }
    }

    fun exportLogs() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val text = logRepository.exportLogsToText()
                _uiState.update { it.copy(exportedText = text) }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearExportedText() {
        _uiState.update { it.copy(exportedText = null) }
    }
}
