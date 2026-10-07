package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZoyaDarkColorScheme = darkColorScheme(
    primary = NeonMagenta,
    onPrimary = Color.White,
    primaryContainer = NeonPurple,
    onPrimaryContainer = Color.White,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = ZoyaSurfaceVariant,
    onSecondaryContainer = NeonCyan,
    tertiary = NeonEmerald,
    onTertiary = Color.Black,
    background = ZoyaDarkBackground,
    onBackground = TextPrimary,
    surface = ZoyaSurface,
    onSurface = TextPrimary,
    surfaceVariant = ZoyaSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = ZoyaCardBorder
)

@Composable
fun ZoyaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Zoya is an immersive dark futuristic experience
    MaterialTheme(
        colorScheme = ZoyaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
