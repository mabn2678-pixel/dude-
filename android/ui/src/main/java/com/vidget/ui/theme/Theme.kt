package com.vidget.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowCompat
import com.vidget.domain.model.ThemeMode

// =============================================================================
// Light Color Scheme
// =============================================================================
private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    secondary = LightSecondary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurface = LightOnSurface,
    error = LightError
)

// =============================================================================
// Dark Color Scheme
// =============================================================================
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    secondary = DarkSecondary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurface = DarkOnSurface,
    error = DarkError
)

// =============================================================================
// Extended Semantic Colors (Success & Warning)
// =============================================================================
private val LightExtendedColors = ExtendedColors(
    success = LightSuccess,
    warning = LightWarning
)

private val DarkExtendedColors = ExtendedColors(
    success = DarkSuccess,
    warning = DarkWarning
)

/**
 * Main application theme wrapper for VidGet.
 * Handles Light/Dark/System modes, dynamic Material You colors (API 31+),
 * automatic locale-based font resolution (Cairo for AR, Inter for EN),
 * and automatic edge-to-edge system bars styling.
 */
@Composable
fun VidGetTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    isArabic: Boolean = isCurrentLocaleArabic(),
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val spacing = Spacing()
    val typography = appTypography(isArabic)
    val metricTextStyle = createMetricTextStyle(isArabic)
    val appFontFamily = resolveFontFamily(isArabic)

    // Edge-to-edge system bars appearance synchronization
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalSpacing provides spacing,
        LocalExtendedColors provides extendedColors,
        LocalAppFontFamily provides appFontFamily,
        LocalMetricTextStyle provides metricTextStyle
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = VidGetShapes,
            content = content
        )
    }
}

/**
 * Convenient object to access custom design tokens like Spacing, ExtendedColors, and MetricTextStyle.
 */
object VidGetTheme {
    val spacing: Spacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current

    val extendedColors: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current

    val metricTextStyle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalMetricTextStyle.current

    val fontFamily: FontFamily
        @Composable
        @ReadOnlyComposable
        get() = LocalAppFontFamily.current
}
