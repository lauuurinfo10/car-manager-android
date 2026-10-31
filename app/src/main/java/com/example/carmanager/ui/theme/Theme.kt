package com.example.carmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E4FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = BlueGrey40,
    secondaryContainer = Color(0xFFDCE2F5),
    onSecondaryContainer = Color(0xFF121C2B),
    tertiary = Teal40,
    tertiaryContainer = Color(0xFFB4EFE6),
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF181C22),
    surface = Color(0xFFF6F8FC),
    onSurface = Color(0xFF181C22),
    surfaceVariant = Color(0xFFE0E4EE),
    onSurfaceVariant = Color(0xFF5A6070),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF0F3F9),
    surfaceContainerHigh = Color(0xFFE9EDF5),
    surfaceContainerHighest = Color(0xFFE3E7F0),
    outlineVariant = Color(0xFFD5DAE5),
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF1B4585),
    onPrimaryContainer = Color(0xFFD8E4FF),
    secondary = BlueGrey80,
    secondaryContainer = Color(0xFF3D4759),
    tertiary = Teal80,
    background = Color(0xFF10141A),
    onBackground = Color(0xFFE0E2EA),
    surface = Color(0xFF10141A),
    onSurface = Color(0xFFE0E2EA),
    surfaceVariant = Color(0xFF424754),
    onSurfaceVariant = Color(0xFFC2C6D4),
    surfaceContainerLowest = Color(0xFF0B0E14),
    surfaceContainerLow = Color(0xFF181C23),
    surfaceContainer = Color(0xFF1C2027),
    surfaceContainerHigh = Color(0xFF262A32),
    surfaceContainerHighest = Color(0xFF31353D),
    outlineVariant = Color(0xFF424754),
)

@Composable
fun CarManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
