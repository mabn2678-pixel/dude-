package com.vidget.core.storage

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.io.File
import kotlin.math.max

data class SpaceCheckResult(
    val isSufficient: Boolean,
    val requiredTempBytes: Long,
    val requiredFinalBytes: Long,
    val availableTempBytes: Long,
    val availableDestinationBytes: Long,
    val deficitBytes: Long
)

/**
 * Validates storage capacity across temporary download cache and final destination.
 * Formula:
 * - Temp Cache Required = estimatedSizeBytes * 1.10
 * - Merging Overhead (if separate video & audio streams): + estimatedSizeBytes (so temp needs 2.10x)
 * - Final Destination Required = estimatedSizeBytes
 */
object StorageSpaceChecker {

    const val TEMP_BUFFER_MULTIPLIER = 1.10

    /**
     * Calculates required temporary workspace space in bytes.
     * @param estimatedSizeBytes Raw expected file size
     * @param hasSeparateStreams True if video & audio streams require FFmpeg post-processing merge
     */
    fun calculateRequiredTempBytes(
        estimatedSizeBytes: Long,
        hasSeparateStreams: Boolean
    ): Long {
        if (estimatedSizeBytes <= 0) return 0L
        val baseBuffer = (estimatedSizeBytes * TEMP_BUFFER_MULTIPLIER).toLong()
        val mergeOverhead = if (hasSeparateStreams) estimatedSizeBytes else 0L
        return baseBuffer + mergeOverhead
    }

    /**
     * Inspects free space using StatFs on given path.
     */
    fun getAvailableBytes(path: String): Long {
        return try {
            val file = File(path)
            val targetFile = if (file.exists()) file else file.parentFile ?: file
            val stat = StatFs(targetFile.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Computes total capacity in bytes for a path.
     */
    fun getTotalBytes(path: String): Long {
        return try {
            val file = File(path)
            val targetFile = if (file.exists()) file else file.parentFile ?: file
            val stat = StatFs(targetFile.absolutePath)
            stat.blockCountLong * stat.blockSizeLong
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Returns free space percentage (0.0 to 1.0) for a directory path.
     */
    fun getFreeSpaceRatio(path: String): Float {
        val total = getTotalBytes(path)
        if (total <= 0L) return 0f
        val free = getAvailableBytes(path)
        return (free.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Evaluates whether download can safely proceed without running out of disk space.
     */
    fun checkSpace(
        context: Context,
        estimatedSizeBytes: Long,
        hasSeparateStreams: Boolean,
        destinationPath: String? = null
    ): SpaceCheckResult {
        val requiredTemp = calculateRequiredTempBytes(estimatedSizeBytes, hasSeparateStreams)
        val requiredFinal = estimatedSizeBytes

        // Temp directory: context.getExternalFilesDir("downloads") or internal filesDir fallback
        val tempDir = context.getExternalFilesDir("downloads") ?: context.filesDir
        val availableTemp = getAvailableBytes(tempDir.absolutePath)

        val finalDir = destinationPath ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath ?: tempDir.absolutePath
        val availableDestination = getAvailableBytes(finalDir)

        val tempDeficit = max(0L, requiredTemp - availableTemp)
        val destDeficit = max(0L, requiredFinal - availableDestination)
        val totalDeficit = max(tempDeficit, destDeficit)

        val isSufficient = (availableTemp >= requiredTemp) && (availableDestination >= requiredFinal)

        return SpaceCheckResult(
            isSufficient = isSufficient,
            requiredTempBytes = requiredTemp,
            requiredFinalBytes = requiredFinal,
            availableTempBytes = availableTemp,
            availableDestinationBytes = availableDestination,
            deficitBytes = totalDeficit
        )
    }

    /**
     * Formats bytes into clean human-readable text (e.g. 1.25 GB, 350 MB).
     */
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0

        return when {
            bytes >= gb -> String.format(java.util.Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(java.util.Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(java.util.Locale.US, "%.1f KB", bytes / kb)
            else -> "$bytes B"
        }
    }
}
