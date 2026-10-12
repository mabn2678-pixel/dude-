package com.vidget.domain.repository

import com.vidget.domain.model.DownloadStatus
import com.vidget.domain.model.DownloadTask
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing persistent and in-memory download tasks.
 */
interface DownloadRepository {
    fun getAllTasks(): Flow<List<DownloadTask>>
    fun getTasksByStatus(statuses: Set<DownloadStatus>): Flow<List<DownloadTask>>
    fun getTaskById(taskId: String): Flow<DownloadTask?>

    suspend fun insertTask(task: DownloadTask)
    suspend fun updateTask(task: DownloadTask)
    suspend fun updateProgress(
        taskId: String,
        downloadedBytes: Long,
        totalBytes: Long,
        speedBytesPerSec: Long,
        etaSeconds: Long,
        percent: Float,
        status: DownloadStatus,
        phaseMessage: String
    )
    suspend fun pauseTask(taskId: String)
    suspend fun resumeTask(taskId: String)
    suspend fun cancelTask(taskId: String)
    suspend fun retryTask(taskId: String)
    suspend fun deleteTask(taskId: String, deleteFromDisk: Boolean = false)
    suspend fun pauseAllActive()
    suspend fun resumeAllPaused()
    suspend fun clearCompleted()
}
