package com.bybit.rebalancer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bybit.rebalancer.data.model.ConnectionStatus
import com.bybit.rebalancer.ui.theme.*

@Composable
fun BybitCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = BybitDarkCard
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun BybitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BybitYellow,
            unfocusedBorderColor = BorderColor,
            focusedLabelColor = BybitYellow,
            cursorColor = BybitYellow
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
fun BybitButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    isPrimary: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) BybitYellow else BybitDarkCard,
            contentColor = if (isPrimary) BybitDark else TextPrimary
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = if (isPrimary) BybitDark else BybitYellow,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StatusIndicator(
    status: ConnectionStatus,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (status) {
        ConnectionStatus.CONNECTED -> ProfitGreen to "Bağlı"
        ConnectionStatus.CONNECTING -> WarningOrange to "Bağlanıyor..."
        ConnectionStatus.DISCONNECTED -> TextSecondary to "Bağlantı Yok"
        ConnectionStatus.ERROR -> LossRed to "Hata"
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
fun PercentageIndicator(
    current: Double,
    target: Double,
    modifier: Modifier = Modifier
) {
    val deviation = current - target
    val color = when {
        deviation > 0.5 -> ProfitGreen
        deviation < -0.5 -> LossRed
        else -> TextSecondary
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "%${"%.2f".format(current)}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (deviation >= 0) "+${"%.2f".format(deviation)}%" else "${"%.2f".format(deviation)}%",
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
fun CoinItem(
    coin: String,
    balance: String,
    usdValue: String,
    currentPercentage: Double,
    targetPercentage: Double,
    onRemove: () -> Unit,
    onPercentageChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }

    BybitCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = coin,
                    style = MaterialTheme.typography.titleMedium,
                    color = BybitYellow,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bakiye: $balance",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "Değer: $${"%.2f".format(usdValue.toDoubleOrNull() ?: 0.0)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Hedef: %${"%.2f".format(targetPercentage)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                PercentageIndicator(
                    current = currentPercentage,
                    target = targetPercentage
                )
            }

            Row {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Düzenle",
                        tint = BybitYellow
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Kaldır",
                        tint = LossRed
                    )
                }
            }
        }
    }

    if (showEditDialog) {
        EditPercentageDialog(
            coin = coin,
            currentPercentage = targetPercentage,
            onDismiss = { showEditDialog = false },
            onConfirm = { newPercentage ->
                onPercentageChange(newPercentage)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun EditPercentageDialog(
    coin: String,
    currentPercentage: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var percentageText by remember { mutableStateOf(currentPercentage.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$coin Hedef Yüzdesi") },
        text = {
            Column {
                Text(
                    text = "Yeni hedef yüzdesini girin (0.01 - 100):",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                BybitTextField(
                    value = percentageText,
                    onValueChange = { percentageText = it },
                    label = "Yüzde (%)",
                    keyboardType = KeyboardType.Decimal
                )
            }
        },
        confirmButton = {
            BybitButton(
                onClick = {
                    percentageText.toDoubleOrNull()?.let { onConfirm(it) }
                },
                text = "Kaydet"
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

@Composable
fun LogLevelChip(
    level: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (color, bgColor) = when (level) {
        "ERROR" -> LossRed to LossRed.copy(alpha = 0.2f)
        "WARNING" -> WarningOrange to WarningOrange.copy(alpha = 0.2f)
        "INFO" -> ProfitGreen to ProfitGreen.copy(alpha = 0.2f)
        "DEBUG" -> TextSecondary to TextSecondary.copy(alpha = 0.2f)
        else -> TextPrimary to BybitDarkCard
    }

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(level) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = bgColor,
            selectedLabelColor = color
        )
    )
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary
) {
    BybitCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = BybitYellow,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = valueColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
