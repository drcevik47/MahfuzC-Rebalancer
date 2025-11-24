package com.bybit.rebalancer.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bybit.rebalancer.data.model.AppLog
import com.bybit.rebalancer.data.model.TradeLog
import com.bybit.rebalancer.ui.components.*
import com.bybit.rebalancer.ui.theme.*
import com.bybit.rebalancer.ui.viewmodel.LogUiState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogsScreen(
    uiState: LogUiState,
    onFilterChange: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onClearLogs: () -> Unit,
    onClearTrades: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kayıtlar",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(onClick = onExport) {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = "Dışa Aktar",
                        tint = BybitYellow
                    )
                }
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = "Temizle",
                        tint = LossRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Stats
        uiState.stats?.let { stats ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Toplam Log",
                    value = stats.totalLogs.toString(),
                    icon = Icons.Default.Description,
                    modifier = Modifier.width(120.dp)
                )
                StatCard(
                    title = "Hatalar",
                    value = stats.errorCount.toString(),
                    icon = Icons.Default.Error,
                    valueColor = if (stats.errorCount > 0) LossRed else TextPrimary,
                    modifier = Modifier.width(120.dp)
                )
                StatCard(
                    title = "İşlemler",
                    value = stats.totalTrades.toString(),
                    icon = Icons.Default.SwapHoriz,
                    modifier = Modifier.width(120.dp)
                )
                StatCard(
                    title = "Başarılı",
                    value = stats.successfulTrades.toString(),
                    icon = Icons.Default.CheckCircle,
                    valueColor = ProfitGreen,
                    modifier = Modifier.width(120.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = BybitDarkSurface,
            contentColor = BybitYellow
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Uygulama Logları") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("İşlem Kayıtları") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter chips (only for app logs)
        if (selectedTab == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filterLevel == null,
                    onClick = { onFilterChange(null) },
                    label = { Text("Tümü") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BybitYellow.copy(alpha = 0.3f),
                        selectedLabelColor = BybitYellow
                    )
                )
                listOf("ERROR", "WARNING", "INFO", "DEBUG").forEach { level ->
                    LogLevelChip(
                        level = level,
                        isSelected = uiState.filterLevel == level,
                        onClick = { onFilterChange(level) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search
            BybitTextField(
                value = uiState.searchQuery,
                onValueChange = onSearch,
                label = "Log Ara...",
                leadingIcon = Icons.Default.Search
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Content
        when (selectedTab) {
            0 -> AppLogsList(logs = uiState.appLogs)
            1 -> TradeLogsList(trades = uiState.tradeLogs)
        }
    }

    // Clear Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Kayıtları Temizle") },
            text = {
                Text(
                    if (selectedTab == 0)
                        "Tüm uygulama loglarını silmek istediğinizden emin misiniz?"
                    else
                        "Tüm işlem kayıtlarını silmek istediğinizden emin misiniz?"
                )
            },
            confirmButton = {
                BybitButton(
                    onClick = {
                        if (selectedTab == 0) onClearLogs() else onClearTrades()
                        showClearDialog = false
                    },
                    text = "Temizle"
                )
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("İptal", color = TextSecondary)
                }
            },
            containerColor = BybitDarkSurface
        )
    }
}

@Composable
fun AppLogsList(logs: List<AppLog>) {
    if (logs.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Description,
            title = "Henüz log yok",
            subtitle = "Uygulama etkinlikleri burada görünecek"
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs) { log ->
                LogItem(log = log)
            }
        }
    }
}

@Composable
fun LogItem(log: AppLog) {
    val levelColor = when (log.level) {
        "ERROR" -> LossRed
        "WARNING" -> WarningOrange
        "INFO" -> ProfitGreen
        "DEBUG" -> TextSecondary
        else -> TextPrimary
    }

    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    BybitCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = levelColor.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = log.level,
                            style = MaterialTheme.typography.labelSmall,
                            color = levelColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = log.tag,
                        style = MaterialTheme.typography.labelMedium,
                        color = BybitYellow
                    )
                }
                Text(
                    text = dateFormat.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = log.message,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )

            log.details?.let { details ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = details.take(200),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun TradeLogsList(trades: List<TradeLog>) {
    if (trades.isEmpty()) {
        EmptyState(
            icon = Icons.Default.SwapHoriz,
            title = "Henüz işlem yok",
            subtitle = "Yapılan işlemler burada görünecek"
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(trades) { trade ->
                TradeItem(trade = trade)
            }
        }
    }
}

@Composable
fun TradeItem(trade: TradeLog) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()) }
    val isBuy = trade.action == "BUY"
    val statusColor = when (trade.status) {
        "SUCCESS" -> ProfitGreen
        "FAILED" -> LossRed
        else -> WarningOrange
    }

    BybitCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (isBuy) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isBuy) ProfitGreen else LossRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = trade.action,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isBuy) ProfitGreen else LossRed,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = trade.coin,
                        style = MaterialTheme.typography.titleSmall,
                        color = BybitYellow,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = trade.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Miktar",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "${trade.quantity.take(12)} ${trade.coin}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Fiyat",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "$${trade.price.take(10)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "USDT",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "$${trade.usdtAmount.take(10)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BybitYellow
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = dateFormat.format(Date(trade.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}
