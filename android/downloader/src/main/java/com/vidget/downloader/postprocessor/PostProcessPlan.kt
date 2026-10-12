package com.vidget.downloader.postprocessor

import com.vidget.domain.model.QualityKind
import com.vidget.domain.model.QualityOption
import com.vidget.domain.model.SubtitleTrack

enum class TargetContainer {
    MP4,
    MKV,
    WEBM,
    MP3,
    M4A,
    OPUS
}

enum class AudioFormat {
    NONE,
    COPY,
    MP3_128K,
    MP3_192K,
    MP3_320K,
    M4A_COPY,
    OPUS
}

data class PostProcessPlan(
    val needsMerge: Boolean,
    val targetContainer: TargetContainer,
    val mergeOutputFormat: String? = null,
    val audioFormat: AudioFormat = AudioFormat.NONE,
    val embedThumbnail: Boolean = true,
    val embedMetadata: Boolean = true,
    val embedChapters: Boolean = true,
    val subtitleLangs: List<String> = emptyList(),
    val embedSubtitles: Boolean = false,
    val writeAutoSubs: Boolean = false,
    val customFfmpegArgs: List<String> = emptyList()
) {
    companion object {
        fun create(
            option: QualityOption,
            selectedSubtitles: List<SubtitleTrack> = emptyList(),
            embedSubtitlesInFile: Boolean = true,
            embedThumbnail: Boolean = true,
            embedMetadata: Boolean = true
        ): PostProcessPlan {
            val isAudioOnly = option.kind == QualityKind.AUDIO_ONLY
            val needsMerge = option.kind == QualityKind.NEEDS_MERGE

            if (isAudioOnly) {
                val container = when (option.container.lowercase()) {
                    "mp3" -> TargetContainer.MP3
                    "m4a" -> TargetContainer.M4A
                    "opus", "webm" -> TargetContainer.OPUS
                    else -> TargetContainer.MP3
                }
                val audioFmt = when {
                    option.container.equals("mp3", ignoreCase = true) -> {
                        when (option.audioBitrateKbps) {
                            128 -> AudioFormat.MP3_128K
                            192 -> AudioFormat.MP3_192K
                            320 -> AudioFormat.MP3_320K
                            else -> AudioFormat.MP3_192K
                        }
                    }
                    option.container.equals("m4a", ignoreCase = true) -> AudioFormat.M4A_COPY
                    else -> AudioFormat.COPY
                }
                return PostProcessPlan(
                    needsMerge = false,
                    targetContainer = container,
                    audioFormat = audioFmt,
                    embedThumbnail = embedThumbnail,
                    embedMetadata = embedMetadata,
                    embedChapters = false,
                    subtitleLangs = emptyList(),
                    embedSubtitles = false
                )
            }

            // Video options: determine container
            // If VP9/AV1 with Opus, MP4 is often incompatible or buggy without re-encoding, so MKV is selected automatically
            val vcodec = option.vcodec?.lowercase() ?: ""
            val acodec = option.acodec?.lowercase() ?: ""
            val isVp9OrAv1 = vcodec.contains("vp9") || vcodec.contains("vp09") || vcodec.contains("av01") || vcodec.contains("av1")
            val isOpus = acodec.contains("opus")

            val targetContainer = if (isVp9OrAv1 && isOpus) {
                TargetContainer.MKV
            } else if (option.container.equals("webm", ignoreCase = true)) {
                TargetContainer.WEBM
            } else {
                TargetContainer.MP4
            }

            val subCodes = selectedSubtitles.map { it.language }
            val hasAutoSubs = selectedSubtitles.any { it.isAutomatic }

            return PostProcessPlan(
                needsMerge = needsMerge,
                targetContainer = targetContainer,
                mergeOutputFormat = if (needsMerge) targetContainer.name.lowercase() else null,
                audioFormat = AudioFormat.NONE,
                embedThumbnail = embedThumbnail,
                embedMetadata = embedMetadata,
                embedChapters = true,
                subtitleLangs = subCodes,
                embedSubtitles = embedSubtitlesInFile && subCodes.isNotEmpty(),
                writeAutoSubs = hasAutoSubs
            )
        }
    }
}
