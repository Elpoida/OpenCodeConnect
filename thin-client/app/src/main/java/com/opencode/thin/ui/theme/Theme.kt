package com.opencode.thin.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF0D1117)
val DarkSurface = Color(0xFF161B22)
val DarkSurfaceVariant = Color(0xFF21262D)
val AccentOrange = Color(0xFFCA4C07)
val AccentOrangeLight = Color(0xFFEA6A24)
val AccentOrangeDark = Color(0xFF8C3306)
val AccentGreen = Color(0xFF3FB950)
val AccentBlue = Color(0xFF58A6FF)
val TextPrimary = Color(0xFFE6EDF3)
val TextSecondary = Color(0xFF8B949E)
val ErrorRed = Color(0xFFF85149)
val WarningOrange = Color(0xFFD29922)
val BorderGray = Color(0xFF30363D)
val BuildBlue = Color(0xFF5C9CF5)
val PlanOrange = Color(0xFFF5A742)

private val DarkColorScheme = darkColorScheme(
    primary = AccentOrange,
    onPrimary = Color.Black,
    secondary = AccentOrangeLight,
    onSecondary = Color.Black,
    tertiary = AccentOrangeDark,
    onTertiary = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Color.White,
    outline = BorderGray,
)

@Composable
fun OpenCodeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
