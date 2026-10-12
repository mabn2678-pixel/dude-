package com.vidget.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NetworkPing
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vidget.domain.model.AppLanguage
import com.vidget.domain.model.DefaultContainerFormat
import com.vidget.domain.model.DefaultDownloadQuality
import com.vidget.domain.model.DownloadScheduleWindow
import com.vidget.domain.model.NameConflictResolution
import com.vidget.domain.model.ProxyType
import com.vidget.domain.model.SpeedLimit
import com.vidget.domain.model.SubfolderOrganization
import com.vidget.domain.model.ThemeMode
import com.vidget.domain.model.YtDlpChannel
import com.vidget.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onOpenDestinationPicker: () -> Unit = {},
    onOpenBatteryOptimization: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.screen_settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // =================================================================
            // 1. General (عام)
            // =================================================================
            item(key = "section_general") {
                SettingsSectionCard(title = stringResource(R.string.settings_section_general)) {
                    // Language
                    SettingsClickableItem(
                        icon = Icons.Rounded.Tune,
                        title = stringResource(R.string.settings_language_title),
                        description = stringResource(R.string.settings_language_desc),
                        valueText = when (state.language) {
                            AppLanguage.SYSTEM -> stringResource(R.string.settings_lang_system)
                            AppLanguage.ARABIC -> stringResource(R.string.settings_lang_ar)
                            AppLanguage.ENGLISH -> stringResource(R.string.settings_lang_en)
                        },
                        onClick = { viewModel.openDialog(SettingsActiveDialog.LANGUAGE) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Theme
                    SettingsClickableItem(
                        icon = Icons.Rounded.Palette,
                        title = stringResource(R.string.settings_theme_title),
                        description = stringResource(R.string.settings_theme_desc),
                        valueText = when (state.themeMode) {
                            ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                            ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                            ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                        },
                        onClick = { viewModel.openDialog(SettingsActiveDialog.THEME) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Dynamic Color Switch
                    SettingsSwitchItem(
                        icon = Icons.Rounded.Palette,
                        title = stringResource(R.string.settings_dynamic_color_title),
                        description = stringResource(R.string.settings_dynamic_color_desc),
                        checked = state.dynamicColor,
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Clipboard Detection Switch
                    SettingsSwitchItem(
                        icon = Icons.Rounded.Sync,
                        title = stringResource(R.string.settings_clipboard_title),
                        description = stringResource(R.string.settings_clipboard_desc),
                        checked = state.clipboardDetection,
                        onCheckedChange = { viewModel.setClipboardDetection(it) }
                    )
                }
            }

            // =================================================================
            // 2. Downloads (التحميل)
            // =================================================================
            item(key = "section_downloads") {
                SettingsSectionCard(title = stringResource(R.string.settings_section_downloads)) {
                    // Save Directory
                    SettingsClickableItem(
                        icon = Icons.Rounded.Folder,
                        title = stringResource(R.string.settings_save_dir_title),
                        description = stringResource(R.string.settings_save_dir_desc),
                        valueText = state.saveDirectoryName,
                        onClick = onOpenDestinationPicker
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Naming Template
                    SettingsClickableItem(
                        icon = Icons.Rounded.Tune,
                        title = stringResource(R.string.settings_naming_title),
                        description = stringResource(R.string.settings_naming_desc),
                        valueText = state.namingTemplate,
                        onClick = { /* In-place or subfolder configuration */ }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Conflict Resolution
                    SettingsClickableItem(
                        icon = Icons.Rounded.History,
                        title = stringResource(R.string.settings_conflict_title),
                        description = stringResource(R.string.settings_conflict_desc),
                        valueText = when (state.conflictResolution) {
                            NameConflictResolution.AUTO_RENAME -> stringResource(R.string.conflict_opt_auto_rename)
                            NameConflictResolution.OVERWRITE -> stringResource(R.string.conflict_opt_overwrite)
                            NameConflictResolution.SKIP -> stringResource(R.string.conflict_opt_skip)
                            NameConflictResolution.ASK_EVERY_TIME -> stringResource(R.string.conflict_opt_ask_every_time)
                        },
                        onClick = { viewModel.openDialog(SettingsActiveDialog.CONFLICT_RESOLUTION) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Folder Organization
                    SettingsClickableItem(
                        icon = Icons.Rounded.Folder,
                        title = stringResource(R.string.settings_org_title),
                        description = stringResource(R.string.settings_org_desc),
                        valueText = when (state.subfolderOrganization) {
                            SubfolderOrganization.NONE -> stringResource(R.string.subfolder_org_none)
                            SubfolderOrganization.PLATFORM -> stringResource(R.string.subfolder_org_platform)
                            SubfolderOrganization.DATE -> stringResource(R.string.subfolder_org_date)
                        },
                        onClick = { viewModel.openDialog(SettingsActiveDialog.FOLDER_ORGANIZATION) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Default Quality
                    SettingsClickableItem(
                        icon = Icons.Rounded.CloudDownload,
                        title = stringResource(R.string.settings_default_quality_title),
                        description = stringResource(R.string.settings_default_quality_desc),
                        valueText = when (state.defaultQuality) {
                            DefaultDownloadQuality.ALWAYS_ASK -> stringResource(R.string.quality_always_ask)
                            DefaultDownloadQuality.BEST -> stringResource(R.string.quality_best)
                            DefaultDownloadQuality.RES_1080P -> stringResource(R.string.quality_1080p)
                            DefaultDownloadQuality.RES_720P -> stringResource(R.string.quality_720p)
                            DefaultDownloadQuality.RES_480P -> stringResource(R.string.quality_480p)
                            DefaultDownloadQuality.AUDIO_ONLY -> stringResource(R.string.quality_audio_only)
                        },
                        onClick = { viewModel.openDialog(SettingsActiveDialog.DEFAULT_QUALITY) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Default Format (MP4 / MKV)
                    SettingsClickableItem(
                        icon = Icons.Rounded.Tune,
                        title = stringResource(R.string.settings_default_format_title),
                        description = stringResource(R.string.settings_default_format_desc),
                        valueText = state.defaultFormat.name,
                        onClick = { viewModel.openDialog(SettingsActiveDialog.DEFAULT_FORMAT) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Connections Count (1 to 16)
                    SettingsSliderItem(
                        icon = Icons.Rounded.NetworkPing,
                        title = stringResource(R.string.settings_connections_title),
                        description = stringResource(R.string.settings_connections_desc, state.connectionsCount),
                        value = state.connectionsCount.toFloat(),
                        valueRange = 1f..16f,
                        steps = 14,
                        onValueChange = { viewModel.setConnectionsCount(it.toInt()) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Concurrent Downloads (1 to 5)
                    SettingsSliderItem(
                        icon = Icons.Rounded.CloudDownload,
                        title = stringResource(R.string.settings_concurrent_title),
                        description = stringResource(R.string.settings_concurrent_desc, state.maxConcurrentDownloads),
                        value = state.maxConcurrentDownloads.toFloat(),
                        valueRange = 1f..5f,
                        steps = 3,
                        onValueChange = { viewModel.setMaxConcurrentDownloads(it.toInt()) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Embed Metadata & Thumbnail Switch
                    SettingsSwitchItem(
                        icon = Icons.Rounded.Tune,
                        title = stringResource(R.string.settings_embed_metadata_title),
                        description = stringResource(R.string.settings_embed_metadata_desc),
                        checked = state.embedMetadata,
                        onCheckedChange = { viewModel.setEmbedMetadata(it) }
                    )
                }
            }

            // =================================================================
            // 3. Network (الشبكة)
            // =================================================================
            item(key = "section_network") {
                SettingsSectionCard(title = stringResource(R.string.settings_section_network)) {
                    // Wi-Fi Only
                    SettingsSwitchItem(
                        icon = Icons.Rounded.Wifi,
                        title = stringResource(R.string.settings_wifi_only_title),
                        description = stringResource(R.string.settings_wifi_only_desc),
                        checked = state.wifiOnly,
                        onCheckedChange = { viewModel.setWifiOnly(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Data Saver
                    SettingsSwitchItem(
                        icon = Icons.Rounded.NetworkPing,
                        title = stringResource(R.string.settings_data_saver_title),
                        description = stringResource(R.string.settings_data_saver_desc),
                        checked = state.dataSaver,
                        onCheckedChange = { viewModel.setDataSaver(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Speed Limit
                    SettingsClickableItem(
                        icon = Icons.Rounded.NetworkPing,
                        title = stringResource(R.string.settings_speed_limit_title),
                        description = stringResource(R.string.settings_speed_limit_desc),
                        valueText = when (state.speedLimit) {
                            SpeedLimit.NO_LIMIT -> stringResource(R.string.speed_limit_none)
                            SpeedLimit.SPEED_512_KBPS -> "512 KB/s"
                            SpeedLimit.SPEED_1_MBPS -> "1 MB/s"
                            SpeedLimit.SPEED_2_MBPS -> "2 MB/s"
                            SpeedLimit.SPEED_5_MBPS -> "5 MB/s"
                            SpeedLimit.SPEED_10_MBPS -> "10 MB/s"
                        },
                        onClick = { viewModel.openDialog(SettingsActiveDialog.SPEED_LIMIT) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Allowed Download Hours
                    SettingsClickableItem(
                        icon = Icons.Rounded.History,
                        title = stringResource(R.string.settings_allowed_hours_title),
                        description = if (state.scheduleWindow.enabled) {
                            state.scheduleWindow.formatDisplay()
                        } else {
                            stringResource(R.string.settings_allowed_hours_desc)
                        },
                        valueText = if (state.scheduleWindow.enabled) state.scheduleWindow.formatDisplay() else stringResource(R.string.speed_limit_none),
                        onClick = { viewModel.openDialog(SettingsActiveDialog.SCHEDULE_HOURS) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Block on Roaming
                    SettingsSwitchItem(
                        icon = Icons.Rounded.NetworkPing,
                        title = stringResource(R.string.settings_block_roaming_title),
                        description = stringResource(R.string.settings_block_roaming_desc),
                        checked = state.blockOnRoaming,
                        onCheckedChange = { viewModel.setBlockOnRoaming(it) }
                    )
                }
            }

            // =================================================================
            // 4. Storage (التخزين)
            // =================================================================
            item(key = "section_storage") {
                val cacheMb = "%.1f MB".format(state.storageInfo.cacheSizeBytes / (1024.0 * 1024.0))
                val orphansMb = "%.1f MB".format(state.storageInfo.orphanFilesSizeBytes / (1024.0 * 1024.0))
                val freeGb = "%.1f GB".format(state.storageInfo.freeSpaceBytes / (1024.0 * 1024.0 * 1024.0))
                val totalGb = "%.1f GB".format(state.storageInfo.totalSpaceBytes / (1024.0 * 1024.0 * 1024.0))

                SettingsSectionCard(title = stringResource(R.string.settings_section_storage)) {
                    // Clear Cache
                    SettingsActionItem(
                        icon = Icons.Rounded.DeleteSweep,
                        title = stringResource(R.string.settings_clear_cache_title),
                        description = stringResource(R.string.settings_clear_cache_desc, cacheMb),
                        buttonLabel = stringResource(R.string.action_delete),
                        onAction = { viewModel.clearCache() }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Delete Orphaned Temp Files
                    SettingsActionItem(
                        icon = Icons.Rounded.Delete,
                        title = stringResource(R.string.settings_delete_orphans_title),
                        description = stringResource(R.string.settings_delete_orphans_desc, state.storageInfo.orphanFilesCount, orphansMb),
                        buttonLabel = stringResource(R.string.action_delete),
                        onAction = { viewModel.deleteOrphanFiles() }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Free Storage Space Display
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.settings_free_space_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.settings_free_space_desc, freeGb, totalGb),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val progressRatio = if (state.storageInfo.totalSpaceBytes > 0) {
                            (1f - (state.storageInfo.freeSpaceBytes.toFloat() / state.storageInfo.totalSpaceBytes.toFloat())).coerceIn(0f, 1f)
                        } else 0.5f

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // =================================================================
            // 5. Advanced (متقدم)
            // =================================================================
            item(key = "section_advanced") {
                SettingsSectionCard(title = stringResource(R.string.settings_section_advanced)) {
                    // Update yt-dlp (manual)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_ytdlp_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(
                                    R.string.settings_ytdlp_desc,
                                    state.ytDlpCurrentVersion,
                                    state.ytDlpChannel.name
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (state.isUpdatingYtDlp) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.checkAndUpdateYtDlp() },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(stringResource(R.string.action_retry))
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // yt-dlp Auto Update
                    SettingsSwitchItem(
                        icon = Icons.Rounded.Refresh,
                        title = stringResource(R.string.settings_ytdlp_auto_title),
                        description = stringResource(R.string.settings_ytdlp_auto_desc),
                        checked = state.ytDlpAutoUpdate,
                        onCheckedChange = { viewModel.setYtDlpAutoUpdate(it) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // yt-dlp Channel (Stable vs Nightly)
                    SettingsClickableItem(
                        icon = Icons.Rounded.Tune,
                        title = stringResource(R.string.settings_ytdlp_channel_title),
                        description = stringResource(R.string.settings_ytdlp_channel_desc),
                        valueText = state.ytDlpChannel.name,
                        onClick = {
                            val nextChan = if (state.ytDlpChannel == YtDlpChannel.STABLE) YtDlpChannel.NIGHTLY else YtDlpChannel.STABLE
                            viewModel.setYtDlpChannel(nextChan)
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Cookies File (Netscape)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cookie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_cookies_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = state.cookiesFileName?.let {
                                    stringResource(R.string.settings_cookies_loaded, it)
                                } ?: stringResource(R.string.settings_cookies_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (state.cookiesFileName != null) {
                            IconButton(onClick = { viewModel.clearCookies() }) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.settings_cookies_clear),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.openDialog(SettingsActiveDialog.COOKIES_IMPORT) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(stringResource(R.string.settings_cookies_import))
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Network Proxy (HTTP/SOCKS)
                    SettingsClickableItem(
                        icon = Icons.Rounded.Security,
                        title = stringResource(R.string.settings_proxy_title),
                        description = if (state.proxyEnabled) "${state.proxyType}://${state.proxyHost}:${state.proxyPort}" else stringResource(R.string.settings_proxy_desc),
                        valueText = if (state.proxyEnabled) "ON" else "OFF",
                        onClick = { viewModel.openDialog(SettingsActiveDialog.PROXY_CONFIG) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Ignore Battery Optimization
                    SettingsClickableItem(
                        icon = Icons.Rounded.BatteryAlert,
                        title = stringResource(R.string.settings_battery_title),
                        description = stringResource(R.string.settings_battery_desc),
                        valueText = "",
                        onClick = {
                            viewModel.openBatteryOptimization(context)
                            onOpenBatteryOptimization()
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Export Diagnostic Log
                    SettingsClickableItem(
                        icon = Icons.Rounded.Share,
                        title = stringResource(R.string.settings_diagnostic_title),
                        description = stringResource(R.string.settings_diagnostic_desc),
                        valueText = "",
                        onClick = {
                            viewModel.exportDiagnosticLogs { logText ->
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, logText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Export Diagnostic Logs")
                                context.startActivity(shareIntent)
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Reset to Defaults (with dialog)
                    SettingsClickableItem(
                        icon = Icons.Rounded.RestartAlt,
                        title = stringResource(R.string.settings_reset_title),
                        description = stringResource(R.string.settings_reset_desc),
                        valueText = "",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { viewModel.openDialog(SettingsActiveDialog.RESET_CONFIRMATION) }
                    )
                }
            }

            // =================================================================
            // 6. About (حول)
            // =================================================================
            item(key = "section_about") {
                SettingsSectionCard(title = stringResource(R.string.settings_section_about)) {
                    // Version
                    SettingsClickableItem(
                        icon = Icons.Rounded.Info,
                        title = stringResource(R.string.settings_about_version_title),
                        description = stringResource(R.string.settings_about_version_desc),
                        valueText = "v1.0.0",
                        onClick = {
                            Toast.makeText(context, "VidGet v1.0.0 (Build 1)", Toast.LENGTH_SHORT).show()
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Privacy Policy
                    SettingsClickableItem(
                        icon = Icons.Rounded.PrivacyTip,
                        title = stringResource(R.string.settings_privacy_title),
                        description = stringResource(R.string.settings_privacy_desc),
                        valueText = "",
                        onClick = { viewModel.openDialog(SettingsActiveDialog.LEGAL_NOTICE) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Legal Notice
                    SettingsClickableItem(
                        icon = Icons.Rounded.Gavel,
                        title = stringResource(R.string.settings_legal_title),
                        description = stringResource(R.string.settings_legal_desc),
                        valueText = "",
                        onClick = { viewModel.openDialog(SettingsActiveDialog.LEGAL_NOTICE) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Open Source Licenses
                    SettingsClickableItem(
                        icon = Icons.Rounded.Storage,
                        title = stringResource(R.string.settings_licenses_title),
                        description = stringResource(R.string.settings_licenses_desc),
                        valueText = "",
                        onClick = { viewModel.openDialog(SettingsActiveDialog.LICENSES) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // =========================================================================
    // Dialog Handlers
    // =========================================================================

    when (state.activeDialog) {
        SettingsActiveDialog.LANGUAGE -> {
            SelectionDialog(
                title = stringResource(R.string.settings_language_title),
                items = listOf(
                    AppLanguage.SYSTEM to stringResource(R.string.settings_lang_system),
                    AppLanguage.ARABIC to stringResource(R.string.settings_lang_ar),
                    AppLanguage.ENGLISH to stringResource(R.string.settings_lang_en)
                ),
                selectedItem = state.language,
                onSelect = { viewModel.setLanguage(it) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        SettingsActiveDialog.THEME -> {
            SelectionDialog(
                title = stringResource(R.string.settings_theme_title),
                items = listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                    ThemeMode.DARK to stringResource(R.string.settings_theme_dark)
                ),
                selectedItem = state.themeMode,
                onSelect = { viewModel.setThemeMode(it) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        SettingsActiveDialog.CONFLICT_RESOLUTION -> {
            SelectionDialog(
                title = stringResource(R.string.settings_conflict_title),
                items = listOf(
                    NameConflictResolution.AUTO_RENAME to stringResource(R.string.conflict_opt_auto_rename),
                    NameConflictResolution.OVERWRITE to stringResource(R.string.conflict_opt_overwrite),
                    NameConflictResolution.SKIP to stringResource(R.string.conflict_opt_skip),
                    NameConflictResolution.ASK_EVERY_TIME to stringResource(R.string.conflict_opt_ask_every_time)
                ),
                selectedItem = state.conflictResolution,
                onSelect = { viewModel.setConflictResolution(it) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        SettingsActiveDialog.FOLDER_ORGANIZATION -> {
            SelectionDialog(
                title = stringResource(R.string.settings_org_title),
                items = listOf(
                    SubfolderOrganization.NONE to stringResource(R.string.subfolder_org_none),
                    SubfolderOrganization.PLATFORM to stringResource(R.string.subfolder_org_platform),
                    SubfolderOrganization.DATE to stringResource(R.string.subfolder_org_date)
                ),
                selectedItem = state.subfolderOrganization,
                onSelect = { viewModel.setSubfolderOrganization(it) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        SettingsActiveDialog.DEFAULT_QUALITY -> {
            SelectionDialog(
                title = stringResource(R.string.settings_default_quality_title),
                items = listOf(
                    DefaultDownloadQuality.ALWAYS_ASK to stringResource(R.string.quality_always_ask),
                    DefaultDownloadQuality.BEST to stringResource(R.string.quality_best),
                    DefaultDownloadQuality.RES_1080P to stringResource(R.string.quality_1080p),
                    DefaultDownloadQuality.RES_720P to stringResource(R.string.quality_720p),
                    DefaultDownloadQuality.RES_480P to stringResource(R.string.quality_480p),
                    DefaultDownloadQuality.AUDIO_ONLY to stringResource(R.string.quality_audio_only)
                ),
                selectedItem = state.defaultQuality,
                onSelect = { viewModel.setDefaultQuality(it) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        SettingsActiveDialog.DEFAULT_FORMAT -> {
            SelectionDialog(
                title = stringResource(R.string.settings_default_format_title),
                items = listOf(
                    DefaultContainerFormat.MP4 to "MP4 (H.264 / AAC - Best Compatibility)",
                    DefaultContainerFormat.MKV to "MKV (VP9 / AV1 / Opus - High Efficiency)"
                ),
                selectedItem = state.defaultFormat,
                onSelect = { viewModel.setDefaultFormat(it) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        SettingsActiveDialog.SPEED_LIMIT -> {
            SelectionDialog(
                title = stringResource(R.string.settings_speed_limit_title),
                items = listOf(
                    SpeedLimit.NO_LIMIT to stringResource(R.string.speed_limit_none),
                    SpeedLimit.SPEED_512_KBPS to "512 KB/s",
                    SpeedLimit.SPEED_1_MBPS to "1 MB/s",
                    SpeedLimit.SPEED_2_MBPS to "2 MB/s",
                    SpeedLimit.SPEED_5_MBPS to "5 MB/s",
                    SpeedLimit.SPEED_10_MBPS to "10 MB/s"
                ),
                selectedItem = state.speedLimit,
                onSelect = { viewModel.setSpeedLimit(it) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        SettingsActiveDialog.SCHEDULE_HOURS -> {
            ScheduleHoursDialog(
                currentWindow = state.scheduleWindow,
                onConfirm = { viewModel.setScheduleWindow(it) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        SettingsActiveDialog.PROXY_CONFIG -> {
            ProxyConfigDialog(
                enabled = state.proxyEnabled,
                type = state.proxyType,
                host = state.proxyHost,
                port = state.proxyPort,
                onConfirm = { pEnabled, pType, pHost, pPort ->
                    viewModel.setProxy(pEnabled, pType, pHost, pPort)
                },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        SettingsActiveDialog.COOKIES_IMPORT -> {
            CookiesImportDialog(
                onImport = { content, filename ->
                    viewModel.importCookies(content, filename)
                },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        SettingsActiveDialog.RESET_CONFIRMATION -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text(stringResource(R.string.settings_reset_dialog_title)) },
                text = { Text(stringResource(R.string.settings_reset_dialog_desc)) },
                confirmButton = {
                    Button(
                        onClick = { viewModel.resetToDefaults() },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.settings_reset_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        SettingsActiveDialog.LEGAL_NOTICE -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text(stringResource(R.string.settings_legal_title)) },
                text = {
                    Text(
                        text = stringResource(R.string.settings_legal_desc) + "\n\n" +
                               stringResource(R.string.settings_privacy_desc)
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) {
                        Text(stringResource(R.string.action_continue))
                    }
                }
            )
        }

        SettingsActiveDialog.LICENSES -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text(stringResource(R.string.settings_licenses_title)) },
                text = {
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("• yt-dlp & youtubedl-android (Unlicense)", style = MaterialTheme.typography.bodyMedium)
                        Text("• FFmpeg (LGPL / GPL)", style = MaterialTheme.typography.bodyMedium)
                        Text("• Aria2c (GPL v2)", style = MaterialTheme.typography.bodyMedium)
                        Text("• Android Jetpack & Jetpack Compose (Apache 2.0)", style = MaterialTheme.typography.bodyMedium)
                        Text("• Google Media3 ExoPlayer (Apache 2.0)", style = MaterialTheme.typography.bodyMedium)
                        Text("• Square OkHttp & Coil (Apache 2.0)", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) {
                        Text(stringResource(R.string.action_continue))
                    }
                }
            )
        }

        SettingsActiveDialog.NONE -> Unit
    }
}

// -----------------------------------------------------------------------------
// Component Primitives
// -----------------------------------------------------------------------------

@Composable
fun SettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            content()
        }
    }
}

@Composable
fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    description: String,
    valueText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (titleColor == MaterialTheme.colorScheme.error) titleColor else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (valueText.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsSliderItem(
    icon: ImageVector,
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    description: String,
    buttonLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = onAction,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(buttonLabel)
        }
    }
}

// -----------------------------------------------------------------------------
// Selection Dialog Generic
// -----------------------------------------------------------------------------

@Composable
fun <T> SelectionDialog(
    title: String,
    items: List<Pair<T, String>>,
    selectedItem: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items.forEach { (item, label) ->
                    val isSelected = item == selectedItem
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(item) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(item) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun ScheduleHoursDialog(
    currentWindow: DownloadScheduleWindow,
    onConfirm: (DownloadScheduleWindow) -> Unit,
    onDismiss: () -> Unit
) {
    var enabled by remember { mutableStateOf(currentWindow.enabled) }
    var startHour by remember { mutableIntStateOf(currentWindow.startHour) }
    var endHour by remember { mutableIntStateOf(currentWindow.endHour) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_allowed_hours_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.settings_allowed_hours_desc))
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                if (enabled) {
                    Text("Start Hour: %02d:00".format(startHour), fontWeight = FontWeight.Medium)
                    Slider(
                        value = startHour.toFloat(),
                        onValueChange = { startHour = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 22
                    )

                    Text("End Hour: %02d:00".format(endHour), fontWeight = FontWeight.Medium)
                    Slider(
                        value = endHour.toFloat(),
                        onValueChange = { endHour = it.toInt() },
                        valueRange = 0f..23f,
                        steps = 22
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    DownloadScheduleWindow(
                        enabled = enabled,
                        startHour = startHour,
                        startMinute = 0,
                        endHour = endHour,
                        endMinute = 0
                    )
                )
            }) {
                Text(stringResource(R.string.action_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun ProxyConfigDialog(
    enabled: Boolean,
    type: ProxyType,
    host: String,
    port: Int,
    onConfirm: (Boolean, ProxyType, String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var proxyEnabled by remember { mutableStateOf(enabled) }
    var proxyType by remember { mutableStateOf(type) }
    var proxyHost by remember { mutableStateOf(host) }
    var proxyPort by remember { mutableStateOf(port.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_proxy_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Proxy")
                    Switch(checked = proxyEnabled, onCheckedChange = { proxyEnabled = it })
                }

                if (proxyEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(ProxyType.HTTP, ProxyType.SOCKS5).forEach { pType ->
                            OutlinedButton(
                                onClick = { proxyType = pType },
                                shape = RoundedCornerShape(8.dp),
                                colors = if (proxyType == pType) {
                                    androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                } else androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(pType.name)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = proxyHost,
                        onValueChange = { proxyHost = it },
                        label = { Text("Proxy Host (e.g. 127.0.0.1)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = proxyPort,
                        onValueChange = { proxyPort = it },
                        label = { Text("Port (e.g. 8080)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedPort = proxyPort.toIntOrNull() ?: 8080
                onConfirm(proxyEnabled, proxyType, proxyHost, parsedPort)
            }) {
                Text(stringResource(R.string.action_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun CookiesImportDialog(
    onImport: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("cookies.txt") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_cookies_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.settings_cookies_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Paste Netscape format cookies content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rawText.isNotBlank()) {
                        onImport(rawText, fileName)
                    }
                },
                enabled = rawText.isNotBlank()
            ) {
                Text(stringResource(R.string.settings_cookies_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
