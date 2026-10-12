package com.vidget.downloader.engine

import com.vidget.downloader.model.DownloadConfig
import com.vidget.downloader.model.DownloadPhase
import com.vidget.downloader.model.StreamType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArgsBuilderTest {

    @Test
    fun `direct HTTP MP4 uses aria2c with 8 connections by default`() {
        val config = DownloadConfig(connectionCount = 8)
        val args = ArgsBuilder.build(
            url = "https://example.com/video.mp4",
            formatSelector = "bestvideo+bestaudio/best",
            outputDirectory = "/data/user/0/com.vidget/cache/task_123",
            streamType = StreamType.DIRECT_HTTP,
            config = config
        )

        assertTrue("Should specify aria2c downloader", args.contains("--downloader"))
        val downloaderIdx = args.indexOf("--downloader")
        assertEquals("aria2c", args[downloaderIdx + 1])

        val downloaderArgsIdx = args.indexOf("--downloader-args")
        assertTrue("Should include downloader-args", downloaderArgsIdx != -1)
        val ariaArgs = args[downloaderArgsIdx + 1]
        assertTrue("Should configure 8 connections in aria2c", ariaArgs.contains("-x 8"))
        assertTrue("Should configure 8 splits in aria2c", ariaArgs.contains("-s 8"))
        assertTrue("Should have --file-allocation=none", ariaArgs.contains("--file-allocation=none"))
        assertTrue("Should include --no-mtime", args.contains("--no-mtime"))
        assertTrue("Should include --newline", args.contains("--newline"))
        assertTrue("Should include --no-playlist", args.contains("--no-playlist"))
    }

    @Test
    fun `direct HTTP fallback to native does not include aria2c`() {
        val config = DownloadConfig(connectionCount = 8)
        val args = ArgsBuilder.build(
            url = "https://example.com/video.mp4",
            formatSelector = "137+140",
            outputDirectory = "/data/user/0/com.vidget/cache/task_123",
            streamType = StreamType.DIRECT_HTTP,
            config = config,
            useAria2cFallbackToNative = true
        )

        assertFalse("Should not include aria2c when falling back to native", args.contains("--downloader"))
    }

    @Test
    fun `HLS DASH uses concurrent fragments and retries`() {
        val config = DownloadConfig(connectionCount = 8)
        val args = ArgsBuilder.build(
            url = "https://example.com/stream.m3u8",
            formatSelector = "best",
            outputDirectory = "/cache/task_hls",
            streamType = StreamType.HLS_DASH,
            config = config
        )

        assertTrue(args.contains("--concurrent-fragments"))
        val fragIdx = args.indexOf("--concurrent-fragments")
        assertEquals("8", args[fragIdx + 1])
        assertTrue(args.contains("--fragment-retries"))
    }

    @Test
    fun `applies rate limiting, cookies, proxy, and subtitles`() {
        val config = DownloadConfig(
            rateLimitBytesPerSec = 5_000_000,
            cookiesFilePath = "/sdcard/cookies.txt",
            proxyUrl = "socks5://127.0.0.1:9050",
            subtitleLanguages = listOf("ar", "en"),
            embedSubtitles = true
        )

        val args = ArgsBuilder.build(
            url = "https://youtube.com/watch?v=demo",
            formatSelector = "best",
            outputDirectory = "/cache/task_sub",
            streamType = StreamType.DIRECT_HTTP,
            config = config
        )

        val rateIdx = args.indexOf("--limit-rate")
        assertEquals("5000000", args[rateIdx + 1])

        val cookiesIdx = args.indexOf("--cookies")
        assertEquals("/sdcard/cookies.txt", args[cookiesIdx + 1])

        val proxyIdx = args.indexOf("--proxy")
        assertEquals("socks5://127.0.0.1:9050", args[proxyIdx + 1])

        assertTrue(args.contains("--write-subs"))
        assertTrue(args.contains("--embed-subs"))
        val subLangsIdx = args.indexOf("--sub-langs")
        assertEquals("ar,en", args[subLangsIdx + 1])
    }
}

class ProgressParserTest {

    @Test
    fun `parses standard yt-dlp percentage, speed, and ETA`() {
        val line = "[download]  45.2% of  120.50MiB at  12.45MiB/s ETA 00:05"
        val progress = ProgressParser.parseLine("task_1", line, 0L, DownloadPhase.DOWNLOADING_VIDEO)

        assertNotNull(progress)
        assertEquals(45.2f, progress!!.percent, 0.01f)
        assertEquals(54_713_384L, progress.downloadedBytes) // 45.2% of 120.5 MiB
        assertEquals(126_353_408L, progress.totalBytes)
        assertEquals(13_054_771L, progress.speedBytesPerSec) // ~12.45 MiB/s
        assertEquals(5L, progress.etaSec)
        assertEquals(DownloadPhase.DOWNLOADING_VIDEO, progress.phase)
    }

    @Test
    fun `parses aria2c speed and percentage output`() {
        val line = "[#abc123 60.5MiB/120.0MiB(50%) CN:8 DL:15.2MiB/s ETA:4s]"
        val progress = ProgressParser.parseLine("task_2", line, 0L, DownloadPhase.DOWNLOADING_VIDEO)

        assertNotNull(progress)
        assertEquals(50.0f, progress!!.percent, 0.01f)
        assertEquals(4L, progress.etaSec)
        assertEquals(DownloadPhase.DOWNLOADING_VIDEO, progress.phase)
    }

    @Test
    fun `detects ffmpeg merge phase`() {
        val line = "[ffmpeg] Merging formats into \"output.mp4\""
        val progress = ProgressParser.parseLine("task_3", line, 0L, DownloadPhase.DOWNLOADING_VIDEO)

        assertNotNull(progress)
        assertEquals(DownloadPhase.MERGING, progress!!.phase)
    }
}
