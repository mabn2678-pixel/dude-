package com.vidget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidget.domain.model.DownloadTask
import com.vidget.domain.model.FailureReason
import com.vidget.ui.R

/**
 * Dialog displaying classified download error with contextual explanation,
 * dedicated action button (retry, change folder, fix permission, or retry with MKV),
 * and expandable technical log.
 */
@Composable
fun DownloadErrorDialog(
    task: DownloadTask,
    onDismiss: () -> Unit,
    onPrimaryAction: (FailureReason) -> Unit,
    onSecondaryAction: () -> Unit = onDismiss
) {
    val reason = task.failureReason ?: FailureReason.UNKNOWN
    var showTechnicalDetails by remember { mutableStateOf(false) }

    val icon = when (reason) {
        FailureReason.NETWORK_ERROR -> Icons.rounded.WifiOff
        FailureReason.DISK_FULL -> Icons.rounded.SdStorage
        FailureReason.PERMISSION_DENIED -> Icons.rounded.FolderOff
        FailureReason.MERGE_FAILED -> Icons.rounded.MovieFilter
        FailureReason.LINK_EXPIRED -> Icons.rounded.TimerOff
        FailureReason.UNSUPPORTED_OR_PRIVATE -> Icons.rounded.Lock
        FailureReason.UNKNOWN -> Icons.rounded.WifiOff
    }

    val title = when (reason) {
        FailureReason.NETWORK_ERROR -> stringResource(R.string.error_network_title)
        FailureReason.DISK_FULL -> stringResource(R.string.error_disk_full_title)
        FailureReason.PERMISSION_DENIED -> stringResource(R.string.error_perm_denied_title)
        FailureReason.MERGE_FAILED -> stringResource(R.string.error_merge_failed_title)
        FailureReason.LINK_EXPIRED -> stringResource(R.string.error_expired_title)
        FailureReason.UNSUPPORTED_OR_PRIVATE -> stringResource(R.string.error_unsupported_title)
        FailureReason.UNKNOWN -> stringResource(R.string.error_dialog_title)
    }

    val message = when (reason) {
        FailureReason.NETWORK_ERROR -> stringResource(R.string.error_network_desc)
        FailureReason.DISK_FULL -> stringResource(R.string.error_disk_full_desc)
        FailureReason.PERMISSION_DENIED -> stringResource(R.string.error_perm_denied_desc)
        FailureReason.MERGE_FAILED -> stringResource(R.string.error_merge_failed_desc)
        FailureReason.LINK_EXPIRED -> stringResource(R.string.error_expired_desc)
        FailureReason.UNSUPPORTED_OR_PRIVATE -> stringResource(R.string.error_unsupported_desc)
        FailureReason.UNKNOWN -> task.rawErrorMessage ?: stringResource(R.string.error_dialog_title)
    }

    val actionButtonText = when (reason) {
        FailureReason.NETWORK_ERROR -> stringResource(R.string.error_network_action)
        FailureReason.DISK_FULL -> stringResource(R.string.error_disk_full_action)
        FailureReason.PERMISSION_DENIED -> stringResource(R.string.error_perm_denied_action)
        FailureReason.MERGE_FAILED -> stringResource(R.string.error_merge_failed_action)
        FailureReason.LINK_EXPIRED -> stringResource(R.string.error_expired_action)
        FailureReason.UNSUPPORTED_OR_PRIVATE -> stringResource(R.string.insufficient_space_action_ok)
        FailureReason.UNKNOWN -> stringResource(R.string.action_retry)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!task.rawErrorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = { showTechnicalDetails = !showTechnicalDetails },
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = if (showTechnicalDetails) "إخفاء السجل الفني" else stringResource(R.string.error_dialog_technical_details),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    if (showTechnicalDetails) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = task.rawErrorMessage,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPrimaryAction(reason) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(text = actionButtonText)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onSecondaryAction) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    )
}
