package com.vidget.downloader.engine

import com.vidget.downloader.model.DownloadPhase
import com.vidget.downloader.model.DownloadProgress
import java.util.regex.Pattern

object ProgressParser {

    // Matches: [download]  45.2% of  120.50MiB at  12.45MiB/s ETA 00:05
    private val PROGRESS_PATTERN = Pattern.compile(
        "\\[download\\]\\s+([0-9.]+)%\\s+of\\s+(?:~?\\s*)?([0-9.]+)([KMGTP]?i?B)(?:\\s+at\\s+([0-9.]+)([KMGTP]?i?B/s))?(?:\\s+ETA\\s+([0-9:]+))?",
        Pattern.CASE_INSENSITIVE
    )

    fun parseLine(
        taskId: String,
        line: String,
        estimatedTotalBytes: Long = 0L,
        currentPhase: DownloadPhase = DownloadPhase.DOWNLOADING_VIDEO
    ): DownloadProgress? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        // Detect phase changes
        val detectedPhase = when {
            trimmed.contains("[Merger]") || trimmed.contains("Merging formats") -> DownloadPhase.MERGING
            trimmed.contains("[ExtractAudio]") || trimmed.contains("Destination: ") && trimmed.endsWith(".mp3") -> DownloadPhase.MERGING
            trimmed.contains("Saving to destination") -> DownloadPhase.SAVING
            trimmed.contains("[download] 100%") -> DownloadPhase.COMPLETED
            else -> currentPhase
        }

        val matcher = PROGRESS_PATTERN.matcher(trimmed)
        if (matcher.find()) {
            val percent = matcher.group(1)?.toFloatOrNull() ?: 0f
            val totalSizeVal = matcher.group(2)?.toDoubleOrNull() ?: 0.0
            val totalSizeUnit = matcher.group(3) ?: "MiB"

            val totalBytes = if (totalSizeVal > 0) {
                parseBytesWithUnit(totalSizeVal, totalSizeUnit)
            } else {
                estimatedTotalBytes
            }

            val speedVal = matcher.group(4)?.toDoubleOrNull() ?: 0.0
            val speedUnit = matcher.group(5) ?: "MiB/s"
            val speedBytesPerSec = if (speedVal > 0) parseSpeedWithUnit(speedVal, speedUnit) else 0L

            val etaString = matcher.group(6)
            val etaSec = parseEta(etaString)

            val downloadedBytes = ((percent / 100.0) * totalBytes).toLong()

            return DownloadProgress(
                taskId = taskId,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                speedBytesPerSec = speedBytesPerSec,
                etaSec = etaSec,
                percent = percent,
                phase = if (percent >= 100f && detectedPhase == DownloadPhase.DOWNLOADING_VIDEO) DownloadPhase.DOWNLOADING_VIDEO else detectedPhase
            )
        }

        if (detectedPhase != currentPhase) {
            return DownloadProgress(
                taskId = taskId,
                phase = detectedPhase
            )
        }

        return null
    }

    private fun parseBytesWithUnit(value: Double, unit: String): Long {
        val u = unit.uppercase()
        return when {
            u.startsWith("G") -> (value * 1024 * 1024 * 1024).toLong()
            u.startsWith("M") -> (value * 1024 * 1024).toLong()
            u.startsWith("K") -> (value * 1024).toLong()
            u.startsWith("T") -> (value * 1024L * 1024L * 1024L * 1024L)
            else -> value.toLong()
        }
    }

    private fun parseSpeedWithUnit(value: Double, unit: String): Long {
        val u = unit.uppercase()
        return when {
            u.startsWith("G") -> (value * 1024 * 1024 * 1024).toLong()
            u.startsWith("M") -> (value * 1024 * 1024).toLong()
            u.startsWith("K") -> (value * 1024).toLong()
            else -> value.toLong()
        }
    }

    private fun parseEta(etaString: String?): Long {
        if (etaString.isNullOrBlank()) return 0L
        val parts = etaString.split(":").mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            2 -> parts[0] * 60 + parts[1]
            1 -> parts[0]
            else -> 0L
        }
    }
}
