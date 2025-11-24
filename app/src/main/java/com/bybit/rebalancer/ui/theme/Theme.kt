package com.bybit.rebalancer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Bybit Brand Colors
val BybitYellow = Color(0xFFF7A600)
val BybitYellowLight = Color(0xFFFFD54F)
val BybitDark = Color(0xFF0D1117)
val BybitDarkSurface = Color(0xFF161B22)
val BybitDarkCard = Color(0xFF21262D)

// Status Colors
val ProfitGreen = Color(0xFF00C853)
val LossRed = Color(0xFFFF5252)
val WarningOrange = Color(0xFFFF9800)

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8B949E)
val TextDisabled = Color(0xFF484F58)

// Border Colors
val BorderColor = Color(0xFF30363D)

private val DarkColorScheme = darkColorScheme(
    primary = BybitYellow,
    onPrimary = BybitDark,
    primaryContainer = BybitYellowLight,
    onPrimaryContainer = BybitDark,
    secondary = BybitYellowLight,
    onSecondary = BybitDark,
    secondaryContainer = BybitDarkCard,
    onSecondaryContainer = TextPrimary,
    tertiary = ProfitGreen,
    onTertiary = BybitDark,
    background = BybitDark,
    onBackground = TextPrimary,
    surface = BybitDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = BybitDarkCard,
    onSurfaceVariant = TextSecondary,
    error = LossRed,
    onError = TextPrimary,
    outline = BorderColor,
    outlineVariant = BorderColor
)

@Composable
fun BybitRebalancerTheme(
    darkTheme: Boolean = true, // Her zaman dark theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
