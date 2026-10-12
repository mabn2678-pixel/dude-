package com.vidget.domain.parser

import com.vidget.domain.model.MediaFormat
import com.vidget.domain.model.MediaInfo
import com.vidget.domain.model.QualityKind
import com.vidget.domain.model.QualityOption

data class ParsedQualityResult(
    val videoOptions: List<QualityOption>,
    val audioOptions: List<QualityOption>
)

class QualityParser(
    private val sizeCalculator: SizeCalculator = SizeCalculator()
) {

    companion object {
        // Approved target resolutions in descending order
        val APPROVED_HEIGHTS = listOf(
            4320 to "8K",
            2160 to "4K",
            1440 to "1440p",
            1080 to "1080p",
            720 to "720p",
            480 to "480p",
            360 to "360p",
            240 to "240p",
            144 to "144p"
        )
    }

    /**
     * Parses MediaInfo formats into curated user options (video and audio).
     */
    fun parse(mediaInfo: MediaInfo): ParsedQualityResult {
        val validFormats = mediaInfo.formats.filter { isValidFormat(it) }

        // Find available audio tracks for pairing
        val audioTracks = validFormats.filter { it.isAudioOnly }
        val bestM4aAudio = audioTracks
            .filter { it.ext.equals("m4a", ignoreCase = true) || it.acodec?.contains("mp4a", ignoreCase = true) == true }
            .maxByOrNull { it.abr ?: it.tbr ?: 0.0 }
        val bestOpusAudio = audioTracks
            .filter { it.ext.equals("webm", ignoreCase = true) || it.ext.equals("opus", ignoreCase = true) || it.acodec?.contains("opus", ignoreCase = true) == true }
            .maxByOrNull { it.abr ?: it.tbr ?: 0.0 }
        val fallbackAudio = audioTracks.maxByOrNull { it.abr ?: it.tbr ?: 0.0 }

        // Group video formats by approved height bucket
        val videoFormats = validFormats.filter { it.isVideo }
        val groupedByHeight = mutableMapOf<Int, MutableList<MediaFormat>>()

        for (format in videoFormats) {
            val rawHeight = format.height ?: continue
            val mappedHeight = mapToApprovedHeight(rawHeight) ?: continue
            groupedByHeight.getOrPut(mappedHeight) { mutableListOf() }.add(format)
        }

        // Build one best video option per approved height bucket
        val candidateVideoOptions = mutableListOf<QualityOption>()
        for ((approvedHeight, formatsInBucket) in groupedByHeight.entries.sortedByDescending { it.key }) {
            val bestFormat = selectBestVideoFormat(formatsInBucket) ?: continue

            val isCombined = bestFormat.isCombined
            val pairedAudio = if (isCombined) {
                null
            } else {
                if (bestFormat.ext.equals("mp4", ignoreCase = true)) {
                    bestM4aAudio ?: fallbackAudio
                } else {
                    bestOpusAudio ?: fallbackAudio
                }
            }

            val kind = if (isCombined) QualityKind.COMBINED else QualityKind.NEEDS_MERGE

            // Calculate size
            val videoSize = sizeCalculator.calculateFormatSize(bestFormat, mediaInfo.durationSeconds, mediaInfo.isLive)
            val finalSize = if (kind == QualityKind.NEEDS_MERGE && pairedAudio != null) {
                val audioSize = sizeCalculator.calculateFormatSize(pairedAudio, mediaInfo.durationSeconds, mediaInfo.isLive)
                sizeCalculator.computeMergedSize(videoSize, audioSize)
            } else {
                videoSize
            }

            val fps = bestFormat.fps ?: 30
            val resolutionLabel = APPROVED_HEIGHTS.firstOrNull { it.first == approvedHeight }?.second ?: "${approvedHeight}p"
            val displayLabel = if (fps >= 50) "${resolutionLabel}${if (fps >= 59) "60" else fps}" else resolutionLabel

            val formatSelector = if (kind == QualityKind.COMBINED) {
                bestFormat.formatId
            } else if (pairedAudio != null) {
                "${bestFormat.formatId}+${pairedAudio.formatId}"
            } else {
                "${bestFormat.formatId}+bestaudio"
            }

            candidateVideoOptions.add(
                QualityOption(
                    id = "video_${approvedHeight}_${bestFormat.ext}_${bestFormat.formatId}",
                    label = displayLabel,
                    height = approvedHeight,
                    fps = fps,
                    container = bestFormat.ext,
                    vcodec = bestFormat.vcodec,
                    acodec = if (isCombined) bestFormat.acodec else pairedAudio?.acodec,
                    sizeBytes = finalSize.sizeBytes,
                    sizeIsEstimate = finalSize.isEstimate,
                    isUnknownSize = finalSize.isUnknown,
                    isLive = mediaInfo.isLive,
                    kind = kind,
                    isBest = false, // Will calculate below
                    formatSelector = formatSelector,
                    videoFormat = bestFormat,
                    audioFormat = pairedAudio
                )
            )
        }

        // Assign "Best" badge: highest resolution <= 1080p with compatible codec (H.264/AVC), else highest available
        val bestOption = candidateVideoOptions
            .filter { it.height <= 1080 && isCompatibleH264(it.vcodec) }
            .maxByOrNull { it.height }
            ?: candidateVideoOptions.filter { it.height <= 1080 }.maxByOrNull { it.height }
            ?: candidateVideoOptions.firstOrNull()

        val finalVideoOptions = candidateVideoOptions.map { option ->
            if (bestOption != null && option.id == bestOption.id) {
                option.copy(isBest = true)
            } else {
                option
            }
        }

        // Build Audio Options
        val audioOptions = mutableListOf<QualityOption>()

        // 1. Best M4A
        if (bestM4aAudio != null) {
            val audioSize = sizeCalculator.calculateFormatSize(bestM4aAudio, mediaInfo.durationSeconds, mediaInfo.isLive)
            val bitrate = (bestM4aAudio.abr ?: bestM4aAudio.tbr ?: 128.0).toInt()
            audioOptions.add(
                QualityOption(
                    id = "audio_m4a_${bestM4aAudio.formatId}",
                    label = "M4A ($bitrate kbps)",
                    height = 0,
                    fps = 0,
                    container = "m4a",
                    vcodec = null,
                    acodec = bestM4aAudio.acodec ?: "AAC",
                    sizeBytes = audioSize.sizeBytes,
                    sizeIsEstimate = audioSize.isEstimate,
                    isUnknownSize = audioSize.isUnknown,
                    isLive = mediaInfo.isLive,
                    kind = QualityKind.AUDIO_ONLY,
                    isBest = true,
                    formatSelector = bestM4aAudio.formatId,
                    videoFormat = null,
                    audioFormat = bestM4aAudio,
                    audioBitrateKbps = bitrate
                )
            )
        }

        // 2. Best OPUS
        if (bestOpusAudio != null) {
            val audioSize = sizeCalculator.calculateFormatSize(bestOpusAudio, mediaInfo.durationSeconds, mediaInfo.isLive)
            val bitrate = (bestOpusAudio.abr ?: bestOpusAudio.tbr ?: 160.0).toInt()
            audioOptions.add(
                QualityOption(
                    id = "audio_opus_${bestOpusAudio.formatId}",
                    label = "OPUS ($bitrate kbps)",
                    height = 0,
                    fps = 0,
                    container = "opus",
                    vcodec = null,
                    acodec = bestOpusAudio.acodec ?: "Opus",
                    sizeBytes = audioSize.sizeBytes,
                    sizeIsEstimate = audioSize.isEstimate,
                    isUnknownSize = audioSize.isUnknown,
                    isLive = mediaInfo.isLive,
                    kind = QualityKind.AUDIO_ONLY,
                    isBest = bestM4aAudio == null,
                    formatSelector = bestOpusAudio.formatId,
                    videoFormat = null,
                    audioFormat = bestOpusAudio,
                    audioBitrateKbps = bitrate
                )
            )
        }

        // 3. MP3 conversion options (320, 192, 128 kbps)
        val sourceForMp3 = bestM4aAudio ?: bestOpusAudio ?: fallbackAudio
        val mp3Bitrates = listOf(320, 192, 128)
        for (kbps in mp3Bitrates) {
            val estimatedBytes = if (mediaInfo.durationSeconds > 0) {
                (kbps * mediaInfo.durationSeconds * 1000.0 / 8.0).toLong()
            } else 0L

            audioOptions.add(
                QualityOption(
                    id = "audio_mp3_$kbps",
                    label = "MP3 ($kbps kbps)",
                    height = 0,
                    fps = 0,
                    container = "mp3",
                    vcodec = null,
                    acodec = "mp3",
                    sizeBytes = estimatedBytes,
                    sizeIsEstimate = true,
                    isUnknownSize = estimatedBytes <= 0,
                    isLive = mediaInfo.isLive,
                    kind = QualityKind.AUDIO_ONLY,
                    isBest = false,
                    // formatSelector instructs yt-dlp / ffmpeg conversion: bestaudio/extract-audio --audio-format mp3
                    formatSelector = if (sourceForMp3 != null) "${sourceForMp3.formatId}[ext=mp3@$kbps]" else "bestaudio[ext=mp3@$kbps]",
                    videoFormat = null,
                    audioFormat = sourceForMp3,
                    audioBitrateKbps = kbps
                )
            )
        }

        return ParsedQualityResult(
            videoOptions = finalVideoOptions,
            audioOptions = audioOptions
        )
    }

    private fun mapToApprovedHeight(rawHeight: Int): Int? {
        if (rawHeight <= 0) return null
        return APPROVED_HEIGHTS
            .map { it.first }
            .firstOrNull { rawHeight >= it }
            ?: APPROVED_HEIGHTS.last().first // smallest approved
    }

    private fun isValidFormat(format: MediaFormat): Boolean {
        // Exclude storyboard, mhtml, and zero resolution corrupt formats
        if (format.isStoryboard) return false
        if (format.ext.equals("mhtml", ignoreCase = true)) return false
        if (format.formatNote?.contains("storyboard", ignoreCase = true) == true) return false

        val hasVideo = format.isVideo
        val hasAudio = format.isAudio

        if (!hasVideo && !hasAudio) return false
        if (hasVideo && (format.height == null || format.height <= 0)) return false

        return true
    }

    /**
     * Selects single best video format for this bucket:
     * Priority: MP4 (H.264/AVC) first for compatibility, then VP9, then AV1.
     */
    private fun selectBestVideoFormat(formats: List<MediaFormat>): MediaFormat? {
        return formats.maxWithOrNull(
            compareBy<MediaFormat> { format ->
                when {
                    isCompatibleH264(format.vcodec) && format.ext.equals("mp4", ignoreCase = true) -> 100
                    isCompatibleH264(format.vcodec) -> 90
                    isVp9(format.vcodec) -> 80
                    isAv1(format.vcodec) -> 70
                    else -> 50
                }
            }.thenBy { it.fps ?: 30 }
            .thenBy { it.tbr ?: it.vbr ?: 0.0 }
        )
    }

    private fun isCompatibleH264(vcodec: String?): Boolean {
        if (vcodec == null) return false
        return vcodec.contains("avc", ignoreCase = true) ||
               vcodec.contains("h264", ignoreCase = true) ||
               vcodec.contains("avc1", ignoreCase = true)
    }

    private fun isVp9(vcodec: String?): Boolean {
        if (vcodec == null) return false
        return vcodec.contains("vp9", ignoreCase = true) || vcodec.contains("vp09", ignoreCase = true)
    }

    private fun isAv1(vcodec: String?): Boolean {
        if (vcodec == null) return false
        return vcodec.contains("av1", ignoreCase = true) || vcodec.contains("av01", ignoreCase = true)
    }
}
