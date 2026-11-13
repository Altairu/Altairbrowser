package com.example.altairbrowser.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// 常にダークテーマを使用
private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = TextPrimary,
    primaryContainer = AccentBlueMuted,
    onPrimaryContainer = TextPrimary,
    secondary = AccentBlue,
    onSecondary = BackgroundDark,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = ErrorRed,
    onError = TextPrimary,
)

@Composable
fun AltairBrowserTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
