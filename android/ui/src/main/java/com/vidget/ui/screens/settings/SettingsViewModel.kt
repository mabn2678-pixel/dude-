package com.vidget.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidget.core.permission.PermissionManager
import com.vidget.domain.model.AppLanguage
import com.vidget.domain.model.DefaultContainerFormat
import com.vidget.domain.model.DefaultDownloadQuality
import com.vidget.domain.model.DownloadScheduleWindow
import com.vidget.domain.model.NameConflictResolution
import com.vidget.domain.model.ProxyType
import com.vidget.domain.model.SpeedLimit
import com.vidget.domain.model.StorageInfo
import com.vidget.domain.model.SubfolderOrganization
import com.vidget.domain.model.ThemeMode
import com.vidget.domain.model.YtDlpChannel
import com.vidget.domain.repository.DestinationRepository
import com.vidget.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SettingsActiveDialog {
    NONE,
    LANGUAGE,
    THEME,
    CONFLICT_RESOLUTION,
    FOLDER_ORGANIZATION,
    DEFAULT_QUALITY,
    DEFAULT_FORMAT,
    SPEED_LIMIT,
    SCHEDULE_HOURS,
    PROXY_CONFIG,
    COOKIES_IMPORT,
    RESET_CONFIRMATION,
    LEGAL_NOTICE,
    LICENSES
}

