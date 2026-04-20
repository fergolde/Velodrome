package com.example.velodrome.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.example.velodrome.domain.repository.SettingsRepository

/**
 * Velodrome theme composable.
 * Always uses dark theme with accent color from SettingsRepository.
 * 
 * This composable will automatically update when the user changes the accent color.
 * 
 * @param settingsRepository Repository to get accent color from (injected by Hilt).
 *                              Can be null for previews.
 * @param content Composable content
 */
@Composable
fun VelodromeTheme(
    settingsRepository: SettingsRepository? = null,
    content: @Composable () -> Unit
) {
    // Always dark theme - no light mode in Velodrome
    
    // Get accent color from settings or use default
    val accentHex = settingsRepository?.accentColor?.collectAsState(initial = "#B6A0FF")?.value ?: "#B6A0FF"
    val accentComposeColor = accentHex.toComposeColor()
    
    // Build dynamic dark color scheme with custom accent
    val DarkColorScheme = darkColorScheme(
        primary = accentComposeColor,
        secondary = accentComposeColor.copy(alpha = 0.7f),
        tertiary = Pink80,
        background = VelodromeColors.BackgroundDark,
        surface = VelodromeColors.SurfaceDark,
        onPrimary = VelodromeColors.BackgroundDark,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = VelodromeColors.TextPrimary,
        onSurface = VelodromeColors.TextPrimary,
        surfaceVariant = VelodromeColors.SurfaceContainer,
        onSurfaceVariant = VelodromeColors.TextSecondary
    )

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}