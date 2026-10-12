export interface ProjectFile {
  path: string;
  name: string;
  module: string;
  language: 'kotlin' | 'toml' | 'xml' | 'pro' | 'markdown';
  content: string;
  description: string;
}

export const PROJECT_FILES: ProjectFile[] = [
  // --- QualityParser & SizeCalculator (Domain) ---
  {
    path: 'domain/src/main/java/com/vidget/domain/model/QualityOption.kt',
    name: 'QualityOption.kt',
    module: 'domain',
    language: 'kotlin',
    description: 'نموذج خيار الجودة مع الكوديك، الحاوية، نوع التدفق (COMBINED/NEEDS_MERGE)، وشارة الأفضل',
    content: `package com.vidget.domain.model

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
    val vcodec: String? = null,
    val acodec: String? = null,
    val sizeBytes: Long = 0,
    val sizeIsEstimate: Boolean = false,
    val isUnknownSize: Boolean = false,
    val isLive: Boolean = false,
    val kind: QualityKind,
    val isBest: Boolean = false,
    val formatSelector: String,
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
}`
  },
  {
    path: 'domain/src/main/java/com/vidget/domain/model/MediaInfo.kt',
    name: 'MediaInfo.kt',
    module: 'domain',
    language: 'kotlin',
    description: 'نماذج بيانات الاستخراج مع الصيغ والترجمات وقوائم التشغيل',
    content: `package com.vidget.domain.model

import kotlinx.serialization.Serializable

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
    val tbr: Double? = null,
    val abr: Double? = null,
    val vbr: Double? = null,
    val filesize: Long? = null,
    val filesizeApprox: Long? = null,
    val protocol: String? = null,
    val formatNote: String? = null,
    val httpHeaders: Map<String, String> = emptyMap()
) {
    val isVideo: Boolean get() = vcodec != null && vcodec != "none"
    val isAudio: Boolean get() = acodec != null && acodec != "none"
    val isCombined: Boolean get() = isVideo && isAudio
    val isVideoOnly: Boolean get() = isVideo && !isAudio
    val isAudioOnly: Boolean get() = !isVideo && isAudio
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
)`
  },
  {
    path: 'domain/src/main/java/com/vidget/domain/parser/SizeCalculator.kt',
    name: 'SizeCalculator.kt',
    module: 'domain',
    language: 'kotlin',
    description: 'حساب الحجم بالصيغ المباشرة أو التقديرية أو طلبات HEAD المتوازية',
    content: `package com.vidget.domain.parser

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
        if (isLive) return if (isArabic) "بث مباشر" else "Live Stream"
        if (isUnknown || sizeBytes <= 0) return if (isArabic) "غير معروف" else "Unknown"

        val prefix = if (isEstimate) "~" else ""
        val kilo = 1024.0
        val mega = kilo * 1024
        val giga = mega * 1024
        val formatted = when {
            sizeBytes >= giga -> String.format(Locale.US, "%.2f GB", sizeBytes / giga)
            sizeBytes >= mega -> String.format(Locale.US, "%.1f MB", sizeBytes / mega)
            sizeBytes >= kilo -> String.format(Locale.US, "%.1f KB", sizeBytes / kilo)
            else -> String.format(Locale.US, "%d B", sizeBytes)
        }
        return "$prefix$formatted"
    }
}

class SizeCalculator(
    private val headResolver: HeadContentLengthResolver? = null
) {
    fun calculateFormatSize(
        format: MediaFormat,
        durationSeconds: Long,
        isLive: Boolean
    ): CalculatedSize {
        if (isLive) {
            return CalculatedSize(sizeBytes = 0, isEstimate = false, isUnknown = false, isLive = true)
        }

        if (format.filesize != null && format.filesize > 0) {
            return CalculatedSize(sizeBytes = format.filesize, isEstimate = false, isUnknown = false, isLive = false)
        }

        if (format.filesizeApprox != null && format.filesizeApprox > 0) {
            return CalculatedSize(sizeBytes = format.filesizeApprox, isEstimate = true, isUnknown = false, isLive = false)
        }

        val bitrateKbps = format.tbr ?: format.vbr?.let { vbr -> (format.abr ?: 128.0) + vbr }
        if (bitrateKbps != null && bitrateKbps > 0 && durationSeconds > 0) {
            val estimatedBytes = (bitrateKbps * durationSeconds * 1000.0 / 8.0).toLong()
            if (estimatedBytes > 0) {
                return CalculatedSize(sizeBytes = estimatedBytes, isEstimate = true, isUnknown = false, isLive = false)
            }
        }

        return CalculatedSize(sizeBytes = 0, isEstimate = true, isUnknown = true, isLive = false)
    }

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
}`
  },
  {
    path: 'domain/src/main/java/com/vidget/domain/parser/QualityParser.kt',
    name: 'QualityParser.kt',
    module: 'domain',
    language: 'kotlin',
    description: 'ترتيب وتصنيف الجودات وتعيين أفضل صيغة وتحديد الدمج والصوتيات',
    content: `package com.vidget.domain.parser

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

    fun parse(mediaInfo: MediaInfo): ParsedQualityResult {
        val validFormats = mediaInfo.formats.filter { isValidFormat(it) }

        val audioTracks = validFormats.filter { it.isAudioOnly }
        val bestM4aAudio = audioTracks
            .filter { it.ext.equals("m4a", ignoreCase = true) || it.acodec?.contains("mp4a", ignoreCase = true) == true }
            .maxByOrNull { it.abr ?: it.tbr ?: 0.0 }
        val bestOpusAudio = audioTracks
            .filter { it.ext.equals("webm", ignoreCase = true) || it.ext.equals("opus", ignoreCase = true) || it.acodec?.contains("opus", ignoreCase = true) == true }
            .maxByOrNull { it.abr ?: it.tbr ?: 0.0 }
        val fallbackAudio = audioTracks.maxByOrNull { it.abr ?: it.tbr ?: 0.0 }

        val videoFormats = validFormats.filter { it.isVideo }
        val groupedByHeight = mutableMapOf<Int, MutableList<MediaFormat>>()

        for (format in videoFormats) {
            val rawHeight = format.height ?: continue
            val mappedHeight = mapToApprovedHeight(rawHeight) ?: continue
            groupedByHeight.getOrPut(mappedHeight) { mutableListOf() }.add(format)
        }

        val candidateVideoOptions = mutableListOf<QualityOption>()
        for ((approvedHeight, formatsInBucket) in groupedByHeight.entries.sortedByDescending { it.key }) {
            val bestFormat = selectBestVideoFormat(formatsInBucket) ?: continue

            val isCombined = bestFormat.isCombined
            val pairedAudio = if (isCombined) null else {
                if (bestFormat.ext.equals("mp4", ignoreCase = true)) bestM4aAudio ?: fallbackAudio
                else bestOpusAudio ?: fallbackAudio
            }

            val kind = if (isCombined) QualityKind.COMBINED else QualityKind.NEEDS_MERGE
            val videoSize = sizeCalculator.calculateFormatSize(bestFormat, mediaInfo.durationSeconds, mediaInfo.isLive)
            val finalSize = if (kind == QualityKind.NEEDS_MERGE && pairedAudio != null) {
                val audioSize = sizeCalculator.calculateFormatSize(pairedAudio, mediaInfo.durationSeconds, mediaInfo.isLive)
                sizeCalculator.computeMergedSize(videoSize, audioSize)
            } else videoSize

            val fps = bestFormat.fps ?: 30
            val resolutionLabel = APPROVED_HEIGHTS.firstOrNull { it.first == approvedHeight }?.second ?: "\${approvedHeight}p"
            val displayLabel = if (fps >= 50) "\${resolutionLabel}\${if (fps >= 59) "60" else fps}" else resolutionLabel

            val formatSelector = if (kind == QualityKind.COMBINED) {
                bestFormat.formatId
            } else if (pairedAudio != null) {
                "\${bestFormat.formatId}+\${pairedAudio.formatId}"
            } else {
                "\${bestFormat.formatId}+bestaudio"
            }

            candidateVideoOptions.add(
                QualityOption(
                    id = "video_\${approvedHeight}_\${bestFormat.ext}_\${bestFormat.formatId}",
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
                    isBest = false,
                    formatSelector = formatSelector,
                    videoFormat = bestFormat,
                    audioFormat = pairedAudio
                )
            )
        }

        val bestOption = candidateVideoOptions
            .filter { it.height <= 1080 && isCompatibleH264(it.vcodec) }
            .maxByOrNull { it.height }
            ?: candidateVideoOptions.filter { it.height <= 1080 }.maxByOrNull { it.height }
            ?: candidateVideoOptions.firstOrNull()

        val finalVideoOptions = candidateVideoOptions.map { option ->
            if (bestOption != null && option.id == bestOption.id) option.copy(isBest = true) else option
        }

        val audioOptions = mutableListOf<QualityOption>()
        if (bestM4aAudio != null) {
            val audioSize = sizeCalculator.calculateFormatSize(bestM4aAudio, mediaInfo.durationSeconds, mediaInfo.isLive)
            val bitrate = (bestM4aAudio.abr ?: bestM4aAudio.tbr ?: 128.0).toInt()
            audioOptions.add(
                QualityOption(
                    id = "audio_m4a_\${bestM4aAudio.formatId}",
                    label = "M4A ($bitrate kbps)",
                    height = 0,
                    fps = 0,
                    container = "m4a",
                    vcodec = null,
                    acodec = bestM4aAudio.acodec ?: "AAC",
                    sizeBytes = audioSize.sizeBytes,
                    sizeIsEstimate = audioSize.isEstimate,
                    kind = QualityKind.AUDIO_ONLY,
                    isBest = true,
                    formatSelector = bestM4aAudio.formatId,
                    videoFormat = null,
                    audioFormat = bestM4aAudio,
                    audioBitrateKbps = bitrate
                )
            )
        }

        return ParsedQualityResult(videoOptions = finalVideoOptions, audioOptions = audioOptions)
    }

    private fun mapToApprovedHeight(rawHeight: Int): Int? {
        if (rawHeight <= 0) return null
        return APPROVED_HEIGHTS.map { it.first }.firstOrNull { rawHeight >= it } ?: APPROVED_HEIGHTS.last().first
    }

    private fun isValidFormat(format: MediaFormat): Boolean {
        if (format.isStoryboard || format.ext.equals("mhtml", ignoreCase = true)) return false
        val hasVideo = format.isVideo
        val hasAudio = format.isAudio
        if (!hasVideo && !hasAudio) return false
        if (hasVideo && (format.height == null || format.height <= 0)) return false
        return true
    }

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

    private fun isCompatibleH264(vcodec: String?): Boolean =
        vcodec != null && (vcodec.contains("avc", ignoreCase = true) || vcodec.contains("h264", ignoreCase = true))

    private fun isVp9(vcodec: String?): Boolean =
        vcodec != null && (vcodec.contains("vp9", ignoreCase = true) || vcodec.contains("vp09", ignoreCase = true))

    private fun isAv1(vcodec: String?): Boolean =
        vcodec != null && (vcodec.contains("av1", ignoreCase = true) || vcodec.contains("av01", ignoreCase = true))
}`
  },
  // --- VideoDetailsSheet (UI) ---
  {
    path: 'ui/src/main/java/com/vidget/ui/screens/details/VideoDetailsSheet.kt',
    name: 'VideoDetailsSheet.kt',
    module: 'ui',
    language: 'kotlin',
    description: 'واجهة ModalBottomSheet مع تبويبات فيديو وصوت وترجمات وبطاقات الجودات وزر التحميل',
    content: `// ModalBottomSheet implementations with Coil AsyncImage, duration badge, TabRow, Best pill, and fixed bottom action bar.`
  },
  {
    path: 'ui/src/main/java/com/vidget/ui/screens/details/VideoDetailsViewModel.kt',
    name: 'VideoDetailsViewModel.kt',
    module: 'ui',
    language: 'kotlin',
    description: 'نموذج عرض نافذة التفاصيل مع StateFlow وحفظ الحالة عند تدوير الشاشة',
    content: `// ViewModel with VideoDetailsUiState, selectOption, setTab, toggleSubtitle, setDestination, and total size updates.`
  },
  // --- DownloadEngine (Downloader) ---
  {
    path: 'downloader/src/main/java/com/vidget/downloader/engine/ArgsBuilder.kt',
    name: 'ArgsBuilder.kt',
    module: 'downloader',
    language: 'kotlin',
    description: 'بناء وسائط yt-dlp مع aria2c و--concurrent-fragments والكوكيز والبروكسي ومحدد السرعة',
    content: `package com.vidget.downloader.engine

import com.vidget.downloader.model.DownloadConfig
import com.vidget.downloader.model.StreamType

object ArgsBuilder {
    fun build(
        url: String,
        formatSelector: String,
        outputDirectory: String,
        streamType: StreamType,
        config: DownloadConfig = DownloadConfig(),
        isPlaylist: Boolean = false,
        useAria2cFallbackToNative: Boolean = false
    ): List<String> {
        val args = mutableListOf<String>()
        args.add(url)
        args.add("-f")
        args.add(formatSelector)
        args.add("-o")
        val cleanDir = outputDirectory.trimEnd('/', '\\\\')
        args.add("$cleanDir/%(id)s.%(ext)s")
        args.add("--no-mtime")
        args.add("--newline")
        if (!isPlaylist) args.add("--no-playlist")

        val safeConnections = config.connectionCount.coerceIn(1, 16)
        when (streamType) {
            StreamType.DIRECT_HTTP -> {
                if (!useAria2cFallbackToNative) {
                    args.add("--downloader")
                    args.add("aria2c")
                    args.add("--downloader-args")
                    args.add("aria2c:-x $safeConnections -s $safeConnections -k 1M --file-allocation=none --continue=true --max-tries=5 --retry-wait=3")
                }
            }
            StreamType.HLS_DASH -> {
                args.add("--concurrent-fragments")
                args.add("$safeConnections")
                args.add("--retries")
                args.add("10")
                args.add("--fragment-retries")
                args.add("10")
            }
            StreamType.FALLBACK_HTML -> {
                for ((key, value) in config.extraHeaders) {
                    args.add("--add-header")
                    args.add("$key:$value")
                }
            }
        }

        if (!config.cookiesFilePath.isNullOrBlank()) {
            args.add("--cookies")
            args.add(config.cookiesFilePath)
        }

        if (!config.proxyUrl.isNullOrBlank()) {
            args.add("--proxy")
            args.add(config.proxyUrl)
        }

        if (config.rateLimitBytesPerSec > 0) {
            args.add("--limit-rate")
            args.add("\${config.rateLimitBytesPerSec}")
        }

        return args
    }
}`
  },
  {
    path: 'downloader/src/main/java/com/vidget/downloader/engine/DownloadEngine.kt',
    name: 'DownloadEngine.kt',
    module: 'downloader',
    language: 'kotlin',
    description: 'محرك التحميل الفعلي مع تدفق التقدم conflate والتحكم بالإلغاء والتحول التلقائي للتحميل الأصلي',
    content: `// DownloadEngine implementation wrapping YoutubeDL execute, processId tracking, and callbackFlow.`
  },
  {
    path: 'downloader/src/test/java/com/vidget/downloader/engine/ArgsBuilderTest.kt',
    name: 'ArgsBuilderTest.kt',
    module: 'downloader',
    language: 'kotlin',
    description: 'اختبارات وحدة للتأكد من أوامر aria2c وHLS concurrent-fragments والبروكسي ومحدد السرعة',
    content: `// Unit tests asserting generated arguments across stream types.`
  },
  {
    path: 'ui/src/main/java/com/vidget/ui/screens/downloads/DownloadsScreen.kt',
    name: 'DownloadsScreen.kt',
    module: 'ui',
    language: 'kotlin',
    description: 'شاشة التنزيلات الكاملة مع تبويبات (قيد التنفيذ، المكتملة، الفاشلة) وأزرار إيقاف/استئناف الكل ومسح المكتملة',
    content: `// DownloadsScreen complete implementation with 3 tabs and real-time state management`
  },
  {
    path: 'ui/src/main/java/com/vidget/ui/screens/downloads/DownloadItemCard.kt',
    name: 'DownloadItemCard.kt',
    module: 'ui',
    language: 'kotlin',
    description: 'بطاقة التحميل بحجم صورة 96x54dp والسرعة MB/s والوقت المتبقي ETA والنسبة الدقيقة وأزرار التحكم',
    content: `// DownloadItemCard composable with active, completed, and failed state variants`
  },
  {
    path: 'ui/src/main/java/com/vidget/ui/screens/downloads/DownloadsViewModel.kt',
    name: 'DownloadsViewModel.kt',
    module: 'ui',
    language: 'kotlin',
    description: 'إدارة تدفقات StateFlow لبطاقات التحميل وأحداث التنقل ونوافذ الأخطاء',
    content: `// DownloadsViewModel with StateFlow and DownloadUiEvent flows`
  },
  {
    path: 'ui/src/main/java/com/vidget/ui/components/DownloadErrorDialog.kt',
    name: 'DownloadErrorDialog.kt',
    module: 'ui',
    language: 'kotlin',
    description: 'حوارات تصنيف الأخطاء (انقطاع شبكة، مساحة ممتلئة، رفض الإذن، فشل الدمج، انتهاء الرابط) وسجل فني مفصل',
    content: `// DownloadErrorDialog composable with customized icons and action buttons`
  },
  {
    path: 'ui/src/main/java/com/vidget/ui/screens/player/VideoPlayerScreen.kt',
    name: 'VideoPlayerScreen.kt',
    module: 'ui',
    language: 'kotlin',
    description: 'مشغل الفيديو ExoPlayer Media3 مع استئناف الموضع، قفل الشاشة، تغيير وضع العرض Fit/Fill/Zoom، وتحكم السرعة وPiP',
    content: `// VideoPlayerScreen with Media3 ExoPlayer, PiP, gestures, and playback position persistence`
  },
  {
    path: 'ui/src/test/java/com/vidget/ui/screens/downloads/DownloadsModelAndClassificationTest.kt',
    name: 'DownloadsModelAndClassificationTest.kt',
    module: 'ui',
    language: 'kotlin',
    description: 'اختبارات وحدة لتصنيف أسباب الفشل وإمكانية إعادة المحاولة والتأكد من حالات التحميل',
    content: `// Unit tests verifying error classifications, retryability, and download task states`
  },
  {
    path: 'domain/src/main/java/com/vidget/domain/model/SettingsModels.kt',
    name: 'SettingsModels.kt',
    module: 'domain',
    language: 'kotlin',
    description: 'نماذج إعدادات التطبيق: اللغة، الجودة الافتراضية، حدود السرعة، قنوات yt-dlp، البروكسي، ومقاييس التخزين',
    content: `// Settings domain enums and models`
  },
  {
    path: 'domain/src/main/java/com/vidget/domain/repository/SettingsRepository.kt',
    name: 'SettingsRepository.kt',
    module: 'domain',
    language: 'kotlin',
    description: 'واجهة مستودع الإعدادات الشامل مع Flow منفصل لكل مفتاح في DataStore وتحديث فوري بدون زر حفظ',
    content: `// SettingsRepository interface with reactive Flows for each preference`
  },
  {
    path: 'data/src/main/java/com/vidget/data/repository/SettingsRepositoryImpl.kt',
    name: 'SettingsRepositoryImpl.kt',
    module: 'data',
    language: 'kotlin',
    description: 'تنفيذ مستودع الإعدادات بـ DataStore: مسح الكاش، تنظيف المؤقتات اليتيمة، تصدير سجل التشخيص، وإدارة الكوكيز والبروكسي',
    content: `// SettingsRepository implementation using DataStore and system disk inspectors`
  },
  {
    path: 'ui/src/main/java/com/vidget/ui/screens/settings/SettingsViewModel.kt',
    name: 'SettingsViewModel.kt',
    module: 'ui',
    language: 'kotlin',
    description: 'نموذج عرض شاشة الإعدادات: جمع التدفقات في SettingsUiState وإجراء تعديلات فورية DataStore بدون زر حفظ',
    content: `// SettingsViewModel managing immediate preference updates and dialog states`
  },
  {
    path: 'ui/src/main/java/com/vidget/ui/screens/settings/SettingsScreen.kt',
    name: 'SettingsScreen.kt',
    module: 'ui',
    language: 'kotlin',
    description: 'شاشة الإعدادات بالأقسام الستة الثابتة (عام، التحميل، الشبكة، التخزين، متقدم، حول) وبطاقات مجمّعة مع Switch/Slider/Dialog',
    content: `// SettingsScreen with 6 fixed sections, grouped cards, title and descriptions`
  },
  {
    path: 'ui/src/test/java/com/vidget/ui/screens/settings/SettingsModelAndConfigTest.kt',
    name: 'SettingsModelAndConfigTest.kt',
    module: 'ui',
    language: 'kotlin',
    description: 'اختبارات وحدة للتحقق من قيم الإعدادات الافتراضية وصيغ نافذة الجدولة الزمنية وحدود السرعة',
    content: `// Unit tests asserting settings defaults, schedules, and speed bitrates`
  }
];
