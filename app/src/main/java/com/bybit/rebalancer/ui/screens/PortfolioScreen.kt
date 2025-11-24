package com.bybit.rebalancer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bybit.rebalancer.data.model.PortfolioCoin
import com.bybit.rebalancer.ui.components.*
import com.bybit.rebalancer.ui.theme.*
import com.bybit.rebalancer.ui.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    uiState: MainUiState,
    onAddCoin: (String, Double) -> Unit,
    onRemoveCoin: (String) -> Unit,
    onUpdatePercentage: (String, Double) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

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
            Column {
                Text(
                    text = "Portföy",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Coin seçimi ve hedef yüzdeleri",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Row {
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Yenile",
                        tint = BybitYellow
                    )
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Coin Ekle",
                        tint = BybitYellow
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Toplam Değer ve Yüzde Kartı
        if (uiState.portfolioSnapshot != null) {
            BybitCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = "Toplam Portföy Değeri",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "$${"%.2f".format(uiState.portfolioSnapshot.totalValueUsdt)}",
                        style = MaterialTheme.typography.headlineLarge,
                        color = BybitYellow,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Toplam hedef yüzdesi kontrolü
                    val totalTarget = uiState.portfolioCoins.sumOf { it.targetPercentage }
                    val isValid = totalTarget in 99.5..100.5

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isValid) ProfitGreen else WarningOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Toplam Hedef: %${"%.2f".format(totalTarget)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isValid) ProfitGreen else WarningOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Coin Listesi
        if (uiState.portfolioCoins.isEmpty()) {
            EmptyState(
                icon = Icons.Default.AccountBalanceWallet,
                title = "Henüz coin eklenmedi",
                subtitle = "Portföyünüze coin eklemek için + butonuna tıklayın",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.portfolioCoins) { coin ->
                    val balance = uiState.walletBalances.find { it.coin == coin.coin }
                    val snapshot = uiState.portfolioSnapshot?.coins?.find { it.coin == coin.coin }

                    CoinItem(
                        coin = coin.coin,
                        balance = balance?.walletBalance ?: "0",
                        usdValue = balance?.usdValue ?: "0",
                        currentPercentage = snapshot?.currentPercentage ?: 0.0,
                        targetPercentage = coin.targetPercentage,
                        onRemove = { onRemoveCoin(coin.coin) },
                        onPercentageChange = { onUpdatePercentage(coin.coin, it) }
                    )
                }
            }
        }
    }

    // Add Coin Dialog
    if (showAddDialog) {
        AddCoinDialog(
            availableCoins = uiState.availableCoins.filter { coin ->
                uiState.portfolioCoins.none { it.coin == coin }
            },
            walletBalances = uiState.walletBalances,
            onDismiss = { showAddDialog = false },
            onAdd = { coin, percentage ->
                onAddCoin(coin, percentage)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCoinDialog(
    availableCoins: List<String>,
    walletBalances: List<com.bybit.rebalancer.data.model.CoinBalance>,
    onDismiss: () -> Unit,
    onAdd: (String, Double) -> Unit
) {
    var selectedCoin by remember { mutableStateOf("") }
    var percentage by remember { mutableStateOf("10") }
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCoins = remember(searchQuery, availableCoins) {
        if (searchQuery.isBlank()) {
            // USDT'yi ve cüzdanda bakiyesi olan coinleri önce göster
            val withBalance = walletBalances
                .filter { it.walletBalance.toDoubleOrNull() ?: 0.0 > 0 }
                .map { it.coin }
                .filter { it in availableCoins || it == "USDT" }

            val others = availableCoins.filter { it !in withBalance }
            (listOf("USDT") + withBalance + others).distinct().take(50)
        } else {
            (listOf("USDT") + availableCoins)
                .distinct()
                .filter { it.contains(searchQuery, ignoreCase = true) }
                .take(50)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Coin Ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Search / Select
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    BybitTextField(
                        value = if (selectedCoin.isNotBlank()) selectedCoin else searchQuery,
                        onValueChange = {
                            searchQuery = it
                            selectedCoin = ""
                            expanded = true
                        },
                        label = "Coin Seç veya Ara",
                        modifier = Modifier.menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        filteredCoins.forEach { coin ->
                            val balance = walletBalances.find { it.coin == coin }
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(coin)
                                        if (balance != null) {
                                            Text(
                                                text = balance.walletBalance.take(10),
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedCoin = coin
                                    searchQuery = coin
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Percentage
                BybitTextField(
                    value = percentage,
                    onValueChange = { percentage = it },
                    label = "Hedef Yüzde (%)",
                    keyboardType = KeyboardType.Decimal
                )

                // Preview
                if (selectedCoin.isNotBlank()) {
                    val balance = walletBalances.find { it.coin == selectedCoin }
                    if (balance != null) {
                        BybitCard {
                            Column {
                                Text(
                                    text = "Cüzdan Bakiyesi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${balance.walletBalance} $selectedCoin",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "≈ $${"%.2f".format(balance.usdValue.toDoubleOrNull() ?: 0.0)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            BybitButton(
                onClick = {
                    val coin = selectedCoin.ifBlank { searchQuery }
                    val pct = percentage.toDoubleOrNull() ?: 0.0
                    if (coin.isNotBlank() && pct > 0) {
                        onAdd(coin.uppercase(), pct)
                    }
                },
                text = "Ekle",
                enabled = (selectedCoin.isNotBlank() || searchQuery.isNotBlank()) &&
                         (percentage.toDoubleOrNull() ?: 0.0) > 0
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = TextSecondary)
            }
        },
        containerColor = BybitDarkSurface
    )
}
