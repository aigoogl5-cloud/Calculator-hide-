package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SleekDarkColorScheme = darkColorScheme(
    primary = AccentAmber,
    onPrimary = TextOnAmber,
    primaryContainer = AccentAmberGlow,
    onPrimaryContainer = AccentAmber,
    secondary = AccentCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0x3364D2FF),
    onSecondaryContainer = AccentCyan,
    tertiary = AccentEmerald,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderGlass,
    error = AccentRose,
    onError = Color.White
)

@Composable
fun SmartCalculatorTheme(
    content: @Composable () -> Unit
) {
    // Default to sleek dark mode for maximum stealth, high-contrast readability, and OLED efficiency
    MaterialTheme(
        colorScheme = SleekDarkColorScheme,
        typography = Typography,
        content = content
    )
}