data class SettingsUiState(
    // 1. General
    val language: AppLanguage = AppLanguage.SYSTEM,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val clipboardDetection: Boolean = true,

    // 2. Downloads
    val saveDirectoryUri: String? = null,
    val saveDirectoryName: String = "Downloads/VidGet",
    val namingTemplate: String = "{title} - {quality}.{ext}",
    val conflictResolution: NameConflictResolution = NameConflictResolution.AUTO_RENAME,
    val subfolderOrganization: SubfolderOrganization = SubfolderOrganization.NONE,
    val defaultQuality: DefaultDownloadQuality = DefaultDownloadQuality.ALWAYS_ASK,
    val defaultFormat: DefaultContainerFormat = DefaultContainerFormat.MP4,
    val connectionsCount: Int = 8,
    val maxConcurrentDownloads: Int = 3,
    val embedMetadata: Boolean = true,

    // 3. Network
    val wifiOnly: Boolean = false,
    val dataSaver: Boolean = false,
    val speedLimit: SpeedLimit = SpeedLimit.NO_LIMIT,
    val scheduleWindow: DownloadScheduleWindow = DownloadScheduleWindow(),
    val blockOnRoaming: Boolean = true,

    // 4. Storage
    val storageInfo: StorageInfo = StorageInfo(),

    // 5. Advanced
    val ytDlpCurrentVersion: String = "2025.01.26",
    val ytDlpAutoUpdate: Boolean = true,
    val ytDlpChannel: YtDlpChannel = YtDlpChannel.STABLE,
    val isUpdatingYtDlp: Boolean = false,
    val cookiesFileName: String? = null,
    val proxyEnabled: Boolean = false,
    val proxyType: ProxyType = ProxyType.HTTP,
    val proxyHost: String = "",
    val proxyPort: Int = 8080,

    // UI Interactive state
    val activeDialog: SettingsActiveDialog = SettingsActiveDialog.NONE,
    val snackbarMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val destinationRepository: DestinationRepository,
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _interactiveState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            settingsRepository.language,
            settingsRepository.themeMode,
            settingsRepository.dynamicColor,
            settingsRepository.clipboardDetection,
            destinationRepository.currentDirectoryName
        ) { lang, theme, dynamic, clip, dirName ->
            IntermediateGeneral(lang, theme, dynamic, clip, dirName)
        },
        combine(
            settingsRepository.namingTemplate,
            settingsRepository.conflictResolution,
            settingsRepository.subfolderOrganization,
            settingsRepository.defaultQuality,
            settingsRepository.defaultFormat
        ) { naming, conflict, org, qual, fmt ->
            IntermediateDownloadCore(naming, conflict, org, qual, fmt)
        },
        combine(
            settingsRepository.connectionsCount,
            settingsRepository.maxConcurrentDownloads,
            settingsRepository.embedMetadata,
            settingsRepository.wifiOnly,
            settingsRepository.dataSaver
        ) { conn, maxConc, embed, wifi, dataSaver ->
            IntermediateDownloadPerf(conn, maxConc, embed, wifi, dataSaver)
        },
        combine(
            settingsRepository.speedLimit,
            settingsRepository.scheduleWindow,
            settingsRepository.blockOnRoaming,
            settingsRepository.storageInfo
        ) { speed, sched, roaming, storage ->
            IntermediateNetworkStorage(speed, sched, roaming, storage)
        },
        combine(
            settingsRepository.ytDlpCurrentVersion,
            settingsRepository.ytDlpAutoUpdate,
            settingsRepository.ytDlpChannel,
            settingsRepository.cookiesFileName,
            combine(
                settingsRepository.proxyEnabled,
                settingsRepository.proxyType,
                settingsRepository.proxyHost,
                settingsRepository.proxyPort,
                _interactiveState
            ) { pEnabled, pType, pHost, pPort, inter ->
                ProxyAndInteractive(pEnabled, pType, pHost, pPort, inter)
            }
        ) { yVer, yAuto, yChan, cookies, proxyInter ->
            IntermediateAdvanced(yVer, yAuto, yChan, cookies, proxyInter)
        }
    ) { general, dlCore, dlPerf, netStorage, adv ->
        SettingsUiState(
            language = general.lang,
            themeMode = general.theme,
            dynamicColor = general.dynamic,
            clipboardDetection = general.clip,
            saveDirectoryName = general.dirName,
            namingTemplate = dlCore.naming,
            conflictResolution = dlCore.conflict,
            subfolderOrganization = dlCore.org,
            defaultQuality = dlCore.qual,
            defaultFormat = dlCore.fmt,
            connectionsCount = dlPerf.conn,
            maxConcurrentDownloads = dlPerf.maxConc,
            embedMetadata = dlPerf.embed,
            wifiOnly = dlPerf.wifi,
            dataSaver = dlPerf.dataSaver,
            speedLimit = netStorage.speed,
            scheduleWindow = netStorage.sched,
            blockOnRoaming = netStorage.roaming,
            storageInfo = netStorage.storage,
            ytDlpCurrentVersion = adv.yVer,
            ytDlpAutoUpdate = adv.yAuto,
            ytDlpChannel = adv.yChan,
            cookiesFileName = adv.cookies,
            proxyEnabled = adv.proxyInter.pEnabled,
            proxyType = adv.proxyInter.pType,
            proxyHost = adv.proxyInter.pHost,
            proxyPort = adv.proxyInter.pPort,
            isUpdatingYtDlp = adv.proxyInter.inter.isUpdatingYtDlp,
            activeDialog = adv.proxyInter.inter.activeDialog,
            snackbarMessage = adv.proxyInter.inter.snackbarMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        viewModelScope.launch {
            settingsRepository.refreshStorageInfo()
        }
    }

    // -------------------------------------------------------------------------
    // Dialog Controls
    // -------------------------------------------------------------------------

    fun openDialog(dialog: SettingsActiveDialog) {
        _interactiveState.update { it.copy(activeDialog = dialog) }
    }

    fun dismissDialog() {
        _interactiveState.update { it.copy(activeDialog = SettingsActiveDialog.NONE) }
    }

    fun clearSnackbar() {
        _interactiveState.update { it.copy(snackbarMessage = null) }
    }

    // -------------------------------------------------------------------------
    // 1. General Actions
    // -------------------------------------------------------------------------

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
            dismissDialog()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
            dismissDialog()
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColor(enabled)
        }
    }

    fun setClipboardDetection(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setClipboardDetection(enabled)
        }
    }

    // -------------------------------------------------------------------------
    // 2. Download Actions
    // -------------------------------------------------------------------------

    fun setNamingTemplate(template: String) {
        viewModelScope.launch {
            settingsRepository.setNamingTemplate(template)
        }
    }

    fun setConflictResolution(resolution: NameConflictResolution) {
        viewModelScope.launch {
            settingsRepository.setConflictResolution(resolution)
            dismissDialog()
        }
    }

    fun setSubfolderOrganization(organization: SubfolderOrganization) {
        viewModelScope.launch {
            settingsRepository.setSubfolderOrganization(organization)
            dismissDialog()
        }
    }

    fun setDefaultQuality(quality: DefaultDownloadQuality) {
        viewModelScope.launch {
            settingsRepository.setDefaultQuality(quality)
            dismissDialog()
        }
    }

    fun setDefaultFormat(format: DefaultContainerFormat) {
        viewModelScope.launch {
            settingsRepository.setDefaultFormat(format)
            dismissDialog()
        }
    }

    fun setConnectionsCount(count: Int) {
        viewModelScope.launch {
            settingsRepository.setConnectionsCount(count)
        }
    }

    fun setMaxConcurrentDownloads(count: Int) {
        viewModelScope.launch {
            settingsRepository.setMaxConcurrentDownloads(count)
        }
    }

    fun setEmbedMetadata(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEmbedMetadata(enabled)
        }
    }

    // -------------------------------------------------------------------------
    // 3. Network Actions
    // -------------------------------------------------------------------------

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWifiOnly(enabled)
        }
    }

    fun setDataSaver(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDataSaver(enabled)
        }
    }

    fun setSpeedLimit(limit: SpeedLimit) {
        viewModelScope.launch {
            settingsRepository.setSpeedLimit(limit)
            dismissDialog()
        }
    }

    fun setScheduleWindow(window: DownloadScheduleWindow) {
        viewModelScope.launch {
            settingsRepository.setScheduleWindow(window)
            dismissDialog()
        }
    }

    fun setBlockOnRoaming(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBlockOnRoaming(enabled)
        }
    }

    // -------------------------------------------------------------------------
    // 4. Storage Actions
    // -------------------------------------------------------------------------

    fun clearCache() {
        viewModelScope.launch {
            val freedBytes = settingsRepository.clearCache()
            val mb = freedBytes / (1024 * 1024)
            _interactiveState.update {
                it.copy(snackbarMessage = "Cleared cache ($mb MB freed)")
            }
        }
    }

    fun deleteOrphanFiles() {
        viewModelScope.launch {
            val (count, bytes) = settingsRepository.deleteOrphanFiles()
            val mb = bytes / (1024 * 1024)
            _interactiveState.update {
                it.copy(snackbarMessage = "Removed $count orphaned files ($mb MB freed)")
            }
        }
    }

    // -------------------------------------------------------------------------
    // 5. Advanced Actions
    // -------------------------------------------------------------------------

    fun setYtDlpAutoUpdate(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setYtDlpAutoUpdate(enabled)
        }
    }

    fun setYtDlpChannel(channel: YtDlpChannel) {
        viewModelScope.launch {
            settingsRepository.setYtDlpChannel(channel)
        }
    }

    fun checkAndUpdateYtDlp() {
        viewModelScope.launch {
            _interactiveState.update { it.copy(isUpdatingYtDlp = true) }
            val currentChan = uiState.value.ytDlpChannel
            val result = settingsRepository.checkAndUpdateYtDlp(currentChan)
            _interactiveState.update {
                it.copy(
                    isUpdatingYtDlp = false,
                    snackbarMessage = result.getOrNull() ?: "Update check completed"
                )
            }
        }
    }

    fun importCookies(content: String, filename: String) {
        viewModelScope.launch {
            settingsRepository.importCookies(content, filename)
            dismissDialog()
            _interactiveState.update {
                it.copy(snackbarMessage = "Imported cookies file: $filename")
            }
        }
    }

    fun clearCookies() {
        viewModelScope.launch {
            settingsRepository.clearCookies()
            _interactiveState.update {
                it.copy(snackbarMessage = "Cleared cookies")
            }
        }
    }

    fun setProxy(enabled: Boolean, type: ProxyType, host: String, port: Int) {
        viewModelScope.launch {
            settingsRepository.setProxy(enabled, type, host, port)
            dismissDialog()
            _interactiveState.update {
                it.copy(snackbarMessage = if (enabled) "Proxy enabled: $host:$port" else "Proxy disabled")
            }
        }
    }

    fun openBatteryOptimization(context: Context) {
        permissionManager.openBatteryOptimizationSettings(context)
    }

    fun exportDiagnosticLogs(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val logs = settingsRepository.exportDiagnosticLogs()
            onReady(logs)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
            dismissDialog()
            _interactiveState.update {
                it.copy(snackbarMessage = "All settings reset to defaults")
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Internal helper data holders for combine
// -----------------------------------------------------------------------------

private data class IntermediateGeneral(
    val lang: AppLanguage,
    val theme: ThemeMode,
    val dynamic: Boolean,
    val clip: Boolean,
    val dirName: String
)

private data class IntermediateDownloadCore(
    val naming: String,
    val conflict: NameConflictResolution,
    val org: SubfolderOrganization,
    val qual: DefaultDownloadQuality,
    val fmt: DefaultContainerFormat
)

private data class IntermediateDownloadPerf(
    val conn: Int,
    val maxConc: Int,
    val embed: Boolean,
    val wifi: Boolean,
    val dataSaver: Boolean
)

private data class IntermediateNetworkStorage(
    val speed: SpeedLimit,
    val sched: DownloadScheduleWindow,
    val roaming: Boolean,
    val storage: StorageInfo
)

private data class ProxyAndInteractive(
    val pEnabled: Boolean,
    val pType: ProxyType,
    val pHost: String,
    val pPort: Int,
    val inter: SettingsUiState
)

private data class IntermediateAdvanced(
    val yVer: String,
    val yAuto: Boolean,
    val yChan: YtDlpChannel,
    val cookies: String?,
    val proxyInter: ProxyAndInteractive
)
