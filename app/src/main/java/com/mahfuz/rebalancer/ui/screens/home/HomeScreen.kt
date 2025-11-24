package com.mahfuz.rebalancer.ui.screens.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahfuz.rebalancer.data.model.ServiceState
import com.mahfuz.rebalancer.ui.components.CoinCard
import com.mahfuz.rebalancer.ui.components.StatusCard
import com.mahfuz.rebalancer.ui.theme.BybitGreen
import com.mahfuz.rebalancer.ui.theme.BybitRed
import com.mahfuz.rebalancer.ui.theme.BybitYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToPortfolio: () -> Unit,
    onNavigateToLogs: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Permission launcher for notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startService(context)
        }
    }

    // Check and request notification permission on Android 13+
    fun startServiceWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.startService(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bybit Rebalancer") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = { viewModel.refreshPortfolio() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                    }
                    IconButton(onClick = onNavigateToLogs) {
                        Icon(Icons.Default.List, contentDescription = "Loglar")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                    }
                }
            )
        }
    ) { padding ->
        if (!uiState.hasApiCredentials) {
            // Show setup prompt
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = BybitYellow
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "API Bilgileri Gerekli",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Başlamak için Bybit API anahtarınızı ve gizli anahtarınızı girin.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onNavigateToSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BybitYellow
                    )
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ayarlara Git")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Card
                item {
                    StatusCard(
                        serviceState = uiState.serviceState,
                        isConnected = uiState.isConnected,
                        totalValue = uiState.portfolioState?.totalValueUsdt
                    )
                }

                // Service Control
                item {
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
                                Column {
                                    Text(
                                        text = "Rebalancing Servisi",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = when (uiState.serviceState) {
                                            ServiceState.RUNNING -> "Portföyünüz izleniyor"
                                            ServiceState.STARTING -> "Başlatılıyor..."
                                            ServiceState.ERROR -> "Hata oluştu"
                                            else -> "Durduruldu"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (uiState.serviceState == ServiceState.RUNNING ||
                                    uiState.serviceState == ServiceState.STARTING
                                ) {
                                    Button(
                                        onClick = { viewModel.stopService(context) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BybitRed
                                        )
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Durdur")
                                    }
                                } else {
                                    Button(
                                        onClick = { startServiceWithPermissionCheck() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BybitGreen
                                        )
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Başlat")
                                    }
                                }
                            }

                            // Rebalancing toggle
                            if (uiState.serviceState == ServiceState.RUNNING) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Otomatik Rebalancing",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "Eşik aşıldığında otomatik işlem yap",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Switch(
                                        checked = uiState.portfolioConfig.isActive,
                                        onCheckedChange = { viewModel.toggleRebalancing(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = BybitYellow,
                                            checkedTrackColor = BybitYellow.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Portfolio Button
                item {
                    OutlinedButton(
                        onClick = onNavigateToPortfolio,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BybitYellow
                        )
                    ) {
                        Icon(Icons.Default.PieChart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Portföy Ayarları")
                    }
                }

                // Loading indicator
                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BybitYellow)
                        }
                    }
                }

                // Error message
                uiState.error?.let { error ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = BybitRed.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = BybitRed
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = error,
                                    color = BybitRed
                                )
                            }
                        }
                    }
                }

                // Portfolio Holdings
                uiState.portfolioState?.let { state ->
                    if (state.holdings.isNotEmpty()) {
                        item {
                            Text(
                                text = "Portföy",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        items(state.holdings) { holding ->
                            CoinCard(holding = holding)
                        }
                    }
                }
            }
        }
    }
}
