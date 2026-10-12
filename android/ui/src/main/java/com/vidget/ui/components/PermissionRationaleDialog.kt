package com.vidget.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vidget.core.permission.PermissionStatus
import com.vidget.core.permission.PermissionType
import com.vidget.ui.R
import com.vidget.ui.theme.MinTouchTargetSize
import com.vidget.ui.theme.VidGetTheme

data class PermissionDialogStrings(
    val titleResId: Int,
    val descriptionResId: Int,
    val icon: ImageVector
)

/**
 * Resolves the appropriate icon and localized strings based on permission type and denial state.
 */
fun resolveDialogContent(
    type: PermissionType,
    status: PermissionStatus
): PermissionDialogStrings {
    return when (type) {
        PermissionType.NOTIFICATIONS -> {
            when (status) {
                PermissionStatus.DeniedOnce -> PermissionDialogStrings(
                    titleResId = R.string.perm_notif_rationale_title,
                    descriptionResId = R.string.perm_notif_rationale_desc,
                    icon = Icons.Rounded.NotificationsActive
                )
                PermissionStatus.PermanentlyDenied -> PermissionDialogStrings(
                    titleResId = R.string.perm_notif_settings_title,
                    descriptionResId = R.string.perm_notif_settings_desc,
                    icon = Icons.Rounded.Settings
                )
                else -> PermissionDialogStrings(
                    titleResId = R.string.perm_notif_title,
                    descriptionResId = R.string.perm_notif_desc,
                    icon = Icons.Rounded.NotificationsActive
                )
            }
        }
        PermissionType.LEGACY_STORAGE_WRITE -> {
            when (status) {
                PermissionStatus.DeniedOnce -> PermissionDialogStrings(
                    titleResId = R.string.perm_storage_write_rationale_title,
                    descriptionResId = R.string.perm_storage_write_rationale_desc,
                    icon = Icons.Rounded.SdCard
                )
                PermissionStatus.PermanentlyDenied -> PermissionDialogStrings(
                    titleResId = R.string.perm_storage_write_settings_title,
                    descriptionResId = R.string.perm_storage_write_settings_desc,
                    icon = Icons.Rounded.Settings
                )
                else -> PermissionDialogStrings(
                    titleResId = R.string.perm_storage_write_title,
                    descriptionResId = R.string.perm_storage_write_desc,
                    icon = Icons.Rounded.SdCard
                )
            }
        }
        PermissionType.EXTERNAL_MEDIA_READ -> {
            when (status) {
                PermissionStatus.DeniedOnce -> PermissionDialogStrings(
                    titleResId = R.string.perm_media_read_rationale_title,
                    descriptionResId = R.string.perm_media_read_rationale_desc,
                    icon = Icons.Rounded.PermMedia
                )
                PermissionStatus.PermanentlyDenied -> PermissionDialogStrings(
                    titleResId = R.string.perm_media_read_settings_title,
                    descriptionResId = R.string.perm_media_read_settings_desc,
                    icon = Icons.Rounded.Settings
                )
                else -> PermissionDialogStrings(
                    titleResId = R.string.perm_media_read_title,
                    descriptionResId = R.string.perm_media_read_desc,
                    icon = Icons.Rounded.PermMedia
                )
            }
        }
    }
}

/**
 * Contextual Permission Rationale Dialog.
 * Displays: Icon + Title + 2-line explanation + Action buttons (Continue / Not Now or Open Settings).
 */
@Composable
fun PermissionRationaleDialog(
    type: PermissionType,
    status: PermissionStatus,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = VidGetTheme.spacing
    val content = resolveDialogContent(type, status)
    val isPermanent = status is PermissionStatus.PermanentlyDenied

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = content.icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = if (isPermanent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(content.titleResId),
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
                    text = stringResource(content.descriptionResId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.height(MinTouchTargetSize)
            ) {
                Text(
                    text = if (isPermanent) {
                        stringResource(R.string.action_open_settings)
                    } else {
                        stringResource(R.string.action_continue)
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.height(MinTouchTargetSize)
            ) {
                Text(
                    text = stringResource(R.string.action_not_now),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        modifier = modifier
    )
}
