package com.vidget.downloader.engine

import com.vidget.downloader.model.DownloadConfig
import com.vidget.downloader.model.StreamType

/**
 * Pure, testable builder for yt-dlp arguments across stream types.
 */
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

        // 1. Target URL
        args.add(url)

        // 2. Format selector
        args.add("-f")
        args.add(formatSelector)

        // 3. Output template inside task temp directory
        args.add("-o")
        val cleanDir = outputDirectory.trimEnd('/', '\\')
        args.add("$cleanDir/%(id)s.%(ext)s")

        // 4. Standard VidGet operational flags
        args.add("--no-mtime")
        args.add("--newline")

        if (!isPlaylist) {
            args.add("--no-playlist")
        }

        // 5. Engine-specific multi-connection arguments
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
                // Pass extra headers for custom direct extractors
                for ((key, value) in config.extraHeaders) {
                    args.add("--add-header")
                    args.add("$key:$value")
                }
            }
        }

        // 6. User-Agent
        if (!config.userAgent.isNullOrBlank()) {
            args.add("--user-agent")
            args.add(config.userAgent)
        }

        // 7. Cookies
        if (!config.cookiesFilePath.isNullOrBlank()) {
            args.add("--cookies")
            args.add(config.cookiesFilePath)
        }

        // 8. Proxy
        if (!config.proxyUrl.isNullOrBlank()) {
            args.add("--proxy")
            args.add(config.proxyUrl)
        }

        // 9. Speed rate limit (0 = unlimited)
        if (config.rateLimitBytesPerSec > 0) {
            args.add("--limit-rate")
            args.add("${config.rateLimitBytesPerSec}")
        }

        // 10. Subtitles
        if (config.subtitleLanguages.isNotEmpty()) {
            args.add("--write-subs")
            args.add("--sub-langs")
            args.add(config.subtitleLanguages.joinToString(","))

            if (config.embedSubtitles) {
                args.add("--embed-subs")
            }
        }

        return args
    }
}
