package com.example.velodrome.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

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
    val PrimaryColor = Color(0xFF7C4DFF)
    val AccentPurple = Color(0xFFB6A0FF)
}

/**
 * Extension to convert hex string to Compose Color.
 */
fun String.toComposeColor(): Color {
    return try {
        val cleanHex = this.removePrefix("#")
        Color(android.graphics.Color.parseColor("#$cleanHex"))
    } catch (e: Exception) {
        VelodromeColors.AccentPurple
    }
}

/**
 * Extension to convert Compose Color to hex string.
 */
fun Color.toHexString(): String {
    val argb = this.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}