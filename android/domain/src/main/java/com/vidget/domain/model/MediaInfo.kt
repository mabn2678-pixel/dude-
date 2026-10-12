package com.vidget.domain.model

import kotlinx.serialization.Serializable

/**
 * Raw format metadata parsed from yt-dlp dump-json or direct extraction.
 */
@Serializable
data class MediaFormat(
    val formatId: String,
    val ext: String = "mp4",
    val url: String? = null,
    val vcodec: String? = null,
    val acodec: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val fps: Int? = null,
    val tbr: Double? = null, // Total bitrate kbps
    val abr: Double? = null, // Audio bitrate kbps
    val vbr: Double? = null, // Video bitrate kbps
    val filesize: Long? = null,
    val filesizeApprox: Long? = null,
    val protocol: String? = null,
    val formatNote: String? = null,
    val httpHeaders: Map<String, String> = emptyMap()
) {
    val isVideo: Boolean
        get() = vcodec != null && vcodec != "none"

    val isAudio: Boolean
        get() = acodec != null && acodec != "none"

    val isCombined: Boolean
        get() = isVideo && isAudio

    val isVideoOnly: Boolean
        get() = isVideo && !isAudio

    val isAudioOnly: Boolean
        get() = !isVideo && isAudio

    val isStoryboard: Boolean
        get() = (vcodec == "none" && acodec == "none") ||
                ext == "mhtml" ||
                formatNote?.contains("storyboard", ignoreCase = true) == true
}

@Serializable
data class SubtitleTrack(
    val language: String,
    val name: String,
    val ext: String = "srt",
    val isAutomatic: Boolean = false,
    val url: String? = null
)

@Serializable
data class PlaylistItem(
    val id: String,
    val title: String,
    val durationSeconds: Long = 0,
    val thumbnail: String? = null,
    val url: String = ""
)

@Serializable
data class MediaInfo(
    val id: String,
    val title: String,
    val description: String = "",
    val durationSeconds: Long = 0,
    val thumbnail: String? = null,
    val uploader: String = "",
    val extractor: String = "",
    val webpageUrl: String = "",
    val isLive: Boolean = false,
    val isPlaylist: Boolean = false,
    val playlistItems: List<PlaylistItem> = emptyList(),
    val formats: List<MediaFormat> = emptyList(),
    val subtitles: List<SubtitleTrack> = emptyList()
)
