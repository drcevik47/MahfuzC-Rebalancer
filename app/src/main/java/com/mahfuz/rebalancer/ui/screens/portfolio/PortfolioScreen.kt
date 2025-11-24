package com.mahfuz.rebalancer.ui.screens.portfolio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahfuz.rebalancer.data.db.PortfolioCoinEntity
import com.mahfuz.rebalancer.ui.theme.BybitGreen
import com.mahfuz.rebalancer.ui.theme.BybitRed
import com.mahfuz.rebalancer.ui.theme.BybitYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    onNavigateBack: () -> Unit,
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddCoinDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Portföy Ayarları") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.addFromWallet() }) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Cüzdandan Ekle")
                    }
                    IconButton(onClick = { viewModel.distributeEvenly() }) {
                        Icon(Icons.Default.Balance, contentDescription = "Eşit Dağıt")
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Temizle")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCoinDialog = true },
                containerColor = BybitYellow
            ) {
                Icon(Icons.Default.Add, contentDescription = "Coin Ekle")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Threshold setting
            item {
                ThresholdCard(
                    threshold = uiState.threshold,
                    onThresholdChange = { viewModel.setThreshold(it) }
                )
            }

            // Total percentage indicator
            item {
                TotalPercentageCard(
                    total = uiState.totalPercentage,
                    isValid = uiState.isValid
                )
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

            // Portfolio coins
            if (uiState.portfolioCoins.isNotEmpty()) {
                item {
                    Text(
                        text = "Portföy Coinleri",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(
                    items = uiState.portfolioCoins,
                    key = { it.coin }
                ) { coin ->
                    PortfolioCoinCard(
                        coin = coin,
                        onPercentageChange = { viewModel.updateCoinPercentage(coin.coin, it) },
                        onActiveChange = { viewModel.setCoinActive(coin.coin, it) },
                        onRemove = { viewModel.removeCoin(coin.coin) }
                    )
                }
            } else if (!uiState.isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.PieChart,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Henüz coin eklenmedi",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Portföyünüze coin eklemek için + butonuna tıklayın",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Spacer for FAB
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Add Coin Dialog
    if (showAddCoinDialog) {
        AddCoinDialog(
            availableCoins = uiState.availableCoins.filter { coin ->
                coin !in uiState.portfolioCoins.map { it.coin }
            },
            onDismiss = { showAddCoinDialog = false },
            onAdd = { coin, percentage ->
                viewModel.addCoin(coin, percentage)
                showAddCoinDialog = false
            }
        )
    }

    // Clear confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Portföyü Temizle") },
            text = { Text("Tüm coinler portföyden kaldırılacak. Devam etmek istiyor musunuz?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearPortfolio()
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
fun ThresholdCard(
    threshold: Double,
    onThresholdChange: (Double) -> Unit
) {
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
                        text = "Rebalance Eşiği",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Sapma bu değeri aşınca rebalance yapılır",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "%.2f%%".format(threshold),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = BybitYellow
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Slider(
                value = threshold.toFloat(),
                onValueChange = { onThresholdChange(it.toDouble()) },
                valueRange = 0.01f..10f,
                steps = 99,
                colors = SliderDefaults.colors(
                    thumbColor = BybitYellow,
                    activeTrackColor = BybitYellow
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0.01%", style = MaterialTheme.typography.labelSmall)
                Text("10%", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun TotalPercentageCard(
    total: Double,
    isValid: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isValid) {
                BybitGreen.copy(alpha = 0.2f)
            } else {
                BybitRed.copy(alpha = 0.2f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isValid) BybitGreen else BybitRed
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Toplam Hedef",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (isValid) "Geçerli" else "Toplam 100% olmalı",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isValid) BybitGreen else BybitRed
                    )
                }
            }
            Text(
                text = "%.2f%%".format(total),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isValid) BybitGreen else BybitRed
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioCoinCard(
    coin: PortfolioCoinEntity,
    onPercentageChange: (Double) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onRemove: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember { mutableStateOf(coin.targetPercentage.toString()) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active toggle
            Checkbox(
                checked = coin.isActive,
                onCheckedChange = onActiveChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = BybitYellow
                )
            )

            // Coin name
            Text(
                text = coin.coin,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(80.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Percentage
            if (isEditing) {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    modifier = Modifier.width(100.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    suffix = { Text("%") },
                    trailingIcon = {
                        IconButton(onClick = {
                            editValue.toDoubleOrNull()?.let { onPercentageChange(it) }
                            isEditing = false
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Onayla")
                        }
                    }
                )
            } else {
                TextButton(onClick = {
                    editValue = coin.targetPercentage.toString()
                    isEditing = true
                }) {
                    Text(
                        text = "%.2f%%".format(coin.targetPercentage),
                        style = MaterialTheme.typography.titleMedium,
                        color = BybitYellow
                    )
                }
            }

            // Remove button
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Kaldır",
                    tint = BybitRed
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCoinDialog(
    availableCoins: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String, Double) -> Unit
) {
    var selectedCoin by remember { mutableStateOf("") }
    var percentage by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCoins = remember(searchQuery, availableCoins) {
        if (searchQuery.isBlank()) availableCoins
        else availableCoins.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Coin Ekle") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Coin selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCoin.ifBlank { searchQuery },
                        onValueChange = {
                            searchQuery = it
                            selectedCoin = ""
                            expanded = true
                        },
                        label = { Text("Coin Seç") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = expanded && filteredCoins.isNotEmpty(),
                        onDismissRequest = { expanded = false }
                    ) {
                        filteredCoins.take(10).forEach { coin ->
                            DropdownMenuItem(
                                text = { Text(coin) },
                                onClick = {
                                    selectedCoin = coin
                                    searchQuery = coin
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Percentage input
                OutlinedTextField(
                    value = percentage,
                    onValueChange = { percentage = it },
                    label = { Text("Hedef Yüzde") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text("%") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pct = percentage.toDoubleOrNull() ?: 0.0
                    if (selectedCoin.isNotBlank() && pct > 0) {
                        onAdd(selectedCoin, pct)
                    }
                },
                enabled = selectedCoin.isNotBlank() && (percentage.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = BybitYellow)
            ) {
                Text("Ekle")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
