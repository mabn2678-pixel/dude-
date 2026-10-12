package com.vidget.domain.model

import kotlinx.serialization.Serializable

/**
 * Supported app UI languages.
 */
@Serializable
enum class AppLanguage(val code: String) {
    SYSTEM("system"),
    ARABIC("ar"),
    ENGLISH("en")
}

/**
 * Default download quality preference when user starts downloading.
 */
@Serializable
enum class DefaultDownloadQuality {
    ALWAYS_ASK,
    BEST,
    RES_1080P,
    RES_720P,
    RES_480P,
    AUDIO_ONLY
}

/**
 * Default video container format for finished downloads.
 */
@Serializable
enum class DefaultContainerFormat {
    MP4,
    MKV
}

/**
 * Download bandwidth throttling limits.
 */
@Serializable
enum class SpeedLimit(val bytesPerSec: Long) {
    NO_LIMIT(0L),
    SPEED_512_KBPS(512 * 1024L),
    SPEED_1_MBPS(1024 * 1024L),
    SPEED_2_MBPS(2 * 1024 * 1024L),
    SPEED_5_MBPS(5 * 1024 * 1024L),
    SPEED_10_MBPS(10 * 1024 * 1024L)
}

/**
 * Release channel for yt-dlp engine updates.
 */
@Serializable
enum class YtDlpChannel {
    STABLE,
    NIGHTLY
}

/**
 * Proxy protocol configuration.
 */
@Serializable
enum class ProxyType {
    NONE,
    HTTP,
    SOCKS5
}

/**
 * Storage capacity and temporary file metrics.
 */
@Serializable
data class StorageInfo(
    val cacheSizeBytes: Long = 0L,
    val orphanFilesCount: Int = 0,
    val orphanFilesSizeBytes: Long = 0L,
    val freeSpaceBytes: Long = 0L,
    val totalSpaceBytes: Long = 0L
)

/**
 * Scheduled download time window.
 */
@Serializable
data class DownloadScheduleWindow(
    val enabled: Boolean = false,
    val startHour: Int = 0,
    val startMinute: Int = 0,
    val endHour: Int = 6,
    val endMinute: Int = 0
) {
    fun formatDisplay(): String {
        return "%02d:%02d – %02d:%02d".format(startHour, startMinute, endHour, endMinute)
    }
}
