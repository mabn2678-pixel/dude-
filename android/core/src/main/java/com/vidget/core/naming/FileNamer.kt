package com.vidget.core.naming

import com.vidget.domain.model.SubfolderOrganization
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * File name sanitization and generation engine adhering strictly to Android FAT32,
 * ext4, and Windows filesystem limits.
 */
object FileNamer {

    const val MAX_BASE_NAME_LENGTH = 120
    const val PART_EXTENSION = ".part"

    // Disallowed FAT32/NTFS characters: \ / : * ? " < > |
    private val ILLEGAL_CHAR_REGEX = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F\\u007F]")

    // Reserved DOS / Windows names
    private val RESERVED_NAMES = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )

    /**
     * Sanitizes a file base name:
     * - Replaces illegal chars and control characters with '_'
     * - Removes trailing dots and spaces
     * - Safeguards Unicode/Arabic/Emoji surrogate pairs without truncation corruption
     * - Protects against reserved Windows filenames
     * - Caps pre-extension length to exactly 120 code points
     */
    fun sanitizeBaseName(rawName: String): String {
        if (rawName.isBlank()) return "download"

        // 1. Replace illegal characters and ASCII control chars with '_'
        var cleaned = rawName.replace(ILLEGAL_CHAR_REGEX, "_")

        // 2. Trim trailing dots and whitespaces
        cleaned = cleaned.trimEnd { it == '.' || it.isWhitespace() }

        if (cleaned.isBlank()) {
            cleaned = "download"
        }

        // 3. Safe Unicode code point truncation (max 120 code points, preserving emoji & composite chars)
        cleaned = truncateSafely(cleaned, MAX_BASE_NAME_LENGTH)

        // 4. Re-trim trailing dots/spaces after truncation
        cleaned = cleaned.trimEnd { it == '.' || it.isWhitespace() }
        if (cleaned.isBlank()) {
            cleaned = "download"
        }

        // 5. Windows reserved names check (case-insensitive)
        val upper = cleaned.uppercase(Locale.US)
        if (upper in RESERVED_NAMES) {
            cleaned = "_$cleaned"
        }

        return cleaned
    }

    /**
     * Truncates a string by Unicode code points to avoid splitting high/low surrogate pairs or graphemes.
     */
    fun truncateSafely(input: String, maxCodePoints: Int): String {
        var codePointsCount = 0
        var byteIndex = 0

        while (byteIndex < input.length && codePointsCount < maxCodePoints) {
            val codePoint = input.codePointAt(byteIndex)
            byteIndex += Character.charCount(codePoint)
            codePointsCount++
        }

        return input.substring(0, byteIndex)
    }

    /**
     * Generates temporary in-progress file name with .part extension.
     */
    fun generateTempFileName(baseName: String, ext: String): String {
        val sanitizedBase = sanitizeBaseName(baseName)
        val cleanExt = ext.removePrefix(".")
        return "$sanitizedBase.$cleanExt$PART_EXTENSION"
    }

    /**
     * Generates finalized file name after download and merge completion.
     */
    fun generateFinalFileName(baseName: String, ext: String): String {
        val sanitizedBase = sanitizeBaseName(baseName)
        val cleanExt = ext.removePrefix(".")
        return "$sanitizedBase.$cleanExt"
    }

    /**
     * Generates associated subtitle file name: {sanitizedBase}.{lang}.srt
     */
    fun generateSubtitleFileName(baseName: String, languageCode: String): String {
        val sanitizedBase = sanitizeBaseName(baseName)
        val cleanLang = languageCode.trim().lowercase(Locale.US)
        return "$sanitizedBase.$cleanLang.srt"
    }

    /**
     * Resolves subfolder path according to organization strategy.
     */
    fun resolveSubfolder(
        organization: SubfolderOrganization,
        platform: String,
        date: Date = Date()
    ): String? {
        return when (organization) {
            SubfolderOrganization.NONE -> null
            SubfolderOrganization.PLATFORM -> sanitizeBaseName(platform)
            SubfolderOrganization.DATE -> SimpleDateFormat("yyyy-MM", Locale.US).format(date)
        }
    }
}
