package com.vidget.ui.screens.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vidget.domain.model.DownloadStatus
import com.vidget.domain.model.DownloadTask
import com.vidget.domain.model.FailureReason
import com.vidget.ui.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DownloadItemCard(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onPlay: () -> Unit,
    onOpenFolder: () -> Unit,
    onShare: () -> Unit,
    onShowErrorDetails: () -> Unit,
    onDelete: (deleteFromDisk: Boolean) -> Unit,
    onCopyLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Upper Row: Thumbnail + Title & Badges + Overflow Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Thumbnail 96 x 54 dp (16:9)
                Box(
                    modifier = Modifier
                        .size(width = 96.dp, height = 54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (!task.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(task.thumbnailUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = task.title,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Format Badge Overlay in thumbnail corner
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(bottomStart = 8.dp, topEnd = 4.dp),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Text(
                            text = "${task.qualityLabel} • ${task.container.uppercase()}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title + Status line
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.Top)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Phase message / Status text
                    Text(
                        text = task.phaseMessage.ifBlank {
                            when (task.status) {
                                DownloadStatus.QUEUED -> "في الطابور"
                                DownloadStatus.DOWNLOADING -> "يحمّل…"
                                DownloadStatus.MERGING -> "جارٍ الدمج…"
                                DownloadStatus.SAVING -> "جارٍ الحفظ…"
                                DownloadStatus.PAUSED -> "متوقف مؤقتًا"
                                DownloadStatus.COMPLETED -> "اكتمل التحميل"
                                DownloadStatus.FAILED -> "تعذر التحميل"
                                DownloadStatus.CANCELLED -> "تم الإلغاء"
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (task.status) {
                            DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
                            DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Overflow 3-dot Menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.rounded.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_copy_link)) },
                            leadingIcon = { Icon(Icons.rounded.Share, null) },
                            onClick = {
                                menuExpanded = false
                                onCopyLink()
                            }
                        )

                        if (task.status == DownloadStatus.COMPLETED) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_share)) },
                                leadingIcon = { Icon(Icons.rounded.Share, null) },
                                onClick = {
                                    menuExpanded = false
                                    onShare()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete_with_file)) },
                                leadingIcon = { Icon(Icons.rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete(true)
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_remove_from_list)) },
                                leadingIcon = { Icon(Icons.rounded.Delete, null) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete(false)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lower Section: depends on state (Active, Completed, Failed)
            when (task.status) {
                DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED, DownloadStatus.MERGING, DownloadStatus.SAVING, DownloadStatus.PAUSED -> {
                    ActiveProgressSection(
                        task = task,
                        onPause = onPause,
                        onResume = onResume,
                        onCancel = onCancel
                    )
                }
                DownloadStatus.COMPLETED -> {
                    CompletedSection(
                        task = task,
                        onPlay = onPlay,
                        onOpenFolder = onOpenFolder
                    )
                }
                DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                    FailedSection(
                        task = task,
                        onRetry = onRetry,
                        onShowErrorDetails = onShowErrorDetails
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveProgressSection(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // LinearProgressIndicator
        if (task.status == DownloadStatus.MERGING || (task.isLiveStream && task.totalBytes <= 0)) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            val progress = (task.progressPercent / 100f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress Metrics: Percent • Downloaded/Total • Speed • ETA
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val percentText = String.format(Locale.US, "%.1f%%", task.progressPercent)
            val downloadedMb = task.downloadedBytes / (1024.0 * 1024.0)
            val totalMb = task.totalBytes / (1024.0 * 1024.0)
            val bytesText = if (task.totalBytes > 0) {
                String.format(Locale.US, "%.1f MB / %.1f MB", downloadedMb, totalMb)
            } else {
                String.format(Locale.US, "%.1f MB", downloadedMb)
            }

            val speedMb = task.speedBytesPerSec / (1024.0 * 1024.0)
            val speedText = if (task.speedBytesPerSec > 0) String.format(Locale.US, "%.1f MB/s", speedMb) else "--"

            val etaText = if (task.etaSeconds > 0) {
                val mins = task.etaSeconds / 60
                val secs = task.etaSeconds % 60
                String.format(Locale.US, "%02d:%02d", mins, secs)
            } else "--"

            Text(
                text = "$percentText • $bytesText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            Text(
                text = "$speedText • ETA $etaText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons: Pause/Resume + Cancel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (task.status == DownloadStatus.PAUSED) {
                FilledTonalButton(
                    onClick = onResume,
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_resume), fontSize = 12.sp)
                }
            } else {
                OutlinedButton(
                    onClick = onPause,
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.rounded.Pause, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_pause), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.rounded.Close, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.action_cancel_download), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CompletedSection(
    task: DownloadTask,
    onPlay: () -> Unit,
    onOpenFolder: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val sizeMb = task.totalBytes / (1024.0 * 1024.0)
        val formattedSize = String.format(Locale.US, "%.1f MB", sizeMb)
        val formattedDate = remember(task.completedAt) {
            val date = Date(task.completedAt ?: System.currentTimeMillis())
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
        }

        Column {
            Text(
                text = "$formattedSize • $formattedDate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onOpenFolder,
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.rounded.FolderOpen, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.action_open_folder), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalButton(
                onClick = onPlay,
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.rounded.PlayArrow, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.action_play), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FailedSection(
    task: DownloadTask,
    onRetry: () -> Unit,
    onShowErrorDetails: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val reasonLabel = when (task.failureReason) {
            FailureReason.NETWORK_ERROR -> "خطأ شبكة"
            FailureReason.DISK_FULL -> "المساحة ممتلئة"
            FailureReason.PERMISSION_DENIED -> "تم رفض الإذن"
            FailureReason.MERGE_FAILED -> "فشل الدمج"
            FailureReason.LINK_EXPIRED -> "الرابط منتهي"
            FailureReason.UNSUPPORTED_OR_PRIVATE -> "محتوى خاص"
            FailureReason.UNKNOWN, null -> "تعذر التحميل"
        }

        Text(
            text = reasonLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onShowErrorDetails,
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.rounded.ErrorOutline, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.action_details), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalButton(
                onClick = onRetry,
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.rounded.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.action_retry), fontSize = 12.sp)
            }
        }
    }
}
