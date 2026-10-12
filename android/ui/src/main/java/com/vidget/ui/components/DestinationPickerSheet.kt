package com.vidget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vidget.domain.model.DestinationType
import com.vidget.ui.R
import com.vidget.ui.theme.MinTouchTargetSize
import com.vidget.ui.theme.VidGetTheme

data class DestinationOptionItem(
    val type: DestinationType,
    val titleResId: Int,
    val readablePath: String,
    val icon: ImageVector,
    val freeSpaceFormatted: String,
    val freeSpaceRatio: Float,
    val isCustomOrSdCard: Boolean = false,
    val hintResId: Int? = null
)

/**
 * BottomSheet displaying 4 destination option cards with readable paths,
 * free space StatFs progress bars, and Change buttons for SAF trees.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationPickerSheet(
    selectedType: DestinationType,
    options: List<DestinationOptionItem>,
    onSelectDestination: (DestinationType) -> Unit,
    onChangeCustomFolderClick: () -> Unit,
    onChangeSdCardClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = VidGetTheme.spacing

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium)
                .padding(bottom = spacing.large)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.dest_picker_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.dest_picker_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            // 4 Option Cards
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                items(options.size) { index ->
                    val option = options[index]
                    val isSelected = when {
                        option.type is DestinationType.DownloadsDefault && selectedType is DestinationType.DownloadsDefault -> true
                        option.type is DestinationType.Movies && selectedType is DestinationType.Movies -> true
                        option.type is DestinationType.CustomFolder && selectedType is DestinationType.CustomFolder -> true
                        option.type is DestinationType.SdCard && selectedType is DestinationType.SdCard -> true
                        else -> false
                    }

                    DestinationCard(
                        option = option,
                        isSelected = isSelected,
                        onCardClick = { onSelectDestination(option.type) },
                        onChangeClick = {
                            if (option.type is DestinationType.CustomFolder) {
                                onChangeCustomFolderClick()
                            } else if (option.type is DestinationType.SdCard) {
                                onChangeSdCardClick()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationCard(
    option: DestinationOptionItem,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onChangeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = VidGetTheme.spacing
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .fillMaxWidth()
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = borderColor, shape = MaterialTheme.shapes.medium)
            .clickable(onClick = onCardClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(spacing.medium))

                // Title and path
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(option.titleResId),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = option.readablePath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 1
                    )
                }

                // Change button or Radio indicator
                if (option.isCustomOrSdCard) {
                    TextButton(
                        onClick = onChangeClick,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.height(MinTouchTargetSize)
                    ) {
                        Text(
                            text = stringResource(R.string.dest_change_btn),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onCardClick
                )
            }

            Spacer(modifier = Modifier.height(spacing.small))

            // StatFs Free Space Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option.freeSpaceFormatted,
                    style = VidGetTheme.metricTextStyle,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(spacing.extraSmall))

            LinearProgressIndicator(
                progress = { option.freeSpaceRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (option.freeSpaceRatio < 0.10f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Optional Hint for SD card
            if (option.hintResId != null) {
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                Text(
                    text = stringResource(option.hintResId),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                )
            }
        }
    }
}
