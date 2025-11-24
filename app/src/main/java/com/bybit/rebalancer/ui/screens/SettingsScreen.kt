package com.bybit.rebalancer.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bybit.rebalancer.service.RebalancerService
import com.bybit.rebalancer.ui.components.*
import com.bybit.rebalancer.ui.theme.*
import com.bybit.rebalancer.ui.viewmodel.MainUiState

@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onUpdateThreshold: (Double) -> Unit,
    onUpdateMinTrade: (Double) -> Unit,
    onUpdateCheckInterval: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var threshold by remember { mutableStateOf(uiState.settings.rebalanceThreshold.toString()) }
    var minTrade by remember { mutableStateOf(uiState.settings.minTradeUsdt.toString()) }
    var checkInterval by remember { mutableStateOf(uiState.settings.checkIntervalSeconds.toString()) }

    LaunchedEffect(uiState.settings) {
        threshold = uiState.settings.rebalanceThreshold.toString()
        minTrade = uiState.settings.minTradeUsdt.toString()
        checkInterval = uiState.settings.checkIntervalSeconds.toString()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Ayarlar",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        // Rebalance Ayarları
        BybitCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Balance,
                        contentDescription = null,
                        tint = BybitYellow
                    )
                    Text(
                        text = "Dengeleme Ayarları",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                // Threshold
                Column {
                    Text(
                        text = "Dengeleme Eşiği (%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Portföy bu yüzde kadar sapma gösterdiğinde dengeleme yapılır",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BybitTextField(
                            value = threshold,
                            onValueChange = { threshold = it },
                            label = "Eşik (%)",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        BybitButton(
                            onClick = {
                                threshold.toDoubleOrNull()?.let { onUpdateThreshold(it) }
                            },
                            text = "Uygula",
                            modifier = Modifier.width(100.dp)
                        )
                    }

                    // Quick select buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.01, 0.1, 0.5, 1.0, 2.0, 5.0).forEach { value ->
                            FilterChip(
                                selected = threshold.toDoubleOrNull() == value,
                                onClick = {
                                    threshold = value.toString()
                                    onUpdateThreshold(value)
                                },
                                label = { Text("%$value") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BybitYellow.copy(alpha = 0.3f),
                                    selectedLabelColor = BybitYellow
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderColor)

                // Min Trade Amount
                Column {
                    Text(
                        text = "Minimum İşlem Tutarı (USDT)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Bu değerin altındaki işlemler yapılmaz",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BybitTextField(
                            value = minTrade,
                            onValueChange = { minTrade = it },
                            label = "Min. USDT",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        BybitButton(
                            onClick = {
                                minTrade.toDoubleOrNull()?.let { onUpdateMinTrade(it) }
                            },
                            text = "Uygula",
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }

                HorizontalDivider(color = BorderColor)

                // Check Interval
                Column {
                    Text(
                        text = "Kontrol Aralığı (Saniye)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Portföy bu süre aralıklarında kontrol edilir",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BybitTextField(
                            value = checkInterval,
                            onValueChange = { checkInterval = it },
                            label = "Saniye",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                        BybitButton(
                            onClick = {
                                checkInterval.toIntOrNull()?.let { onUpdateCheckInterval(it) }
                            },
                            text = "Uygula",
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }
            }
        }

        // Sistem Ayarları
        BybitCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = BybitYellow
                    )
                    Text(
                        text = "Sistem Ayarları",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                // Battery Optimization
                BybitButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    },
                    text = "Pil Optimizasyonunu Kapat",
                    modifier = Modifier.fillMaxWidth(),
                    isPrimary = false
                )

                Text(
                    text = "Uygulamanın arka planda kesintisiz çalışması için pil optimizasyonunu kapatmanız önerilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                HorizontalDivider(color = BorderColor)

                // App Settings
                BybitButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    },
                    text = "Uygulama Ayarları",
                    modifier = Modifier.fillMaxWidth(),
                    isPrimary = false
                )
            }
        }

        // Servis Durumu
        BybitCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = BybitYellow
                    )
                    Text(
                        text = "Servis Durumu",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                StatusIndicator(status = uiState.serviceState.connectionStatus)

                if (uiState.serviceState.lastCheckTime != null) {
                    Text(
                        text = "Son Kontrol: ${formatTimestamp(uiState.serviceState.lastCheckTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                if (uiState.serviceState.lastRebalanceTime != null) {
                    Text(
                        text = "Son Dengeleme: ${formatTimestamp(uiState.serviceState.lastRebalanceTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                uiState.serviceState.errorMessage?.let { error ->
                    Text(
                        text = "Hata: $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = LossRed
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
