package com.vidget.downloader.engine

import com.vidget.domain.model.QualityOption
import com.vidget.downloader.model.DownloadConfig
import com.vidget.downloader.model.DownloadPhase
import com.vidget.downloader.model.DownloadProgress
import com.vidget.downloader.model.StreamType
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadEngine @Inject constructor() {

    // Map of active processIds by taskId
    private val activeProcesses = ConcurrentHashMap<String, String>()

    /**
     * Executes the download task with automatic fallback and returns a conflated Flow of progress.
     */
    fun download(
        taskId: String,
        url: String,
        qualityOption: QualityOption,
        outputDirectory: File,
        streamType: StreamType = StreamType.DIRECT_HTTP,
        config: DownloadConfig = DownloadConfig()
    ): Flow<DownloadProgress> = callbackFlow {
        val processId = "vidget_task_$taskId"
        activeProcesses[taskId] = processId

        var currentPhase = DownloadPhase.DOWNLOADING_VIDEO

        // Initial Progress
        trySend(
            DownloadProgress(
                taskId = taskId,
                totalBytes = qualityOption.sizeBytes,
                phase = DownloadPhase.QUEUED
            )
        )

        // Try downloading with preferred args (e.g. with aria2c)
        var usedAria2cFallback = false

        fun executeWithArgs(useNativeFallback: Boolean): Boolean {
            val args = ArgsBuilder.build(
                url = url,
                formatSelector = qualityOption.formatSelector,
                outputDirectory = outputDirectory.absolutePath,
                streamType = streamType,
                config = config,
                useAria2cFallbackToNative = useNativeFallback
            )

            val request = YoutubeDLRequest(emptyList())
            for (arg in args) {
                request.addOption(arg)
            }

            return try {
                YoutubeDL.getInstance().execute(
                    request = request,
                    processId = processId,
                    callback = { floatPercent, etaInSeconds, line ->
                        val parsed = ProgressParser.parseLine(
                            taskId = taskId,
                            line = line,
                            estimatedTotalBytes = qualityOption.sizeBytes,
                            currentPhase = currentPhase
                        )

                        if (parsed != null) {
                            currentPhase = parsed.phase
                            trySend(parsed)
                        } else if (floatPercent > 0) {
                            val downloaded = ((floatPercent / 100.0) * qualityOption.sizeBytes).toLong()
                            trySend(
                                DownloadProgress(
                                    taskId = taskId,
                                    downloadedBytes = downloaded,
                                    totalBytes = qualityOption.sizeBytes,
                                    etaSec = etaInSeconds,
                                    percent = floatPercent,
                                    phase = currentPhase
                                )
                            )
                        }
                    }
                )
                true
            } catch (e: Exception) {
                // If failed with aria2c and not yet fallen back, return false to trigger native fallback
                if (!useNativeFallback && streamType == StreamType.DIRECT_HTTP) {
                    false
                } else {
                    throw e
                }
            }
        }

        try {
            outputDirectory.mkdirs()
            val success = executeWithArgs(useNativeFallback = false)
            if (!success) {
                usedAria2cFallback = true
                executeWithArgs(useNativeFallback = true)
            }

            trySend(
                DownloadProgress(
                    taskId = taskId,
                    downloadedBytes = qualityOption.sizeBytes,
                    totalBytes = qualityOption.sizeBytes,
                    percent = 100f,
                    phase = DownloadPhase.COMPLETED
                )
            )
            close()
        } catch (e: Exception) {
            trySend(
                DownloadProgress(
                    taskId = taskId,
                    phase = DownloadPhase.FAILED,
                    message = e.localizedMessage ?: "Download failed"
                )
            )
            close(e)
        } finally {
            activeProcesses.remove(taskId)
        }

        awaitClose {
            cancelDownload(taskId)
        }
    }.conflate().flowOn(Dispatchers.IO)

    /**
     * Gracefully terminates an active download by task ID.
     */
    fun cancelDownload(taskId: String): Boolean {
        val processId = activeProcesses.remove(taskId) ?: return false
        return try {
            YoutubeDL.getInstance().destroyProcessById(processId)
            true
        } catch (e: Exception) {
            false
        }
    }
}
