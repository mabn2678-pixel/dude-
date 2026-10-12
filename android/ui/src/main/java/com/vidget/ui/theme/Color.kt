package com.vidget.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =============================================================================
// Light Theme Palette (Fixed values as specified)
// =============================================================================
val LightPrimary = Color(0xFF2563EB)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightSecondary = Color(0xFF0D9488)
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE2E8F0)
val LightOnSurface = Color(0xFF0F172A)
val LightError = Color(0xFFDC2626)
val LightSuccess = Color(0xFF16A34A)
val LightWarning = Color(0xFFD97706)

// =============================================================================
// Dark Theme Palette (Fixed values as specified)
// =============================================================================
val DarkPrimary = Color(0xFF60A5FA)
val DarkOnPrimary = Color(0xFF0B1220)
val DarkSecondary = Color(0xFF2DD4BF)
val DarkBackground = Color(0xFF0B1220)
val DarkSurface = Color(0xFF111B2E)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkOnSurface = Color(0xFFE2E8F0)
val DarkError = Color(0xFFF87171)
val DarkSuccess = Color(0xFF4ADE80)
val DarkWarning = Color(0xFFFBBF24)

// =============================================================================
// Extended Semantic Colors (Success & Warning)
// =============================================================================
@Immutable
data class ExtendedColors(
    val success: Color,
    val warning: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        success = LightSuccess,
        warning = LightWarning
    )
}
