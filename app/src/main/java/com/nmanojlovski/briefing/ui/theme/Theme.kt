package com.nmanojlovski.briefing.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The app's own colours (built around the icon's navy and sun yellow), instead of
// Android's "dynamic colours" that change with your wallpaper.
// Each "on…" colour is the text/icon colour used on top of the colour with the same name.
private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4F7),
    onPrimaryContainer = Color(0xFF0A1F38),
    secondary = Color(0xFF4F6378),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE6F2),
    onSecondaryContainer = Color(0xFF1A2633),
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDFA3),
    onTertiaryContainer = Color(0xFF2C1B00),
    background = Color(0xFFF2F5F9),
    onBackground = Color(0xFF161C24),
    surface = Color(0xFFF2F5F9),
    onSurface = Color(0xFF161C24),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF4A515C),
    outline = Color(0xFF727883),
    outlineVariant = Color(0xFFC6CBD3),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,           // cards
    surfaceContainer = Color(0xFFEEF2F7),
    surfaceContainerHigh = Color(0xFFE6EBF1),
    surfaceContainerHighest = Color(0xFFDEE4EB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7EE),
    onPrimary = Color(0xFF0E2A47),
    primaryContainer = Color(0xFF24476F),
    onPrimaryContainer = Color(0xFFD6E4F7),
    secondary = Color(0xFFB8C8DA),
    onSecondary = Color(0xFF233140),
    secondaryContainer = Color(0xFF34455A),
    onSecondaryContainer = Color(0xFFD6E3F1),
    tertiary = Color(0xFFFFC24D),
    onTertiary = Color(0xFF452B00),
    tertiaryContainer = Color(0xFF634100),
    onTertiaryContainer = Color(0xFFFFDFA3),
    background = Color(0xFF0E1218),
    onBackground = Color(0xFFDFE3EA),
    surface = Color(0xFF0E1218),
    onSurface = Color(0xFFDFE3EA),
    surfaceVariant = Color(0xFF424852),
    onSurfaceVariant = Color(0xFFC2C7CF),
    outline = Color(0xFF8C919A),
    outlineVariant = Color(0xFF39404A),
    surfaceContainerLowest = Color(0xFF0A0E13),
    surfaceContainerLow = Color(0xFF171D25),     // cards
    surfaceContainer = Color(0xFF1B222B),
    surfaceContainerHigh = Color(0xFF232B35),
    surfaceContainerHighest = Color(0xFF2D3540),
)

@Composable
fun BriefingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // follows the phone's dark mode setting
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
