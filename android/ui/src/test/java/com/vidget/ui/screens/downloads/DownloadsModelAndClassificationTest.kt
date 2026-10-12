package com.vidget.ui.screens.downloads

import com.vidget.domain.model.DownloadStatus
import com.vidget.domain.model.DownloadTask
import com.vidget.domain.model.FailureReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadsModelAndClassificationTest {

    @Test
    fun failureReason_classification_checks_retryability() {
        assertTrue(FailureReason.NETWORK_ERROR.isRetryable)
        assertTrue(FailureReason.MERGE_FAILED.isRetryable)

        assertFalse(FailureReason.DISK_FULL.isRetryable)
        assertFalse(FailureReason.PERMISSION_DENIED.isRetryable)
        assertFalse(FailureReason.LINK_EXPIRED.isRetryable)
        assertFalse(FailureReason.UNSUPPORTED_OR_PRIVATE.isRetryable)
        assertFalse(FailureReason.UNKNOWN.isRetryable)
    }

    @Test
    fun downloadTask_initial_and_progress_state_consistency() {
        val task = DownloadTask(
            id = "task_test_1",
            url = "https://example.com/video.mp4",
            title = "Test Video",
            qualityLabel = "1080p",
            container = "mp4",
            status = DownloadStatus.DOWNLOADING,
            downloadedBytes = 50 * 1024 * 1024L,
            totalBytes = 100 * 1024 * 1024L,
            speedBytesPerSec = 5 * 1024 * 1024L,
            etaSeconds = 10L,
            progressPercent = 50.0f
        )

        assertEquals("task_test_1", task.id)
        assertEquals(DownloadStatus.DOWNLOADING, task.status)
        assertEquals(50.0f, task.progressPercent, 0.01f)
        assertEquals(10L, task.etaSeconds)
        assertNull(task.failureReason)
    }

    @Test
    fun downloadTask_completed_state_stores_local_path() {
        val completedTask = DownloadTask(
            id = "task_done_1",
            url = "https://example.com/video.mp4",
            title = "Test Video Completed",
            status = DownloadStatus.COMPLETED,
            downloadedBytes = 100 * 1024 * 1024L,
            totalBytes = 100 * 1024 * 1024L,
            progressPercent = 100.0f,
            localFilePath = "content://media/external/downloads/42",
            destinationUri = "Downloads/VidGet/Test_Video.mp4",
            completedAt = 1600000000000L
        )

        assertEquals(DownloadStatus.COMPLETED, completedTask.status)
        assertEquals("content://media/external/downloads/42", completedTask.localFilePath)
        assertTrue((completedTask.completedAt ?: 0L) > 0L)
    }
}
