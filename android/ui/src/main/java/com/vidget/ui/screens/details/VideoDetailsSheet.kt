package com.vidget.ui.screens.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.vidget.core.storage.StorageSpaceChecker
import com.vidget.domain.model.DestinationType
import com.vidget.domain.model.MediaInfo
import com.vidget.domain.model.QualityKind
import com.vidget.domain.model.QualityOption
import com.vidget.domain.model.SubtitleTrack
import com.vidget.ui.R
import com.vidget.ui.components.DestinationPickerSheet
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailsSheet(
    uiState: VideoDetailsUiState,
    onSelectOption: (QualityOption) -> Unit,
    onTabSelected: (Int) -> Unit,
    onToggleSubtitle: (String) -> Unit,
    onSetEmbedSubtitles: (Boolean) -> Unit,
    onChangeDestination: (DestinationType) -> Unit,
    onStartDownload: (QualityOption, Set<String>, Boolean, DestinationType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        when (uiState) {
            is VideoDetailsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "جارٍ تحليل الرابط وجلب الجودات…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is VideoDetailsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onDismiss) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                }
            }

            is VideoDetailsUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد تفاصيل متاحة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is VideoDetailsUiState.Success -> {
                VideoDetailsSuccessContent(
                    state = uiState,
                    onSelectOption = onSelectOption,
                    onTabSelected = onTabSelected,
                    onToggleSubtitle = onToggleSubtitle,
                    onSetEmbedSubtitles = onSetEmbedSubtitles,
                    onChangeDestination = onChangeDestination,
                    onStartDownload = {
                        val option = uiState.selectedOption ?: return@VideoDetailsSuccessContent
                        onStartDownload(
                            option,
                            uiState.selectedSubtitles,
                            uiState.embedSubtitles,
                            uiState.destination
                        )
                    },
                    onCancel = onDismiss
                )
            }
        }
    }
}

