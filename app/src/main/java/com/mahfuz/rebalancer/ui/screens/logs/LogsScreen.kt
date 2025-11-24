package com.mahfuz.rebalancer.ui.screens.logs

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahfuz.rebalancer.data.db.LogEntry
import com.mahfuz.rebalancer.data.db.TradeHistoryEntity
import com.mahfuz.rebalancer.ui.theme.BybitGreen
import com.mahfuz.rebalancer.ui.theme.BybitRed
import com.mahfuz.rebalancer.ui.theme.BybitYellow
import com.mahfuz.rebalancer.util.AppLogger
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    onNavigateBack: () -> Unit,
    viewModel: LogsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loglar") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Temizle")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Loglar") },
                    icon = { Icon(Icons.Default.List, contentDescription = null) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("İşlemler") },
                    icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) }
                )
            }

            when (uiState.selectedTab) {
                0 -> LogsList(
                    logs = uiState.logs,
                    filterLevel = uiState.filterLevel,
                    onFilterChange = { viewModel.setFilter(it) }
                )
                1 -> TradesList(trades = uiState.trades)
            }
        }
    }

    // Clear confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Logları Temizle") },
            text = { Text("Tüm log kayıtları silinecek. Devam etmek istiyor musunuz?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearLogs()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BybitRed)
                ) {
                    Text("Temizle")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
fun LogsList(
    logs: List<LogEntry>,
    filterLevel: String?,
    onFilterChange: (String?) -> Unit
) {
    Column {
        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterLevel == null,
                onClick = { onFilterChange(null) },
                label = { Text("Tümü") }
            )
            FilterChip(
                selected = filterLevel == AppLogger.LEVEL_ERROR,
                onClick = { onFilterChange(AppLogger.LEVEL_ERROR) },
                label = { Text("Hatalar") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BybitRed.copy(alpha = 0.2f)
                )
            )
            FilterChip(
                selected = filterLevel == AppLogger.LEVEL_WARNING,
                onClick = { onFilterChange(AppLogger.LEVEL_WARNING) },
                label = { Text("Uyarılar") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BybitYellow.copy(alpha = 0.2f)
                )
            )
            FilterChip(
                selected = filterLevel == AppLogger.LEVEL_INFO,
                onClick = { onFilterChange(AppLogger.LEVEL_INFO) },
                label = { Text("Bilgi") }
            )
            FilterChip(
                selected = filterLevel == AppLogger.LEVEL_DEBUG,
                onClick = { onFilterChange(AppLogger.LEVEL_DEBUG) },
                label = { Text("Debug") }
            )
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Log kaydı bulunamadı",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    LogItem(log = log)
                }
            }
        }
    }
}

@Composable
fun LogItem(log: LogEntry) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val levelColor = when (log.level) {
        AppLogger.LEVEL_ERROR -> BybitRed
        AppLogger.LEVEL_WARNING -> BybitYellow
        AppLogger.LEVEL_INFO -> BybitGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.level,
                        color = levelColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log.tag,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = dateFormat.format(Date(log.timestamp)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = log.message,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            log.details?.let { details ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = details.take(200) + if (details.length > 200) "..." else "",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun TradesList(trades: List<TradeHistoryEntity>) {
    if (trades.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Henüz işlem yapılmadı",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(trades, key = { it.id }) { trade ->
                TradeItem(trade = trade)
            }
        }
    }
}

@Composable
fun TradeItem(trade: TradeHistoryEntity) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()) }
    val isBuy = trade.side == "BUY"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isBuy) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isBuy) BybitGreen else BybitRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = trade.symbol,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBuy) "Alım" else "Satım",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isBuy) BybitGreen else BybitRed
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "%.8f".format(trade.quantity),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$%.2f".format(trade.usdtValue),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateFormat.format(Date(trade.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (trade.status) {
                        "EXECUTED", "COMPLETED" -> BybitGreen
                        "FAILED" -> BybitRed
                        else -> BybitYellow
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .padding(end = 4.dp)
                    ) {
                        // Status dot would go here
                    }
                    Text(
                        text = trade.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }

            if (trade.reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sebep: ${trade.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
