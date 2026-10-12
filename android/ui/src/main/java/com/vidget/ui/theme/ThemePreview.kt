package com.vidget.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vidget.domain.model.ThemeMode
import com.vidget.ui.R

/**
 * Preview composable showing the core design components (Buttons, Card, Badges, BottomSheet header)
 * styled strictly with VidGet tokens (Colors, Shapes, Spacing, and MinTouchTargetSize).
 */
@Composable
fun BaseComponentsShowcase(
    modifier: Modifier = Modifier
) {
    val spacing = VidGetTheme.spacing
    val extendedColors = VidGetTheme.extendedColors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        // 1. Card Component (medium shape 12dp, surface/surfaceVariant colors)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(spacing.large),
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Text(
                    text = stringResource(R.string.preview_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.preview_card_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        // 2. Button Components (Minimum touch target 48dp, primary and secondary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            Button(
                onClick = {},
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = MinTouchTargetSize),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.preview_primary_action),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            OutlinedButton(
                onClick = {},
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = MinTouchTargetSize),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(
                    text = stringResource(R.string.preview_secondary_action),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // 3. Status Badges (Success, Warning, Error semantic tokens)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            StatusBadge(
                text = stringResource(R.string.preview_badge_success),
                backgroundColor = extendedColors.success,
                textColor = Color.White,
                modifier = Modifier.weight(1f)
            )
            StatusBadge(
                text = stringResource(R.string.preview_badge_warning),
                backgroundColor = extendedColors.warning,
                textColor = Color.White,
                modifier = Modifier.weight(1f)
            )
            StatusBadge(
                text = stringResource(R.string.preview_badge_error),
                backgroundColor = MaterialTheme.colorScheme.error,
                textColor = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        // 4. BottomSheet Top Header Mockup (extraLarge top corners 28dp)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = BottomSheetTopShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.large),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.preview_bottom_sheet_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Detailed Typography Showcase testing all font sizes, Arabic and English long texts
 * to rigorously verify that diacritics and ascenders/descenders are never clipped.
 */
@Composable
fun TypographyShowcase(
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = VidGetTheme.spacing
    val typography = MaterialTheme.typography
    val metricStyle = VidGetTheme.metricTextStyle

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        // Section Title
        Text(
            text = stringResource(R.string.typography_preview_title),
            style = typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        // Long Text Card (Verifying anti-clipping of diacritics and flourishing characters)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(spacing.large),
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Text(
                    text = if (isArabic) {
                        stringResource(R.string.typography_arabic_sample_title)
                    } else {
                        stringResource(R.string.typography_english_sample_title)
                    },
                    style = typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Long text demonstration (bodyLarge 16sp with +4sp on Arabic)
                Text(
                    text = if (isArabic) {
                        stringResource(R.string.typography_arabic_long_text)
                    } else {
                        stringResource(R.string.typography_english_long_text)
                    },
                    style = typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Metrics Demonstration Card (Speed, ETA, Western Digits 0-9 & Tabular Figures)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
            ) {
                Text(
                    text = stringResource(R.string.typography_metrics_speed_label),
                    style = typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = stringResource(R.string.typography_metrics_speed_value),
                    style = metricStyle,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Hierarchy Ladder Demo (displayLarge, headlineMedium, titleLarge)
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            Text(
                text = "displayLarge (40sp)",
                style = typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "headlineMedium (28sp)",
                style = typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "titleLarge (22sp)",
                style = typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "bodyMedium (14sp)",
                style = typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "labelSmall (11sp)",
                style = typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val spacing = VidGetTheme.spacing
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .padding(horizontal = spacing.small, vertical = spacing.extraSmall),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}

// =============================================================================
// Previews for Arabic (Cairo) and English (Inter)
// =============================================================================

@Preview(name = "Arabic Typography Preview (Cairo)", showBackground = true, locale = "ar")
@Composable
private fun ArabicTypographyPreview() {
    VidGetTheme(themeMode = ThemeMode.LIGHT, isArabic = true) {
        TypographyShowcase(isArabic = true)
    }
}

@Preview(name = "English Typography Preview (Inter)", showBackground = true, locale = "en")
@Composable
private fun EnglishTypographyPreview() {
    VidGetTheme(themeMode = ThemeMode.LIGHT, isArabic = false) {
        TypographyShowcase(isArabic = false)
    }
}
