package com.vidget.domain.parser

import com.vidget.domain.model.MediaFormat
import com.vidget.domain.model.MediaInfo
import com.vidget.domain.model.QualityKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityParserTest {

    private val sizeCalculator = SizeCalculator()
    private val qualityParser = QualityParser(sizeCalculator)

    @Test
    fun testCase1_youtube_1080p60_separate_video_and_audio() {
        // Sample 1: YouTube video with separate 1080p60 AVC1, 1080p VP9, 720p, and m4a / opus audio
        val formats = listOf(
            MediaFormat(formatId = "137", ext = "mp4", vcodec = "avc1.640028", acodec = "none", height = 1080, fps = 60, filesize = 80_000_000L),
            MediaFormat(formatId = "248", ext = "webm", vcodec = "vp09.00.41", acodec = "none", height = 1080, fps = 60, filesize = 60_000_000L),
            MediaFormat(formatId = "136", ext = "mp4", vcodec = "avc1.4d401f", acodec = "none", height = 720, fps = 30, filesize = 40_000_000L),
            MediaFormat(formatId = "140", ext = "m4a", vcodec = "none", acodec = "mp4a.40.2", abr = 128.0, filesize = 10_000_000L),
            MediaFormat(formatId = "251", ext = "webm", vcodec = "none", acodec = "opus", abr = 160.0, filesize = 12_000_000L),
            MediaFormat(formatId = "sb0", ext = "mhtml", vcodec = "none", acodec = "none", formatNote = "storyboard") // Should be excluded
        )

        val mediaInfo = MediaInfo(
            id = "vid_yt_1080",
            title = "Android Jetpack Compose Full Course",
            durationSeconds = 600,
            formats = formats
        )

        val result = qualityParser.parse(mediaInfo)

        // Must filter out storyboard
        assertEquals(2, result.videoOptions.size) // 1080p and 720p

        // 1080p option must pick AVC1 mp4 over vp9 webm due to priority
        val option1080 = result.videoOptions.first { it.height == 1080 }
        assertEquals("1080p60", option1080.label)
        assertEquals("mp4", option1080.container)
        assertEquals(QualityKind.NEEDS_MERGE, option1080.kind)
        assertTrue("Option should be marked as Best", option1080.isBest)

        // NEEDS_MERGE size must equal video (80MB) + audio (10MB) = 90MB
        assertEquals(90_000_000L, option1080.sizeBytes)
        assertEquals("137+140", option1080.formatSelector)

        // Audio options check
        assertTrue(result.audioOptions.any { it.container == "m4a" && it.formatSelector == "140" })
        assertTrue(result.audioOptions.any { it.container == "opus" && it.formatSelector == "251" })
        assertTrue(result.audioOptions.any { it.container == "mp3" && it.label.contains("320") })
    }

    @Test
    fun testCase2_combined_single_file_mp4() {
        // Sample 2: Direct or legacy MP4 format with audio already muxed (combined)
        val formats = listOf(
            MediaFormat(formatId = "22", ext = "mp4", vcodec = "avc1.64001F", acodec = "mp4a.40.2", height = 720, fps = 30, filesize = 55_000_000L),
            MediaFormat(formatId = "18", ext = "mp4", vcodec = "avc1.42001E", acodec = "mp4a.40.2", height = 360, fps = 30, filesize = 20_000_000L)
        )

        val mediaInfo = MediaInfo(
            id = "vid_combined",
            title = "Direct Combined Video",
            durationSeconds = 300,
            formats = formats
        )

        val result = qualityParser.parse(mediaInfo)

        val option720 = result.videoOptions.first { it.height == 720 }
        assertEquals(QualityKind.COMBINED, option720.kind)
        assertEquals(55_000_000L, option720.sizeBytes)
        assertEquals("22", option720.formatSelector)
        assertFalse(option720.sizeIsEstimate)
    }

    @Test
    fun testCase3_bitrate_estimated_size_when_filesize_missing() {
        // Sample 3: Video without filesize but with tbr (bitrate)
        val formats = listOf(
            MediaFormat(
                formatId = "direct_1080",
                ext = "mp4",
                vcodec = "avc1.4d4028",
                acodec = "none",
                height = 1080,
                fps = 30,
                tbr = 4000.0 // 4000 kbps
            ),
            MediaFormat(
                formatId = "audio_128",
                ext = "m4a",
                vcodec = "none",
                acodec = "mp4a.40.2",
                tbr = 128.0 // 128 kbps
            )
        )

        val mediaInfo = MediaInfo(
            id = "vid_tbr",
            title = "Video with Bitrate",
            durationSeconds = 100, // 100 seconds
            formats = formats
        )

        val result = qualityParser.parse(mediaInfo)
        val option = result.videoOptions.first { it.height == 1080 }

        // Expected size: (4000 + 128) * 100 * 1000 / 8 = 51,600,000 bytes
        assertTrue("Size should be marked as estimate", option.sizeIsEstimate)
        assertEquals(51_600_000L, option.sizeBytes)
    }

    @Test
    fun testCase4_4k_resolution_selection() {
        // Sample 4: 4K (2160p) format exists along with 1080p
        val formats = listOf(
            MediaFormat(formatId = "313", ext = "webm", vcodec = "vp09.00.51", acodec = "none", height = 2160, fps = 60, filesize = 400_000_000L),
            MediaFormat(formatId = "137", ext = "mp4", vcodec = "avc1.640028", acodec = "none", height = 1080, fps = 30, filesize = 90_000_000L),
            MediaFormat(formatId = "140", ext = "m4a", vcodec = "none", acodec = "mp4a.40.2", height = null, filesize = 10_000_000L)
        )

        val mediaInfo = MediaInfo(
            id = "vid_4k",
            title = "Nature 4K",
            durationSeconds = 120,
            formats = formats
        )

        val result = qualityParser.parse(mediaInfo)

        assertEquals(2, result.videoOptions.size)
        val option4k = result.videoOptions.first { it.height == 2160 }
        assertEquals("4K60", option4k.label)

        // Best badge rule: highest <= 1080p with compatible H264 gets Best, not 4K VP9
        val option1080 = result.videoOptions.first { it.height == 1080 }
        assertTrue("1080p should get Best badge for compatibility", option1080.isBest)
        assertFalse(option4k.isBest)
    }

    @Test
    fun testCase5_live_stream_displays_live_status_no_size() {
        // Sample 5: Live stream
        val formats = listOf(
            MediaFormat(formatId = "hls-1080", ext = "mp4", vcodec = "avc1.4d4028", acodec = "mp4a.40.2", height = 1080, fps = 30)
        )

        val mediaInfo = MediaInfo(
            id = "live_stream_01",
            title = "Live News 24/7",
            durationSeconds = 0,
            isLive = true,
            formats = formats
        )

        val result = qualityParser.parse(mediaInfo)
        val option = result.videoOptions.first()

        assertTrue(option.isLive)
        val calculated = sizeCalculator.calculateFormatSize(formats.first(), 0, true)
        assertEquals("بث مباشر", calculated.formatDisplay(isArabic = true))
        assertEquals("Live Stream", calculated.formatDisplay(isArabic = false))
    }
}
