package com.vidget.domain.model

import kotlinx.serialization.Serializable

/**
 * Lifecycle status of a download task.
 */
@Serializable
enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    MERGING,
    SAVING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Domain entity representing a downloadable task with progress, metadata, and final destination.
 */
@Serializable
data class DownloadTask(
    val id: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val qualityLabel: String = "",
    val container: String = "mp4", // mp4, mkv, webm, mp3, m4a
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val progressPercent: Float = 0f,
    val phaseMessage: String = "",
    val destinationUri: String? = null, // SAF URI or file path
    val localFilePath: String? = null,  // final stored path or content URI
    val failureReason: FailureReason? = null,
    val rawErrorMessage: String? = null,
    val isLiveStream: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * Classified failure reasons with contextual explanations and recommended user actions.
 */
@Serializable
enum class FailureReason {
    NETWORK_ERROR,
    DISK_FULL,
    PERMISSION_DENIED,
    MERGE_FAILED,
    LINK_EXPIRED,
    UNSUPPORTED_OR_PRIVATE,
    UNKNOWN;

    val isRetryable: Boolean
        get() = when (this) {
            NETWORK_ERROR, MERGE_FAILED -> true
            DISK_FULL, PERMISSION_DENIED, LINK_EXPIRED, UNSUPPORTED_OR_PRIVATE, UNKNOWN -> false
        }
}
