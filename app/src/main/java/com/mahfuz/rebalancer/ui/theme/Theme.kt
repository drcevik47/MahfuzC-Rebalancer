package com.mahfuz.rebalancer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Bybit Theme Colors
val BybitYellow = Color(0xFFF7A600)
val BybitYellowDark = Color(0xFFC78500)
val BybitBlack = Color(0xFF0D0D0D)
val BybitDarkGray = Color(0xFF1E2026)
val BybitGray = Color(0xFF2B2F36)
val BybitLightGray = Color(0xFF474D57)
val BybitWhite = Color(0xFFFFFFFF)
val BybitGreen = Color(0xFF00C853)
val BybitRed = Color(0xFFFF5252)

private val DarkColorScheme = darkColorScheme(
    primary = BybitYellow,
    onPrimary = BybitBlack,
    primaryContainer = BybitYellowDark,
    onPrimaryContainer = BybitBlack,
    secondary = BybitGray,
    onSecondary = BybitWhite,
    secondaryContainer = BybitLightGray,
    onSecondaryContainer = BybitWhite,
    tertiary = BybitGreen,
    onTertiary = BybitBlack,
    background = BybitBlack,
    onBackground = BybitWhite,
    surface = BybitDarkGray,
    onSurface = BybitWhite,
    surfaceVariant = BybitGray,
    onSurfaceVariant = BybitWhite,
    error = BybitRed,
    onError = BybitWhite,
    outline = BybitLightGray
)

@Composable
fun BybitRebalancerTheme(
    darkTheme: Boolean = true, // Always dark theme for trading app
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
