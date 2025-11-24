package com.mahfuz.rebalancer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mahfuz.rebalancer.data.model.CoinHolding
import com.mahfuz.rebalancer.data.model.ServiceState
import com.mahfuz.rebalancer.ui.theme.BybitGreen
import com.mahfuz.rebalancer.ui.theme.BybitRed
import com.mahfuz.rebalancer.ui.theme.BybitYellow
import java.math.BigDecimal
import java.text.DecimalFormat

@Composable
fun StatusCard(
    serviceState: ServiceState,
    isConnected: Boolean,
    totalValue: BigDecimal?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                Text(
                    text = "Durum",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                StatusBadge(serviceState = serviceState, isConnected = isConnected)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Toplam Portföy Değeri",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = totalValue?.let { formatUsdValue(it) } ?: "---",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = BybitYellow
            )
        }
    }
}

@Composable
fun StatusBadge(
    serviceState: ServiceState,
    isConnected: Boolean
) {
    val (color, text, icon) = when {
        serviceState == ServiceState.RUNNING && isConnected -> Triple(BybitGreen, "Çalışıyor", Icons.Default.CheckCircle)
        serviceState == ServiceState.RUNNING && !isConnected -> Triple(BybitYellow, "Bağlanıyor", Icons.Default.Sync)
        serviceState == ServiceState.STARTING -> Triple(BybitYellow, "Başlatılıyor", Icons.Default.HourglassEmpty)
        serviceState == ServiceState.ERROR -> Triple(BybitRed, "Hata", Icons.Default.Error)
        serviceState == ServiceState.PAUSED -> Triple(BybitYellow, "Duraklatıldı", Icons.Default.Pause)
        else -> Triple(MaterialTheme.colorScheme.outline, "Durduruldu", Icons.Default.Stop)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
fun CoinCard(
    holding: CoinHolding,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = holding.coin,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatCoinAmount(holding.balance),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatUsdValue(holding.usdtValue),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatPercentage(holding.currentPercentage)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (holding.targetPercentage > BigDecimal.ZERO) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${formatPercentage(holding.targetPercentage)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = BybitYellow
                        )
                    }
                }

                // Deviation indicator
                if (holding.targetPercentage > BigDecimal.ZERO) {
                    val deviationColor = when {
                        holding.deviation.abs() < BigDecimal("0.5") -> BybitGreen
                        holding.deviation.abs() < BigDecimal("2") -> BybitYellow
                        else -> BybitRed
                    }
                    val sign = if (holding.deviation >= BigDecimal.ZERO) "+" else ""
                    Text(
                        text = "$sign${formatPercentage(holding.deviation)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = deviationColor
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BybitYellow
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            trailing?.invoke()
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// Formatting helpers
private val usdFormat = DecimalFormat("#,##0.00")
private val coinFormat = DecimalFormat("#,##0.########")
private val percentFormat = DecimalFormat("#0.00")

fun formatUsdValue(value: BigDecimal): String = "$${usdFormat.format(value)}"
fun formatCoinAmount(value: BigDecimal): String = coinFormat.format(value)
fun formatPercentage(value: BigDecimal): String = percentFormat.format(value)
