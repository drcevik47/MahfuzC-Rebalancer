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
import java.text.SimpleDateFormat
import java.util.*
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

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

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

    // Generate shareable text for logs
    fun generateLogsText(): String {
        val logs = _uiState.value.logs
        if (logs.isEmpty()) return "Log kaydı bulunamadı."

        val sb = StringBuilder()
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine("    BYBIT REBALANCER - LOG KAYITLARI")
        sb.appendLine("    Oluşturulma: ${dateFormat.format(Date())}")
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine()

        logs.forEach { log ->
            val time = dateFormat.format(Date(log.timestamp))
            val levelIcon = when (log.level) {
                "ERROR" -> "❌"
                "WARNING" -> "⚠️"
                "INFO" -> "ℹ️"
                "DEBUG" -> "🔧"
                else -> "📝"
            }
            sb.appendLine("[$time] $levelIcon ${log.level}")
            sb.appendLine("Tag: ${log.tag}")
            sb.appendLine("Mesaj: ${log.message}")
            if (!log.details.isNullOrBlank()) {
                sb.appendLine("Detay: ${log.details}")
            }
            sb.appendLine("───────────────────────────────────────")
        }

        sb.appendLine()
        sb.appendLine("Toplam: ${logs.size} log kaydı")
        return sb.toString()
    }

    // Generate shareable text for trades
    fun generateTradesText(): String {
        val trades = _uiState.value.trades
        if (trades.isEmpty()) return "İşlem kaydı bulunamadı."

        val sb = StringBuilder()
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine("   BYBIT REBALANCER - İŞLEM GEÇMİŞİ")
        sb.appendLine("    Oluşturulma: ${dateFormat.format(Date())}")
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine()

        var totalBuyValue = 0.0
        var totalSellValue = 0.0

        trades.forEach { trade ->
            val time = dateFormat.format(Date(trade.timestamp))
            val sideIcon = if (trade.side == "BUY") "🟢 ALIŞ" else "🔴 SATIŞ"

            if (trade.side == "BUY") totalBuyValue += trade.usdtValue
            else totalSellValue += trade.usdtValue

            sb.appendLine("📊 ${trade.symbol}")
            sb.appendLine("   Tarih: $time")
            sb.appendLine("   İşlem: $sideIcon")
            sb.appendLine("   Miktar: ${trade.quantity}")
            sb.appendLine("   Fiyat: $${String.format("%.4f", trade.price)}")
            sb.appendLine("   Değer: $${String.format("%.2f", trade.usdtValue)} USDT")
            if (trade.fee != null) {
                sb.appendLine("   Komisyon: $${String.format("%.4f", trade.fee)}")
            }
            sb.appendLine("   Durum: ${trade.status}")
            sb.appendLine("   Order ID: ${trade.orderId}")
            sb.appendLine("───────────────────────────────────────")
        }

        sb.appendLine()
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine("                  ÖZET")
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine("Toplam İşlem: ${trades.size}")
        sb.appendLine("Toplam Alış: $${String.format("%.2f", totalBuyValue)} USDT")
        sb.appendLine("Toplam Satış: $${String.format("%.2f", totalSellValue)} USDT")
        sb.appendLine("Net: $${String.format("%.2f", totalSellValue - totalBuyValue)} USDT")

        return sb.toString()
    }

    // Generate combined report
    fun generateFullReport(): String {
        val sb = StringBuilder()
        sb.appendLine("╔═══════════════════════════════════════╗")
        sb.appendLine("║   BYBIT PORTFOLIO REBALANCER RAPORU   ║")
        sb.appendLine("║     ${dateFormat.format(Date())}      ║")
        sb.appendLine("╚═══════════════════════════════════════╝")
        sb.appendLine()
        sb.appendLine()
        sb.appendLine(generateTradesText())
        sb.appendLine()
        sb.appendLine()
        sb.appendLine(generateLogsText())
        return sb.toString()
    }
}
