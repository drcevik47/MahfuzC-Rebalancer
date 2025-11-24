package com.bybit.rebalancer.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bybit.rebalancer.data.model.ConnectionStatus
import com.bybit.rebalancer.service.RebalancerService
import com.bybit.rebalancer.ui.components.*
import com.bybit.rebalancer.ui.theme.*
import com.bybit.rebalancer.ui.viewmodel.MainUiState

@Composable
fun DashboardScreen(
    uiState: MainUiState,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Bybit Rebalancer",
                    style = MaterialTheme.typography.headlineMedium,
                    color = BybitYellow,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Portföy Dengeleme Sistemi",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Yenile",
                    tint = BybitYellow
                )
            }
        }

        // Status Card
        BybitCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Servis Durumu",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    StatusIndicator(status = uiState.serviceState.connectionStatus)
                }

                HorizontalDivider(color = BorderColor)

                // Service Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BybitButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onStartService()
                            }
                        },
                        text = "Başlat",
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.serviceState.isRunning && uiState.hasApiCredentials
                    )
                    BybitButton(
                        onClick = onStopService,
                        text = "Durdur",
                        modifier = Modifier.weight(1f),
                        isPrimary = false,
                        enabled = uiState.serviceState.isRunning
                    )
                }

                // Warning if no API credentials
                if (!uiState.hasApiCredentials) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Önce API ayarlarını yapın",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarningOrange
                        )
                    }
                }

                // Last check/rebalance times
                uiState.serviceState.lastCheckTime?.let {
                    Text(
                        text = "Son Kontrol: ${formatTime(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                uiState.serviceState.lastRebalanceTime?.let {
                    Text(
                        text = "Son Dengeleme: ${formatTime(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ProfitGreen
                    )
                }
            }
        }

        // Portfolio Summary
        if (uiState.portfolioSnapshot != null) {
            BybitCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = BybitYellow
                        )
                        Text(
                            text = "Portföy Özeti",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "$${"%.2f".format(uiState.portfolioSnapshot.totalValueUsdt)}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = BybitYellow,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider(color = BorderColor)

                    // Coin breakdown
                    uiState.portfolioSnapshot.coins.forEach { coinSnapshot ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = coinSnapshot.coin,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$${"%.2f".format(coinSnapshot.usdtValue)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            PercentageIndicator(
                                current = coinSnapshot.currentPercentage,
                                target = coinSnapshot.targetPercentage
                            )
                        }
                    }
                }
            }
        } else if (uiState.hasApiCredentials) {
            // Loading or no portfolio
            BybitCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = BybitYellow)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Portföy yükleniyor...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Portföy ekranından coin ekleyin",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Settings Summary
        BybitCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = BybitYellow
                    )
                    Text(
                        text = "Aktif Ayarlar",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                HorizontalDivider(color = BorderColor)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Dengeleme Eşiği", color = TextSecondary)
                    Text("%${uiState.settings.rebalanceThreshold}", color = BybitYellow)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Min. İşlem", color = TextSecondary)
                    Text("${uiState.settings.minTradeUsdt} USDT", color = BybitYellow)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Kontrol Aralığı", color = TextSecondary)
                    Text("${uiState.settings.checkIntervalSeconds} sn", color = BybitYellow)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mod", color = TextSecondary)
                    Text(
                        if (uiState.settings.isTestnet) "Testnet" else "Mainnet",
                        color = if (uiState.settings.isTestnet) WarningOrange else ProfitGreen
                    )
                }
            }
        }

        // Info Card
        BybitCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = BybitYellow
                )
                Column {
                    Text(
                        text = "Nasıl Çalışır?",
                        style = MaterialTheme.typography.titleSmall,
                        color = BybitYellow
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1. API ayarlarını yapın\n" +
                              "2. Portföye coin ekleyin ve hedef yüzdelerini belirleyin\n" +
                              "3. Dengeleme ayarlarını yapın\n" +
                              "4. Servisi başlatın\n\n" +
                              "Uygulama arka planda çalışarak portföyünüzü izler ve gerektiğinde otomatik dengeleme yapar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
