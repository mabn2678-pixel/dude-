package com.vidget.domain.repository

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
import kotlinx.coroutines.flow.Flow

/**
 * Unified repository managing all user settings and preferences with reactive DataStore Flows.
 */
interface SettingsRepository {

    // -------------------------------------------------------------------------
    // 1. General (عام)
    // -------------------------------------------------------------------------
    val language: Flow<AppLanguage>
    val themeMode: Flow<ThemeMode>
    val dynamicColor: Flow<Boolean>
    val clipboardDetection: Flow<Boolean>

    suspend fun setLanguage(language: AppLanguage)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setClipboardDetection(enabled: Boolean)

    // -------------------------------------------------------------------------
    // 2. Downloads (التحميل)
    // -------------------------------------------------------------------------
    val saveDirectoryUri: Flow<String?>
    val namingTemplate: Flow<String>
    val conflictResolution: Flow<NameConflictResolution>
    val subfolderOrganization: Flow<SubfolderOrganization>
    val defaultQuality: Flow<DefaultDownloadQuality>
    val defaultFormat: Flow<DefaultContainerFormat>
    val connectionsCount: Flow<Int> // 1..16
    val maxConcurrentDownloads: Flow<Int> // 1..5
    val embedMetadata: Flow<Boolean>

    suspend fun setSaveDirectoryUri(uri: String?)
    suspend fun setNamingTemplate(template: String)
    suspend fun setConflictResolution(resolution: NameConflictResolution)
    suspend fun setSubfolderOrganization(organization: SubfolderOrganization)
    suspend fun setDefaultQuality(quality: DefaultDownloadQuality)
    suspend fun setDefaultFormat(format: DefaultContainerFormat)
    suspend fun setConnectionsCount(count: Int)
    suspend fun setMaxConcurrentDownloads(count: Int)
    suspend fun setEmbedMetadata(enabled: Boolean)

    // -------------------------------------------------------------------------
    // 3. Network (الشبكة)
    // -------------------------------------------------------------------------
    val wifiOnly: Flow<Boolean>
    val dataSaver: Flow<Boolean>
    val speedLimit: Flow<SpeedLimit>
    val scheduleWindow: Flow<DownloadScheduleWindow>
    val blockOnRoaming: Flow<Boolean>

    suspend fun setWifiOnly(enabled: Boolean)
    suspend fun setDataSaver(enabled: Boolean)
    suspend fun setSpeedLimit(limit: SpeedLimit)
    suspend fun setScheduleWindow(window: DownloadScheduleWindow)
    suspend fun setBlockOnRoaming(enabled: Boolean)

    // -------------------------------------------------------------------------
    // 4. Storage (التخزين)
    // -------------------------------------------------------------------------
    val storageInfo: Flow<StorageInfo>
    suspend fun refreshStorageInfo()
    suspend fun clearCache(): Long // Returns bytes cleared
    suspend fun deleteOrphanFiles(): Pair<Int, Long> // Returns count of files and bytes cleared

    // -------------------------------------------------------------------------
    // 5. Advanced (متقدم)
    // -------------------------------------------------------------------------
    val ytDlpCurrentVersion: Flow<String>
    val ytDlpAutoUpdate: Flow<Boolean>
    val ytDlpChannel: Flow<YtDlpChannel>
    val cookiesFileName: Flow<String?>
    val proxyEnabled: Flow<Boolean>
    val proxyType: Flow<ProxyType>
    val proxyHost: Flow<String>
    val proxyPort: Flow<Int>

    suspend fun setYtDlpAutoUpdate(enabled: Boolean)
    suspend fun setYtDlpChannel(channel: YtDlpChannel)
    suspend fun checkAndUpdateYtDlp(channel: YtDlpChannel): Result<String>
    suspend fun importCookies(content: String, filename: String)
    suspend fun clearCookies()
    suspend fun setProxy(enabled: Boolean, type: ProxyType, host: String, port: Int)
    suspend fun exportDiagnosticLogs(): String
    suspend fun resetToDefaults()
}
