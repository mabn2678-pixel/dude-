package com.vidget.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.os.ConfigurationCompat
import com.vidget.ui.R
import java.util.Locale

// =============================================================================
// Cairo Font Family for Arabic (Local TTF assets only: 400, 500, 700)
// =============================================================================
val CairoFontFamily = FontFamily(
    Font(resId = R.font.cairo_regular, weight = FontWeight.Normal),
    Font(resId = R.font.cairo_medium, weight = FontWeight.Medium),
    Font(resId = R.font.cairo_bold, weight = FontWeight.Bold)
)

// =============================================================================
// Inter Font Family for English (Local TTF assets only: 400, 500, 700)
// =============================================================================
val InterFontFamily = FontFamily(
    Font(resId = R.font.inter_regular, weight = FontWeight.Normal),
    Font(resId = R.font.inter_medium, weight = FontWeight.Medium),
    Font(resId = R.font.inter_bold, weight = FontWeight.Bold)
)

// =============================================================================
// CompositionLocal for dynamic locale-based FontFamily resolution
// =============================================================================
val LocalAppFontFamily = staticCompositionLocalOf { InterFontFamily }

/**
 * Resolves the primary font family based on whether the locale is Arabic.
 */
fun resolveFontFamily(isArabic: Boolean): FontFamily {
    return if (isArabic) CairoFontFamily else InterFontFamily
}

/**
 * Convenience helper to determine whether the current Android Configuration is Arabic.
 */
@Composable
@ReadOnlyComposable
fun isCurrentLocaleArabic(): Boolean {
    val configuration = LocalConfiguration.current
    val currentLocale = ConfigurationCompat.getLocales(configuration).get(0) ?: Locale.getDefault()
    return currentLocale.language.equals("ar", ignoreCase = true)
}
