package com.vidget.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.util.Locale

// =============================================================================
// LineHeightStyle and PlatformTextStyle to prevent Arabic diacritic clipping
// =============================================================================
private val BasePlatformTextStyle = PlatformTextStyle(
    includeFontPadding = false
)

private val BaseLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

/**
 * Builds a standardized TextStyle with anti-clipping parameters and optional tabular figures.
 */
private fun createTextStyle(
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    tabularFigures: Boolean = false
): TextStyle {
    return TextStyle(
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        fontSize = fontSize,
        lineHeight = lineHeight,
        platformStyle = BasePlatformTextStyle,
        lineHeightStyle = BaseLineHeightStyle,
        fontFeatureSettings = if (tabularFigures) "tnum" else null
    )
}

/**
 * Builds Material 3 Typography customized for the current language.
 * Arabic line heights are increased by +4sp to prevent clipping of tashkeel and descenders.
 */
fun appTypography(isArabic: Boolean): Typography {
    val fontFamily = if (isArabic) CairoFontFamily else InterFontFamily
    // Arabic adds +4sp to line height to accommodate tashkeel and flourishes
    val arabicLineHeightOffset = if (isArabic) 4.sp else 0.sp

    return Typography(
        // displayLarge: 40sp / 48sp (English) or 52sp (Arabic), Bold (700)
        displayLarge = createTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            lineHeight = 48.sp + arabicLineHeightOffset
        ),
        // headlineMedium: 28sp / 36sp (English) or 40sp (Arabic), Bold (700)
        headlineMedium = createTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp + arabicLineHeightOffset
        ),
        // titleLarge: 22sp / 30sp (English) or 34sp (Arabic), Bold (700)
        titleLarge = createTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 30.sp + arabicLineHeightOffset
        ),
        // titleMedium: 18sp / 26sp (English) or 30sp (Arabic), Medium (500)
        titleMedium = createTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 26.sp + arabicLineHeightOffset
        ),
        // bodyLarge: 16sp / 26sp (English) or 30sp (Arabic), Regular (400)
        bodyLarge = createTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 26.sp + arabicLineHeightOffset
        ),
        // bodyMedium: 14sp / 22sp (English) or 26sp (Arabic), Regular (400)
        bodyMedium = createTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 22.sp + arabicLineHeightOffset
        ),
        // labelLarge: 14sp / 20sp (English) or 24sp (Arabic), Medium (500)
        labelLarge = createTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp + arabicLineHeightOffset
        ),
        // labelSmall: 11sp / 16sp (English) or 20sp (Arabic), Medium (500)
        labelSmall = createTextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp + arabicLineHeightOffset
        )
    )
}

/**
 * Overload of appTypography accepting a java.util.Locale instance.
 */
fun appTypography(locale: Locale): Typography {
    val isArabic = locale.language.equals("ar", ignoreCase = true)
    return appTypography(isArabic)
}

/**
 * Dedicated TextStyle for dynamic compact text (Download Speed, ETA, Percentage).
 * Medium 12sp / 16sp (English) or 20sp (Arabic), with tabular figures ("tnum")
 * to prevent layout jitter when numeric values update rapidly.
 */
fun createMetricTextStyle(isArabic: Boolean): TextStyle {
    val fontFamily = if (isArabic) CairoFontFamily else InterFontFamily
    val arabicLineHeightOffset = if (isArabic) 4.sp else 0.sp

    return createTextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp + arabicLineHeightOffset,
        tabularFigures = true
    )
}

// CompositionLocal for download metric TextStyle (Speed, ETA, Percentage)
val LocalMetricTextStyle = staticCompositionLocalOf {
    createMetricTextStyle(isArabic = false)
}

/**
 * Formats a metric or numeric string using Western digits (0-9) strictly,
 * ensuring consistency across both Arabic and English locales as specified.
 */
fun formatWesternDigits(value: String): String {
    val easternToWesternMap = mapOf(
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
    )
    return value.map { easternToWesternMap[it] ?: it }.joinToString("")
}
