package com.bybit.rebalancer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bybit.rebalancer.ui.components.*
import com.bybit.rebalancer.ui.theme.*
import com.bybit.rebalancer.ui.viewmodel.MainUiState

@Composable
fun ApiSettingsScreen(
    uiState: MainUiState,
    onSaveCredentials: (String, String) -> Unit,
    onTestConnection: () -> Unit,
    onSetTestnet: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKey by remember { mutableStateOf("") }
    var apiSecret by remember { mutableStateOf("") }
    var isTestnet by remember { mutableStateOf(uiState.settings.isTestnet) }

    // Eğer zaten API bilgileri varsa, göster
    LaunchedEffect(uiState.settings) {
        if (uiState.settings.apiKey.isNotBlank()) {
            apiKey = uiState.settings.apiKey
        }
        isTestnet = uiState.settings.isTestnet
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        BybitCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = null,
                    tint = BybitYellow,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "API Ayarları",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Bybit API bilgilerinizi girin",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // API Key
        BybitCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "API Kimlik Bilgileri",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )

                BybitTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = "API Key",
                    leadingIcon = Icons.Default.VpnKey
                )

                BybitTextField(
                    value = apiSecret,
                    onValueChange = { apiSecret = it },
                    label = "API Secret",
                    isPassword = true,
                    leadingIcon = Icons.Default.Lock
                )

                // Testnet Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Testnet Kullan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Demo hesap için testnet'i aktifleştirin",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isTestnet,
                        onCheckedChange = {
                            isTestnet = it
                            onSetTestnet(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BybitYellow,
                            checkedTrackColor = BybitYellow.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        // Info Card
        BybitCard {
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
                        text = "Önemli Bilgi",
                        style = MaterialTheme.typography.titleSmall,
                        color = BybitYellow
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• API anahtarınızı Bybit hesabınızdan alabilirsiniz\n" +
                              "• 'Unified Trading' izinlerinin açık olduğundan emin olun\n" +
                              "• Spot trading ve okuma/yazma izinleri gereklidir\n" +
                              "• API bilgileriniz cihazda şifrelenmiş olarak saklanır",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BybitButton(
                onClick = onTestConnection,
                text = "Bağlantıyı Test Et",
                modifier = Modifier.weight(1f),
                isPrimary = false,
                isLoading = uiState.isLoading,
                enabled = apiKey.isNotBlank() && apiSecret.isNotBlank()
            )
            BybitButton(
                onClick = { onSaveCredentials(apiKey, apiSecret) },
                text = "Kaydet",
                modifier = Modifier.weight(1f),
                isLoading = uiState.isLoading,
                enabled = apiKey.isNotBlank() && apiSecret.isNotBlank()
            )
        }

        // Status
        if (uiState.hasApiCredentials) {
            BybitCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ProfitGreen
                    )
                    Text(
                        text = "API bilgileri kaydedildi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ProfitGreen
                    )
                }
            }
        }
    }
}
