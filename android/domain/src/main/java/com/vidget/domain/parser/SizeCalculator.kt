package com.vidget.domain.parser

import com.vidget.domain.model.MediaFormat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.Locale

fun interface HeadContentLengthResolver {
    suspend fun resolveContentLength(url: String, headers: Map<String, String>): Long?
}

data class CalculatedSize(
    val sizeBytes: Long,
    val isEstimate: Boolean,
    val isUnknown: Boolean,
    val isLive: Boolean
) {
    fun formatDisplay(isArabic: Boolean = false): String {
        if (isLive) {
            return if (isArabic) "بث مباشر" else "Live Stream"
        }
        if (isUnknown || sizeBytes <= 0) {
            return if (isArabic) "غير معروف" else "Unknown"
        }

        val prefix = if (isEstimate) "~" else ""
        val formattedSize = formatBytesWesternNumerals(sizeBytes)
        return "$prefix$formattedSize"
    }

    private fun formatBytesWesternNumerals(bytes: Long): String {
        val kilo = 1024.0
        val mega = kilo * 1024
        val giga = mega * 1024

        return when {
            bytes >= giga -> String.format(Locale.US, "%.2f GB", bytes / giga)
            bytes >= mega -> String.format(Locale.US, "%.1f MB", bytes / mega)
            bytes >= kilo -> String.format(Locale.US, "%.1f KB", bytes / kilo)
            else -> String.format(Locale.US, "%d B", bytes)
        }
    }
}

class SizeCalculator(
    private val headResolver: HeadContentLengthResolver? = null
) {
    /**
     * Calculates the size for a single media format.
     */
    fun calculateFormatSize(
        format: MediaFormat,
        durationSeconds: Long,
        isLive: Boolean
    ): CalculatedSize {
        if (isLive) {
            return CalculatedSize(sizeBytes = 0, isEstimate = false, isUnknown = false, isLive = true)
        }

        // 1. Direct filesize if present
        if (format.filesize != null && format.filesize > 0) {
            return CalculatedSize(sizeBytes = format.filesize, isEstimate = false, isUnknown = false, isLive = false)
        }

        // 2. filesize_approx if present
        if (format.filesizeApprox != null && format.filesizeApprox > 0) {
            return CalculatedSize(sizeBytes = format.filesizeApprox, isEstimate = true, isUnknown = false, isLive = false)
        }

        // 3. Formula from bitrate (tbr in kbps * duration in seconds * 1000 / 8)
        val bitrateKbps = format.tbr ?: format.vbr?.let { vbr -> (format.abr ?: 128.0) + vbr }
        if (bitrateKbps != null && bitrateKbps > 0 && durationSeconds > 0) {
            val estimatedBytes = (bitrateKbps * durationSeconds * 1000.0 / 8.0).toLong()
            if (estimatedBytes > 0) {
                return CalculatedSize(sizeBytes = estimatedBytes, isEstimate = true, isUnknown = false, isLive = false)
            }
        }

        // 4. Unknown
        return CalculatedSize(sizeBytes = 0, isEstimate = true, isUnknown = true, isLive = false)
    }

    /**
     * Resolves missing sizes for formats with direct URLs using concurrent HEAD requests (up to 4 in parallel).
     */
    suspend fun resolveDirectSizesWithHead(
        formats: List<MediaFormat>,
        durationSeconds: Long,
        isLive: Boolean
    ): Map<String, CalculatedSize> = coroutineScope {
        if (headResolver == null || isLive) {
            return@coroutineScope formats.associate { it.formatId to calculateFormatSize(it, durationSeconds, isLive) }
        }

        val semaphore = Semaphore(4)
        val deferredList = formats.map { format ->
            async {
                val initialSize = calculateFormatSize(format, durationSeconds, isLive)
                if (!initialSize.isUnknown || format.url.isNullOrBlank()) {
                    format.formatId to initialSize
                } else {
                    val resolvedBytes = semaphore.withPermit {
                        try {
                            headResolver.resolveContentLength(format.url, format.httpHeaders)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (resolvedBytes != null && resolvedBytes > 0) {
                        format.formatId to CalculatedSize(
                            sizeBytes = resolvedBytes,
                            isEstimate = false,
                            isUnknown = false,
                            isLive = false
                        )
                    } else {
                        format.formatId to initialSize
                    }
                }
            }
        }

        deferredList.awaitAll().toMap()
    }

    /**
     * Computes the combined size for merged video + audio tracks.
     * Rule: Size of NEEDS_MERGE = Video size + Audio size.
     */
    fun computeMergedSize(videoSize: CalculatedSize, audioSize: CalculatedSize): CalculatedSize {
        if (videoSize.isLive || audioSize.isLive) {
            return CalculatedSize(sizeBytes = 0, isEstimate = false, isUnknown = false, isLive = true)
        }

        if (videoSize.isUnknown && audioSize.isUnknown) {
            return CalculatedSize(sizeBytes = 0, isEstimate = true, isUnknown = true, isLive = false)
        }

        val totalBytes = videoSize.sizeBytes + audioSize.sizeBytes
        val isEstimate = videoSize.isEstimate || audioSize.isEstimate || videoSize.isUnknown || audioSize.isUnknown
        return CalculatedSize(
            sizeBytes = totalBytes,
            isEstimate = isEstimate,
            isUnknown = totalBytes <= 0,
            isLive = false
        )
    }
}
