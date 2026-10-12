package com.vidget.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// =============================================================================
// Shapes Configuration
// small: 8dp, medium: 12dp, large: 16dp, extraLarge: 28dp
// =============================================================================
val VidGetShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// BottomSheet top rounded corners (28dp for top edge)
val BottomSheetTopShape = RoundedCornerShape(
    topStart = 28.dp,
    topEnd = 28.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

// Minimum touch target size (48dp for accessibility compliance)
val MinTouchTargetSize = 48.dp