@Composable
fun VideoDetailsSuccessContent(
    state: VideoDetailsUiState.Success,
    onSelectOption: (QualityOption) -> Unit,
    onTabSelected: (Int) -> Unit,
    onToggleSubtitle: (String) -> Unit,
    onSetEmbedSubtitles: (Boolean) -> Unit,
    onChangeDestination: (DestinationType) -> Unit,
    onStartDownload: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDestPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        // 1. Top Section: 16:9 Thumbnail & Metadata
        HeaderSection(mediaInfo = state.mediaInfo)

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Playlist Banner (if playlist)
        if (state.isPlaylist) {
            PlaylistBanner(
                count = state.mediaInfo.playlistItems.size,
                selectedCount = state.selectedPlaylistItemIds.size
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 3. TabRow (Video, Audio, Subtitles)
        val tabs = listOf(
            stringResource(R.string.tab_video),
            stringResource(R.string.tab_audio),
            stringResource(R.string.tab_subtitles)
        )
        TabRow(
            selectedTabIndex = state.selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = state.selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (state.selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Tab Content (Max height 320dp to leave room for bottom action bar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            when (state.selectedTab) {
                0 -> {
                    // Video Options LazyColumn
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.videoOptions, key = { it.id }) { option ->
                            VideoOptionCard(
                                option = option,
                                isSelected = state.selectedOption?.id == option.id,
                                onSelect = { onSelectOption(option) }
                            )
                        }
                    }
                }
                1 -> {
                    // Audio Options LazyColumn
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.audioOptions, key = { it.id }) { option ->
                            AudioOptionCard(
                                option = option,
                                isSelected = state.selectedOption?.id == option.id,
                                onSelect = { onSelectOption(option) }
                            )
                        }
                    }
                }
                2 -> {
                    // Subtitles Content
                    SubtitlesContent(
                        subtitles = state.mediaInfo.subtitles,
                        selectedSubtitles = state.selectedSubtitles,
                        embedSubtitles = state.embedSubtitles,
                        onToggleSubtitle = onToggleSubtitle,
                        onSetEmbedSubtitles = onSetEmbedSubtitles
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // 5. Fixed Bottom Section: Destination, Total Size, Action Buttons
        BottomBarSection(
            destination = state.destination,
            formattedTotalSize = state.formattedTotalSize,
            canStart = state.selectedOption != null,
            onChangeDestinationClick = { showDestPicker = true },
            onStartDownload = onStartDownload,
            onCancel = onCancel
        )
    }

    if (showDestPicker) {
        DestinationPickerSheet(
            currentDestination = state.destination,
            onDestinationSelected = {
                onChangeDestination(it)
                showDestPicker = false
            },
            onDismiss = { showDestPicker = false }
        )
    }
}

@Composable
private fun HeaderSection(
    mediaInfo: MediaInfo,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Thumbnail with 16:9 ratio and duration badge
        Box(
            modifier = Modifier
                .width(130.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(mediaInfo.thumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = mediaInfo.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Duration badge at bottom-right
            if (mediaInfo.durationSeconds > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatDuration(mediaInfo.durationSeconds),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Title and Uploader
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = mediaInfo.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mediaInfo.uploader.isNotBlank()) {
                    Text(
                        text = mediaInfo.uploader,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (mediaInfo.extractor.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = mediaInfo.extractor.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistBanner(
    count: Int,
    selectedCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "قائمة تشغيل: تم تحديد $selectedCount من أصل $count فيديو (سيتم تطبيق الجودة على الكل)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun VideoOptionCard(
    option: QualityOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            )
        } else null,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Radio & Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Best Badge
                        if (option.isBest) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.secondary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.badge_best),
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Needs Merge Badge
                        if (option.kind == QualityKind.NEEDS_MERGE) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CallSplit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = stringResource(R.string.badge_needs_merge),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Format, Codec & Container pills
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${option.displayContainer} • ${option.displayCodec}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Size with Western Numerals
            val sizeText = when {
                option.isLive -> "بث مباشر"
                option.isUnknownSize -> "غير معروف"
                else -> "${if (option.sizeIsEstimate) "~" else ""}${StorageSpaceChecker.formatBytes(option.sizeBytes)}"
            }
            Text(
                text = sizeText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AudioOptionCard(
    option: QualityOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            )
        } else null,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (option.isBest) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.secondary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.badge_best),
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "${option.displayContainer} • ${option.displayCodec}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val sizeText = when {
                option.isLive -> "بث مباشر"
                option.isUnknownSize -> "غير معروف"
                else -> "${if (option.sizeIsEstimate) "~" else ""}${StorageSpaceChecker.formatBytes(option.sizeBytes)}"
            }
            Text(
                text = sizeText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SubtitlesContent(
    subtitles: List<SubtitleTrack>,
    selectedSubtitles: Set<String>,
    embedSubtitles: Boolean,
    onToggleSubtitle: (String) -> Unit,
    onSetEmbedSubtitles: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (subtitles.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_subtitles_found),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val manual = subtitles.filter { !it.isAutomatic }
    val auto = subtitles.filter { it.isAutomatic }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Embed vs Separate Toggle
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "طريقة حفظ الترجمة:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onSetEmbedSubtitles(true) }
                    ) {
                        RadioButton(selected = embedSubtitles, onClick = { onSetEmbedSubtitles(true) })
                        Text(text = stringResource(R.string.subtitles_embed_option), style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onSetEmbedSubtitles(false) }
                    ) {
                        RadioButton(selected = !embedSubtitles, onClick = { onSetEmbedSubtitles(false) })
                        Text(text = stringResource(R.string.subtitles_separate_option), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Manual Subtitles Section
        if (manual.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.subtitles_manual_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(manual) { sub ->
                SubtitleItemRow(
                    sub = sub,
                    isSelected = selectedSubtitles.contains(sub.language),
                    onToggle = { onToggleSubtitle(sub.language) }
                )
            }
        }

        // Auto Subtitles Section
        if (auto.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.subtitles_auto_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(auto) { sub ->
                SubtitleItemRow(
                    sub = sub,
                    isSelected = selectedSubtitles.contains(sub.language),
                    onToggle = { onToggleSubtitle(sub.language) }
                )
            }
        }
    }
}

@Composable
private fun SubtitleItemRow(
    sub: SubtitleTrack,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
            Text(
                text = "${sub.name} (${sub.language})",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            text = ".${sub.ext}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BottomBarSection(
    destination: DestinationType,
    formattedTotalSize: String,
    canStart: Boolean,
    onChangeDestinationClick: () -> Unit,
    onStartDownload: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Destination Row with Free Space and Change Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "مكان الحفظ: ${destination.displayPath}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            TextButton(onClick = onChangeDestinationClick) {
                Text(
                    text = stringResource(R.string.dest_change_btn),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Total Size Line
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.total_size_label, formattedTotalSize),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.action_cancel))
            }

            Button(
                onClick = onStartDownload,
                enabled = canStart,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1.5f)
            ) {
                Text(stringResource(R.string.action_start_download))
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hrs > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

@Preview(name = "Video Details Sheet Preview", showBackground = true)
@Composable
private fun VideoDetailsSheetPreview() {
    val sampleMedia = MediaInfo(
        id = "sample_vid",
        title = "دورة تطوير تطبيقات أندرويد الحديثة بـ Jetpack Compose",
        durationSeconds = 754,
        uploader = "أكاديمية المطورين",
        extractor = "youtube",
        formats = emptyList()
    )

    val sampleVideos = listOf(
        QualityOption(
            id = "1080p",
            label = "1080p60",
            height = 1080,
            fps = 60,
            container = "mp4",
            vcodec = "avc1.640028",
            sizeBytes = 145_000_000L,
            sizeIsEstimate = false,
            kind = QualityKind.NEEDS_MERGE,
            isBest = true,
            formatSelector = "137+140"
        ),
        QualityOption(
            id = "720p",
            label = "720p",
            height = 720,
            fps = 30,
            container = "mp4",
            vcodec = "avc1.4d401f",
            sizeBytes = 65_000_000L,
            sizeIsEstimate = false,
            kind = QualityKind.COMBINED,
            isBest = false,
            formatSelector = "22"
        )
    )

    VideoDetailsSuccessContent(
        state = VideoDetailsUiState.Success(
            mediaInfo = sampleMedia,
            videoOptions = sampleVideos,
            audioOptions = emptyList(),
            selectedOption = sampleVideos.first(),
            selectedTab = 0,
            totalSizeBytes = 145_000_000L,
            formattedTotalSize = "145.0 MB"
        ),
        onSelectOption = {},
        onTabSelected = {},
        onToggleSubtitle = {},
        onSetEmbedSubtitles = {},
        onChangeDestination = {},
        onStartDownload = {},
        onCancel = {}
    )
}
