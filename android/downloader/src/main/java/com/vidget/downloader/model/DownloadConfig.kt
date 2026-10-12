package com.vidget.downloader.model

import kotlinx.serialization.Serializable

@Serializable
enum class StreamType {
    DIRECT_HTTP,
    HLS_DASH,
    FALLBACK_HTML
}

@Serializable
data class DownloadConfig(
    val connectionCount: Int = 8, // 1 to 16, default 8
    val rateLimitBytesPerSec: Long = 0L, // 0 = unlimited
    val cookiesFilePath: String? = null,
    val proxyUrl: String? = null,
    val userAgent: String? = null,
    val extraHeaders: Map<String, String> = emptyMap(),
    val embedSubtitles: Boolean = false,
    val subtitleLanguages: List<String> = emptyList()
)
