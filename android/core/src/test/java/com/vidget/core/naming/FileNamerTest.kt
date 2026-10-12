package com.vidget.core.naming

import com.vidget.domain.model.NameConflictResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNamerTest {

    @Test
    fun sanitizeBaseName_preserves_arabic_characters_and_diacritics() {
        val rawArabic = "تَحْمِيلُ فِيدْيُو رَائِعْ وَمُمَيَّزْ"
        val sanitized = FileNamer.sanitizeBaseName(rawArabic)
        assertEquals(rawArabic, sanitized)
    }

    @Test
    fun sanitizeBaseName_preserves_emojis_and_surrogate_pairs() {
        val rawWithEmoji = "فيديو رائع 🚀🔥 2026 🎉"
        val sanitized = FileNamer.sanitizeBaseName(rawWithEmoji)
        assertEquals(rawWithEmoji, sanitized)
    }

    @Test
    fun sanitizeBaseName_replaces_illegal_fat32_and_control_characters() {
        val illegalInput = "Video: How to code? <Part 1> / \"Best\" | *awesome* \\ tutorial"
        val expected = "Video_ How to code_ _Part 1_ _ _Best_ _ _awesome_ _ tutorial"
        val sanitized = FileNamer.sanitizeBaseName(illegalInput)
        assertEquals(expected, sanitized)
    }

    @Test
    fun sanitizeBaseName_removes_trailing_dots_and_spaces() {
        val input = "My Important Video....   "
        val sanitized = FileNamer.sanitizeBaseName(input)
        assertEquals("My Important Video", sanitized)
    }

    @Test
    fun sanitizeBaseName_handles_windows_reserved_names() {
        val reservedCon = "con"
        val reservedNul = "NUL"
        val reservedPrn = "Prn"
        val reservedCom1 = "COM1"

        assertEquals("_con", FileNamer.sanitizeBaseName(reservedCon))
        assertEquals("_NUL", FileNamer.sanitizeBaseName(reservedNul))
        assertEquals("_Prn", FileNamer.sanitizeBaseName(reservedPrn))
        assertEquals("_COM1", FileNamer.sanitizeBaseName(reservedCom1))
    }

    @Test
    fun sanitizeBaseName_truncates_long_names_at_120_codepoints_without_splitting_pairs() {
        val emojiString = "🔥".repeat(150)
        val sanitized = FileNamer.sanitizeBaseName(emojiString)

        // Must cap at exactly 120 code points
        assertEquals(120, sanitized.codePointCount(0, sanitized.length))
        assertEquals("🔥".repeat(120), sanitized)
    }

    @Test
    fun generateSubtitleFileName_appends_clean_language_suffix() {
        val subAr = FileNamer.generateSubtitleFileName("lecture_01", "ar")
        val subEn = FileNamer.generateSubtitleFileName("lecture_01", "EN")

        assertEquals("lecture_01.ar.srt", subAr)
        assertEquals("lecture_01.en.srt", subEn)
    }

    @Test
    fun conflictResolver_increments_numbers_properly() {
        val existingFiles = mutableSetOf("test_video.mp4", "test_video (1).mp4", "test_video (2).mp4")

        val uniqueName = ConflictResolver.resolveUniqueName(
            sanitizedBase = "test_video",
            cleanExt = "mp4",
            fileExistsCheck = { existingFiles.contains(it) }
        )

        assertEquals("test_video (3).mp4", uniqueName)
    }

    @Test
    fun templateParser_substitutes_allowed_placeholders() {
        val context = VideoTemplateContext(
            title = "أندرويد 15 للمحترفين",
            quality = "1080p",
            ext = "mp4",
            platform = "YouTube",
            date = "2026-10-12",
            id = "xyz123"
        )

        val template = "[{platform}] {title} ({quality}) - {date}.{ext}"
        val parsed = TemplateParser.parse(template, context)

        assertEquals("[YouTube] أندرويد 15 للمحترفين (1080p) - 2026-10-12.mp4", parsed)
    }

    @Test
    fun templateParser_rejects_unauthorized_placeholders() {
        assertFalse(TemplateParser.validateTemplate("{title} - {hacked_variable}"))
        assertTrue(TemplateParser.validateTemplate("{title} - {quality}.{ext}"))
    }
}
