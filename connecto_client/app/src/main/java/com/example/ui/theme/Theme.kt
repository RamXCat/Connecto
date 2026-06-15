package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StitchDarkColorScheme = darkColorScheme(
    primary = TealAccent,
    onPrimary = OnPrimaryDark,
    secondary = CardSurface,
    onSecondary = TextPrimary,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    error = ShutdownRed,
    onError = OnErrorColor,
    surfaceVariant = BackgroundDark,
    onSurfaceVariant = TextMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark mode only
    dynamicColor: Boolean = false, // Enforce brand colors strictly
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = StitchDarkColorScheme,
        typography = Typography,
        content = content
    )
}
