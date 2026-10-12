package com.vidget.ui.screens.settings

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsModelAndConfigTest {

    @Test
    fun testAppLanguageCodes() {
        assertEquals("system", AppLanguage.SYSTEM.code)
        assertEquals("ar", AppLanguage.ARABIC.code)
        assertEquals("en", AppLanguage.ENGLISH.code)
    }

    @Test
    fun testSpeedLimitBitrates() {
        assertEquals(0L, SpeedLimit.NO_LIMIT.bytesPerSec)
        assertEquals(512 * 1024L, SpeedLimit.SPEED_512_KBPS.bytesPerSec)
        assertEquals(1024 * 1024L, SpeedLimit.SPEED_1_MBPS.bytesPerSec)
        assertEquals(2 * 1024 * 1024L, SpeedLimit.SPEED_2_MBPS.bytesPerSec)
        assertEquals(5 * 1024 * 1024L, SpeedLimit.SPEED_5_MBPS.bytesPerSec)
        assertEquals(10 * 1024 * 1024L, SpeedLimit.SPEED_10_MBPS.bytesPerSec)
    }

    @Test
    fun testScheduleWindowFormatting() {
        val window = DownloadScheduleWindow(
            enabled = true,
            startHour = 2,
            startMinute = 15,
            endHour = 7,
            endMinute = 45
        )
        assertTrue(window.enabled)
        assertEquals("02:15 – 07:45", window.formatDisplay())
    }

    @Test
    fun testDefaultSettingsUiStateInitialization() {
        val defaultState = SettingsUiState()
        assertEquals(AppLanguage.SYSTEM, defaultState.language)
        assertEquals(ThemeMode.SYSTEM, defaultState.themeMode)
        assertTrue(defaultState.dynamicColor)
        assertTrue(defaultState.clipboardDetection)
        assertEquals(8, defaultState.connectionsCount)
        assertEquals(3, defaultState.maxConcurrentDownloads)
        assertEquals(DefaultContainerFormat.MP4, defaultState.defaultFormat)
        assertEquals(DefaultDownloadQuality.ALWAYS_ASK, defaultState.defaultQuality)
        assertEquals(NameConflictResolution.AUTO_RENAME, defaultState.conflictResolution)
        assertEquals(SubfolderOrganization.NONE, defaultState.subfolderOrganization)
        assertFalse(defaultState.wifiOnly)
        assertFalse(defaultState.dataSaver)
        assertEquals(SpeedLimit.NO_LIMIT, defaultState.speedLimit)
        assertTrue(defaultState.blockOnRoaming)
        assertTrue(defaultState.ytDlpAutoUpdate)
        assertEquals(YtDlpChannel.STABLE, defaultState.ytDlpChannel)
        assertFalse(defaultState.proxyEnabled)
        assertEquals(SettingsActiveDialog.NONE, defaultState.activeDialog)
    }

    @Test
    fun testStorageInfoMetrics() {
        val storage = StorageInfo(
            cacheSizeBytes = 50 * 1024 * 1024L,
            orphanFilesCount = 4,
            orphanFilesSizeBytes = 120 * 1024 * 1024L,
            freeSpaceBytes = 15L * 1024 * 1024 * 1024,
            totalSpaceBytes = 64L * 1024 * 1024 * 1024
        )
        assertEquals(4, storage.orphanFilesCount)
        assertEquals(50 * 1024 * 1024L, storage.cacheSizeBytes)
        assertTrue(storage.freeSpaceBytes < storage.totalSpaceBytes)
    }

    @Test
    fun testProxyTypeProtocols() {
        val types = ProxyType.values()
        assertTrue(types.contains(ProxyType.NONE))
        assertTrue(types.contains(ProxyType.HTTP))
        assertTrue(types.contains(ProxyType.SOCKS5))
    }
}
