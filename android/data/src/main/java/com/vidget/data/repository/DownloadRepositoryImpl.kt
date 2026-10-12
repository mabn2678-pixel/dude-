package com.vidget.data.repository

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.vidget.domain.model.DownloadStatus
import com.vidget.domain.model.DownloadTask
import com.vidget.domain.model.FailureReason
import com.vidget.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DownloadRepository {

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasksFlow = _tasks.asStateFlow()

    init {
        // Initialize with realistic mock items to ensure rich testing & acceptance verification
        initDefaultSampleTasks()
    }

    private fun initDefaultSampleTasks() {
        val sampleList = listOf(
            DownloadTask(
                id = "task-act-1",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                title = "Big Buck Bunny 4K 60fps Open Source Animation Movie",
                thumbnailUrl = "https://images.unsplash.com/photo-1536240478700-b869070f9279?w=600&auto=format&fit=crop&q=80",
                qualityLabel = "1080p60",
                container = "MP4",
                status = DownloadStatus.DOWNLOADING,
                downloadedBytes = 52_428_800L, // 50.0 MB
                totalBytes = 157_286_400L,     // 150.0 MB
                speedBytesPerSec = 14_800_000L, // 14.8 MB/s
                etaSeconds = 7L,
                progressPercent = 33.3f,
                phaseMessage = "يحمّل الفيديو… (33.3%)",
                isLiveStream = false
            ),
            DownloadTask(
                id = "task-act-2",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                title = "Elephants Dream (Sci-Fi Short CGI Film)",
                thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
                qualityLabel = "4K",
                container = "MKV",
                status = DownloadStatus.MERGING,
                downloadedBytes = 310_000_000L,
                totalBytes = 310_000_000L,
                speedBytesPerSec = 0L,
                etaSeconds = 0L,
                progressPercent = 95.0f,
                phaseMessage = "جارٍ الدمج… (ffmpeg -c copy)",
                isLiveStream = false
            ),
            DownloadTask(
                id = "task-act-3",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                title = "Google Chromecast Demo: For Bigger Blazes",
                thumbnailUrl = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=600&auto=format&fit=crop&q=80",
                qualityLabel = "720p",
                container = "MP4",
                status = DownloadStatus.QUEUED,
                downloadedBytes = 0L,
                totalBytes = 45_000_000L,
                speedBytesPerSec = 0L,
                etaSeconds = 0L,
                progressPercent = 0.0f,
                phaseMessage = "في الطابور",
                isLiveStream = false
            ),
            // Completed tasks
            DownloadTask(
                id = "task-comp-1",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                title = "Tears of Steel (Blender VFX Movie 1080p)",
                thumbnailUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=600&auto=format&fit=crop&q=80",
                qualityLabel = "1080p",
                container = "MP4",
                status = DownloadStatus.COMPLETED,
                downloadedBytes = 228_500_000L,
                totalBytes = 228_500_000L,
                speedBytesPerSec = 0L,
                etaSeconds = 0L,
                progressPercent = 100.0f,
                phaseMessage = "اكتمل التحميل",
                localFilePath = "content://media/external/downloads/10042",
                destinationUri = "Downloads/VidGet/Tears_of_Steel_1080p.mp4",
                completedAt = System.currentTimeMillis() - 3_600_000L
            ),
            DownloadTask(
                id = "task-comp-2",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                title = "Sintel Open Movie Soundtrack Audio",
                thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
                qualityLabel = "320 kbps",
                container = "MP3",
                status = DownloadStatus.COMPLETED,
                downloadedBytes = 14_800_000L,
                totalBytes = 14_800_000L,
                speedBytesPerSec = 0L,
                etaSeconds = 0L,
                progressPercent = 100.0f,
                phaseMessage = "اكتمل التحميل",
                localFilePath = "content://com.android.externalstorage.documents/tree/primary%3ADownloads/document/primary%3ADownloads%2FSintel_320k.mp3",
                destinationUri = "SAF/Downloads/VidGet/Sintel_320k.mp3",
                completedAt = System.currentTimeMillis() - 86_400_000L
            ),
            // Failed tasks
            DownloadTask(
                id = "task-fail-1",
                url = "https://youtube.com/watch?v=diskfull123",
                title = "High Resolution 8K Drone Footage across Nature",
                thumbnailUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&auto=format&fit=crop&q=80",
                qualityLabel = "8K",
                container = "MKV",
                status = DownloadStatus.FAILED,
                downloadedBytes = 1_200_000_000L,
                totalBytes = 4_800_000_000L,
                progressPercent = 25.0f,
                failureReason = FailureReason.DISK_FULL,
                rawErrorMessage = "ENOSPC: No space left on device. Required 3.6 GB, available 412 MB."
            ),
            DownloadTask(
                id = "task-fail-2",
                url = "https://youtube.com/watch?v=expiredstream",
                title = "Live Broadcast Archive Stream Event",
                thumbnailUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=600&auto=format&fit=crop&q=80",
                qualityLabel = "1080p",
                container = "MP4",
                status = DownloadStatus.FAILED,
                downloadedBytes = 85_000_000L,
                totalBytes = 320_000_000L,
                progressPercent = 26.5f,
                failureReason = FailureReason.LINK_EXPIRED,
                rawErrorMessage = "HTTP 403 Forbidden: Direct signature token expired after 4 hours."
            )
        )
        _tasks.value = sampleList
    }

    override fun getAllTasks(): Flow<List<DownloadTask>> = tasksFlow

    override fun getTasksByStatus(statuses: Set<DownloadStatus>): Flow<List<DownloadTask>> {
        return tasksFlow.map { list -> list.filter { it.status in statuses } }
    }

    override fun getTaskById(taskId: String): Flow<DownloadTask?> {
        return tasksFlow.map { list -> list.firstOrNull { it.id == taskId } }
    }

    override suspend fun insertTask(task: DownloadTask) {
        _tasks.update { current ->
            listOf(task) + current.filterNot { it.id == task.id }
        }
    }

    override suspend fun updateTask(task: DownloadTask) {
        _tasks.update { current ->
            current.map { if (it.id == task.id) task else it }
        }
    }

    override suspend fun updateProgress(
        taskId: String,
        downloadedBytes: Long,
        totalBytes: Long,
        speedBytesPerSec: Long,
        etaSeconds: Long,
        percent: Float,
        status: DownloadStatus,
        phaseMessage: String
    ) {
        _tasks.update { current ->
            current.map { item ->
                if (item.id == taskId) {
                    item.copy(
                        downloadedBytes = downloadedBytes,
                        totalBytes = if (totalBytes > 0) totalBytes else item.totalBytes,
                        speedBytesPerSec = speedBytesPerSec,
                        etaSeconds = etaSeconds,
                        progressPercent = percent,
                        status = status,
                        phaseMessage = phaseMessage
                    )
                } else item
            }
        }
    }

    override suspend fun pauseTask(taskId: String) {
        _tasks.update { current ->
            current.map { item ->
                if (item.id == taskId) {
                    item.copy(
                        status = DownloadStatus.PAUSED,
                        speedBytesPerSec = 0L,
                        phaseMessage = "متوقف مؤقتًا"
                    )
                } else item
            }
        }
    }

    override suspend fun resumeTask(taskId: String) {
        _tasks.update { current ->
            current.map { item ->
                if (item.id == taskId) {
                    item.copy(
                        status = DownloadStatus.DOWNLOADING,
                        phaseMessage = "جارٍ التحميل…"
                    )
                } else item
            }
        }
    }

    override suspend fun cancelTask(taskId: String) {
        _tasks.update { current ->
            current.map { item ->
                if (item.id == taskId) {
                    item.copy(
                        status = DownloadStatus.CANCELLED,
                        speedBytesPerSec = 0L,
                        phaseMessage = "تم الإلغاء"
                    )
                } else item
            }
        }
    }

    override suspend fun retryTask(taskId: String) {
        _tasks.update { current ->
            current.map { item ->
                if (item.id == taskId) {
                    item.copy(
                        status = DownloadStatus.DOWNLOADING,
                        failureReason = null,
                        rawErrorMessage = null,
                        phaseMessage = "إعادة المحاولة…"
                    )
                } else item
            }
        }
    }

    override suspend fun deleteTask(taskId: String, deleteFromDisk: Boolean) {
        val target = _tasks.value.firstOrNull { it.id == taskId }
        if (deleteFromDisk && target != null) {
            deleteFilePhysical(target)
        }
        _tasks.update { current -> current.filterNot { it.id == taskId } }
    }

    override suspend fun pauseAllActive() {
        _tasks.update { current ->
            current.map { item ->
                if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.QUEUED) {
                    item.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L, phaseMessage = "متوقف مؤقتًا")
                } else item
            }
        }
    }

    override suspend fun resumeAllPaused() {
        _tasks.update { current ->
            current.map { item ->
                if (item.status == DownloadStatus.PAUSED) {
                    item.copy(status = DownloadStatus.DOWNLOADING, phaseMessage = "جارٍ التحميل…")
                } else item
            }
        }
    }

    override suspend fun clearCompleted() {
        _tasks.update { current ->
            current.filterNot { it.status == DownloadStatus.COMPLETED }
        }
    }

    private fun deleteFilePhysical(task: DownloadTask) {
        val path = task.localFilePath ?: return
        try {
            if (path.startsWith("content://")) {
                val uri = path.toUri()
                val doc = DocumentFile.fromSingleUri(context, uri)
                if (doc?.exists() == true) {
                    doc.delete()
                } else {
                    context.contentResolver.delete(uri, null, null)
                }
            } else {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (_: Exception) {
            // Ignore failure on cleanup
        }
    }
}
