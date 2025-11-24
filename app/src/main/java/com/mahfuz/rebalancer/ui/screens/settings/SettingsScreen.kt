package com.mahfuz.rebalancer.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahfuz.rebalancer.ui.components.SectionHeader
import com.mahfuz.rebalancer.ui.components.SettingsItem
import com.mahfuz.rebalancer.ui.theme.BybitGreen
import com.mahfuz.rebalancer.ui.theme.BybitRed
import com.mahfuz.rebalancer.ui.theme.BybitYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showApiSecret by remember { mutableStateOf(false) }
    var showClearLogsDialog by remember { mutableStateOf(false) }

    // Show success snackbar
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            // Reset after showing
            kotlinx.coroutines.delay(2000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // API Settings Section
            item {
                SectionHeader(title = "API Ayarları")
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // API Key
                        OutlinedTextField(
                            value = uiState.apiKey,
                            onValueChange = { viewModel.updateApiKey(it) },
                            label = { Text("API Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Key, contentDescription = null)
                            }
                        )

                        // API Secret
                        OutlinedTextField(
                            value = uiState.apiSecret,
                            onValueChange = { viewModel.updateApiSecret(it) },
                            label = { Text("API Secret") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = if (showApiSecret) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { showApiSecret = !showApiSecret }) {
                                    Icon(
                                        if (showApiSecret) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = "Göster/Gizle"
                                    )
                                }
                            }
                        )

                        // Testnet toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Testnet Kullan")
                                Text(
                                    text = "Test ağında işlem yap",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.isTestnet,
                                onCheckedChange = { viewModel.updateTestnet(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BybitYellow,
                                    checkedTrackColor = BybitYellow.copy(alpha = 0.5f)
                                )
                            )
                        }

                        // Test Result
                        uiState.testResult?.let { result ->
                            val isSuccess = result.startsWith("Bağlantı başarılı")
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSuccess) {
                                        BybitGreen.copy(alpha = 0.2f)
                                    } else {
                                        BybitRed.copy(alpha = 0.2f)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (isSuccess) BybitGreen else BybitRed
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = result,
                                        color = if (isSuccess) BybitGreen else BybitRed
                                    )
                                }
                            }
                        }

                        // Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.testConnection() },
                                enabled = !uiState.isTesting && uiState.apiKey.isNotBlank() && uiState.apiSecret.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (uiState.isTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.NetworkCheck, contentDescription = null)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test Et")
                            }

                            Button(
                                onClick = { viewModel.saveSettings() },
                                enabled = !uiState.isSaving,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BybitYellow
                                )
                            ) {
                                if (uiState.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Kaydet")
                            }
                        }

                        if (uiState.saveSuccess) {
                            Text(
                                text = "Ayarlar kaydedildi",
                                color = BybitGreen,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Service Settings Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = "Servis Ayarları")
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column {
                        SettingsItem(
                            icon = Icons.Default.PowerSettingsNew,
                            title = "Açılışta Başlat",
                            subtitle = "Telefon açıldığında servisi otomatik başlat",
                            trailing = {
                                Switch(
                                    checked = uiState.autoStartOnBoot,
                                    onCheckedChange = { viewModel.updateAutoStart(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BybitYellow,
                                        checkedTrackColor = BybitYellow.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        )

                        Divider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsItem(
                            icon = Icons.Default.Notifications,
                            title = "Bildirimler",
                            subtitle = "İşlem bildirimlerini göster",
                            trailing = {
                                Switch(
                                    checked = uiState.notificationsEnabled,
                                    onCheckedChange = { viewModel.updateNotifications(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BybitYellow,
                                        checkedTrackColor = BybitYellow.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        )
                    }
                }
            }

            // Data Settings Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = "Veri Ayarları")
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column {
                        SettingsItem(
                            icon = Icons.Default.Storage,
                            title = "Log Saklama Süresi",
                            subtitle = "${uiState.logRetentionDays} gün",
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        if (uiState.logRetentionDays > 1) {
                                            viewModel.updateLogRetention(uiState.logRetentionDays - 1)
                                        }
                                    }) {
                                        Icon(Icons.Default.Remove, contentDescription = "Azalt")
                                    }
                                    Text("${uiState.logRetentionDays}")
                                    IconButton(onClick = {
                                        if (uiState.logRetentionDays < 90) {
                                            viewModel.updateLogRetention(uiState.logRetentionDays + 1)
                                        }
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "Artır")
                                    }
                                }
                            }
                        )

                        Divider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsItem(
                            icon = Icons.Default.DeleteForever,
                            title = "Tüm Logları Temizle",
                            subtitle = "Tüm log kayıtlarını sil",
                            onClick = { showClearLogsDialog = true }
                        )
                    }
                }
            }

            // Version info
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Bybit Rebalancer v1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Clear logs confirmation dialog
        if (showClearLogsDialog) {
            AlertDialog(
                onDismissRequest = { showClearLogsDialog = false },
                title = { Text("Logları Temizle") },
                text = { Text("Tüm log kayıtları silinecek. Bu işlem geri alınamaz.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearLogs()
                            showClearLogsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BybitRed)
                    ) {
                        Text("Temizle")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showClearLogsDialog = false }) {
                        Text("İptal")
                    }
                }
            )
        }
    }
}
