package com.example.velodrome.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

/**
 * Centralized theme colors for Velodrome.
 * 
 * This object provides the base colors that are the same throughout the app.
 * For the accent color, use [VelodromeTheme] which observes SettingsRepository.
 */
object VelodromeColors {

    // Hardcoded dark theme colors (these don't change)
    val BackgroundDark = Color(0xFF0C0E17)
    val SurfaceDark = Color(0xFF171924)
    val SurfaceContainer = Color(0xFF222532)
    val TextPrimary = Color(0xFFF0F0FD)
    val TextSecondary = Color(0xFFAAAAB7)
    
    // Primary variants
    val AccentPurple = Color(0xFFB6A0FF)
}

/**
 * Extension to convert hex string to Compose Color.
 */
fun String.toComposeColor(): Color {
    return try {
        val cleanHex = this.removePrefix("#")
        Color("#$cleanHex".toColorInt())
    } catch (e: Exception) {
        VelodromeColors.AccentPurple
    }
}