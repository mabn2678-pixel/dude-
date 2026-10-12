package com.vidget.downloader.model

import kotlinx.serialization.Serializable

@Serializable
enum class DownloadPhase {
    QUEUED,
    DOWNLOADING_VIDEO,
    DOWNLOADING_AUDIO,
    MERGING,
    SAVING,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Serializable
data class DownloadProgress(
    val taskId: String,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSec: Long = 0L,
    val percent: Float = 0f,
    val phase: DownloadPhase = DownloadPhase.QUEUED,
    val message: String? = null
)
