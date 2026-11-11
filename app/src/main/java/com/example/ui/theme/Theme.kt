package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Color(0xFF002228),
    primaryContainer = CyanAccentDark,
    onPrimaryContainer = CyanAccent,
    secondary = PurpleAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF381E72),
    onSecondaryContainer = Color(0xFFEADDFF),
    tertiary = GreenBest,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = DarkSurfaceContainer,
    outline = BorderSubtle,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MangaCompareTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
