package com.vidget.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class QualityKind {
    COMBINED,
    NEEDS_MERGE,
    AUDIO_ONLY
}

@Serializable
data class QualityOption(
    val id: String,
    val label: String, // e.g. "1080p60", "720p", "320 kbps"
    val height: Int, // e.g. 1080, 0 for audio
    val fps: Int = 30,
    val container: String, // e.g. "mp4", "webm", "m4a", "opus", "mp3"
    val vcodec: String? = null, // e.g. "avc1.640028", "vp09", "av01"
    val acodec: String? = null, // e.g. "mp4a.40.2", "opus"
    val sizeBytes: Long = 0,
    val sizeIsEstimate: Boolean = false,
    val isUnknownSize: Boolean = false,
    val isLive: Boolean = false,
    val kind: QualityKind,
    val isBest: Boolean = false,
    val formatSelector: String, // yt-dlp syntax, e.g. "137+140" or "bestvideo[height<=1080]+bestaudio"
    val videoFormat: MediaFormat? = null,
    val audioFormat: MediaFormat? = null,
    val audioBitrateKbps: Int? = null
) {
    val displayCodec: String
        get() = when {
            vcodec?.contains("avc", ignoreCase = true) == true || vcodec?.contains("h264", ignoreCase = true) == true -> "H.264"
            vcodec?.contains("vp9", ignoreCase = true) == true || vcodec?.contains("vp09", ignoreCase = true) == true -> "VP9"
            vcodec?.contains("av01", ignoreCase = true) == true || vcodec?.contains("av1", ignoreCase = true) == true -> "AV1"
            vcodec?.contains("hevc", ignoreCase = true) == true || vcodec?.contains("h265", ignoreCase = true) == true -> "H.265"
            vcodec != null -> vcodec.take(8)
            acodec != null -> acodec.take(8)
            else -> container.uppercase()
        }

    val displayContainer: String
        get() = container.uppercase()
}
