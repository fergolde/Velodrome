package com.fergolde.velodrome.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ─── FONTS ───────────────────────────────────────────────────────────────────
// Add syne_bold.ttf and Syne-ExtraBold.ttf to res/font/
// Add DMSans-Regular.ttf, DMSans-Medium.ttf, DMSans-SemiBold.ttf to res/font/

val SyneFontFamily = FontFamily(
    Font(com.fergolde.velodrome.R.font.syne_bold, FontWeight.Bold),
    Font(com.fergolde.velodrome.R.font.syne_extrabold, FontWeight.ExtraBold),
)

val DmSansFontFamily = FontFamily(
    Font(com.fergolde.velodrome.R.font.dmsans_regular, FontWeight.Normal),
    Font(com.fergolde.velodrome.R.font.dmsans_medium, FontWeight.Medium),
    Font(com.fergolde.velodrome.R.font.dmsans_semibold, FontWeight.SemiBold),
)

// ─── PALETTE ─────────────────────────────────────────────────────────────────

object VeloPalette {
    // Backgrounds
    val Bg         = Color(0xFF0A0B0F)
    val Bg2        = Color(0xFF111318)
    val Bg3        = Color(0xFF161920)
    val Bg4        = Color(0xFF1E2129)
    val Border     = Color(0xFF2A2E3A)

    // Text
    val TextPrimary   = Color(0xFFF0F0F0)
    val TextSecondary = Color(0xFF8A8F9E)
    val TextTertiary  = Color(0xFF4A4F5E)

    // Accent (lime-green — works perfectly on dark bg, WCAG AA)
    // Can be swapped at runtime via VeloTheme.accentColor
    val AccentDefault = Color(0xFFC8FF00)

    // Semantic
    val Destructive = Color(0xFFEF5350)
    val DestructiveBg = Color(0x1AEF5350)

    // Feature card backgrounds (dark tints, not saturated fills)
    val FeatureOffline    = Color(0xFF0F1F18)
    val FeatureTop100     = Color(0xFF1F1508)
    val FeatureDiscovery  = Color(0xFF1A0F1F)

    // Scrobble red
    val LastFmRed = Color(0xFFE2231A)
}

// ─── EXTENDED THEME (accent override support) ────────────────────────────────

data class VeloColors(
    val accent: Color = VeloPalette.AccentDefault,
    val onAccent: Color = Color.Black,
)

val LocalVeloColors = staticCompositionLocalOf { VeloColors() }

// ─── MATERIAL COLOR SCHEME ───────────────────────────────────────────────────

private fun veloColorScheme(accent: Color) = darkColorScheme(
    primary              = accent,
    onPrimary            = Color.Black,
    primaryContainer     = accent.copy(alpha = 0.15f),
    onPrimaryContainer   = accent,
    secondary            = VeloPalette.TextSecondary,
    onSecondary          = VeloPalette.TextPrimary,
    background           = VeloPalette.Bg,
    onBackground         = VeloPalette.TextPrimary,
    surface              = VeloPalette.Bg3,
    onSurface            = VeloPalette.TextPrimary,
    surfaceVariant       = VeloPalette.Bg4,
    onSurfaceVariant     = VeloPalette.TextSecondary,
    outline              = VeloPalette.Border,
    error                = VeloPalette.Destructive,
)

// ─── TYPOGRAPHY ──────────────────────────────────────────────────────────────

val VeloTypography = androidx.compose.material3.Typography(
    // Display — Syne ExtraBold, used in section headers
    displayLarge = TextStyle(
        fontFamily = SyneFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
    ),
    // Section title (22sp)
    headlineMedium = TextStyle(
        fontFamily = SyneFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
    ),
    // Screen title (28sp)
    headlineLarge = TextStyle(
        fontFamily = SyneFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = VeloPalette.TextSecondary,
    ),
    labelLarge = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 2.sp,
        color = VeloPalette.TextTertiary,
    ),
)

// ─── COMPOSABLE THEME ENTRY POINT ────────────────────────────────────────────

@Composable
fun VelodromeTheme(
    accentColor: Color = VeloPalette.AccentDefault,
    content: @Composable () -> Unit,
) {
    val veloColors = VeloColors(
        accent = accentColor,
        onAccent = if (accentColor.luminance() > 0.4f) Color.Black else Color.White,
    )
    CompositionLocalProvider(LocalVeloColors provides veloColors) {
        MaterialTheme(
            colorScheme = veloColorScheme(accentColor),
            typography = VeloTypography,
            content = content,
        )
    }
}

// Helper so screens can grab the extended tokens
val MaterialTheme.velo: VeloColors
    @Composable get() = LocalVeloColors.current

// Luminance extension (avoids importing graphics package at call sites)
private fun Color.luminance(): Float {
    val r = red.linearize()
    val g = green.linearize()
    val b = blue.linearize()
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
private fun Float.linearize() = if (this <= 0.04045f) this / 12.92f else Math.pow(((this + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()