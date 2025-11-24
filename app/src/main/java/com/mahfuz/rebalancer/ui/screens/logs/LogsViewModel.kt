package com.mahfuz.rebalancer.ui.screens.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahfuz.rebalancer.data.db.LogEntry
import com.mahfuz.rebalancer.data.db.TradeHistoryEntity
import com.mahfuz.rebalancer.data.repository.PortfolioRepository
import com.mahfuz.rebalancer.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogsUiState(
    val logs: List<LogEntry> = emptyList(),
    val trades: List<TradeHistoryEntity> = emptyList(),
    val selectedTab: Int = 0,
    val filterLevel: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val logger: AppLogger,
    private val portfolioRepository: PortfolioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Observe logs
            launch {
                logger.getRecentLogs(500).collect { logs ->
                    _uiState.update { state ->
                        val filtered = if (state.filterLevel != null) {
                            logs.filter { it.level == state.filterLevel }
                        } else {
                            logs
                        }
                        state.copy(logs = filtered, isLoading = false)
                    }
                }
            }

            // Observe trades
            launch {
                portfolioRepository.getRecentTrades(100).collect { trades ->
                    _uiState.update { it.copy(trades = trades) }
                }
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun setFilter(level: String?) {
        _uiState.update { it.copy(filterLevel = level) }
        loadData()
    }

    fun clearLogs() {
        viewModelScope.launch {
            logger.clearAllLogs()
        }
    }
}
