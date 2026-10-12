package com.vidget.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilePresent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vidget.domain.model.NameConflictResolution
import com.vidget.ui.R
import com.vidget.ui.theme.MinTouchTargetSize
import com.vidget.ui.theme.VidGetTheme

/**
 * Dialog presented when target filename already exists and user preference is ASK_EVERY_TIME.
 */
@Composable
fun ConflictResolutionDialog(
    filename: String,
    onSelectStrategy: (NameConflictResolution) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = VidGetTheme.spacing

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Rounded.FilePresent,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.conflict_dialog_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.conflict_dialog_desc, filename),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(spacing.medium))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    // 1. Auto Rename (Recommended)
                    Button(
                        onClick = { onSelectStrategy(NameConflictResolution.AUTO_RENAME) },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MinTouchTargetSize)
                    ) {
                        Text(
                            text = stringResource(R.string.conflict_opt_auto_rename),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // 2. Overwrite
                    OutlinedButton(
                        onClick = { onSelectStrategy(NameConflictResolution.OVERWRITE) },
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MinTouchTargetSize)
                    ) {
                        Text(
                            text = stringResource(R.string.conflict_opt_overwrite),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // 3. Skip
                    TextButton(
                        onClick = { onSelectStrategy(NameConflictResolution.SKIP) },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MinTouchTargetSize)
                    ) {
                        Text(
                            text = stringResource(R.string.conflict_opt_skip),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
        modifier = modifier
    )
}
