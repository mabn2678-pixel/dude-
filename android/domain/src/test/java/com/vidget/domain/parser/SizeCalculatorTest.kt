package com.vidget.domain.parser

import com.vidget.domain.model.MediaFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SizeCalculatorTest {

    private val calculator = SizeCalculator()

    @Test
    fun `filesize exact takes highest precedence`() {
        val format = MediaFormat(
            formatId = "1",
            filesize = 104_857_600L, // 100 MB
            filesizeApprox = 120_000_000L,
            tbr = 5000.0
        )
        val result = calculator.calculateFormatSize(format, durationSeconds = 60, isLive = false)

        assertEquals(104_857_600L, result.bytes)
        assertFalse(result.isEstimate)
        assertEquals("100.00 MB", result.formatDisplay(isArabic = false))
        assertEquals("100.00 م.ب", result.formatDisplay(isArabic = true))
    }

    @Test
    fun `filesize approx used when exact is null`() {
        val format = MediaFormat(
            formatId = "2",
            filesize = null,
            filesizeApprox = 52_428_800L, // 50 MB approx
            tbr = 4000.0
        )
        val result = calculator.calculateFormatSize(format, durationSeconds = 60, isLive = false)

        assertEquals(52_428_800L, result.bytes)
        assertTrue(result.isEstimate)
        assertEquals("~50.00 MB", result.formatDisplay(isArabic = false))
        assertEquals("~50.00 م.ب", result.formatDisplay(isArabic = true))
    }

    @Test
    fun `bitrate formula used when filesizes missing`() {
        // (tbr * duration * 1000 / 8) -> 2000 kbps * 300s * 1000 / 8 = 75,000,000 bytes
        val format = MediaFormat(
            formatId = "3",
            tbr = 2000.0
        )
        val result = calculator.calculateFormatSize(format, durationSeconds = 300, isLive = false)

        assertEquals(75_000_000L, result.bytes)
        assertTrue(result.isEstimate)
        assertEquals("~71.53 MB", result.formatDisplay(isArabic = false))
    }

    @Test
    fun `live stream displays live message without zero`() {
        val format = MediaFormat(formatId = "live")
        val result = calculator.calculateFormatSize(format, durationSeconds = 0, isLive = true)

        assertTrue(result.isLive)
        assertEquals("Live Stream", result.formatDisplay(isArabic = false))
        assertEquals("بث مباشر", result.formatDisplay(isArabic = true))
    }

    @Test
    fun `unknown size displays unknown never zero`() {
        val format = MediaFormat(formatId = "empty")
        val result = calculator.calculateFormatSize(format, durationSeconds = 0, isLive = false)

        assertTrue(result.isUnknown)
        assertEquals("Unknown size", result.formatDisplay(isArabic = false))
        assertEquals("حجم غير معروف", result.formatDisplay(isArabic = true))
    }
}
