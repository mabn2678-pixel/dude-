package com.vidget.core.naming

import com.vidget.domain.model.NamingTemplateConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VideoTemplateContext(
    val title: String,
    val quality: String,
    val ext: String,
    val platform: String,
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val id: String
)

/**
 * Parses and substitutes placeholders into custom file naming templates.
 * Enforces strict restriction to allowed variables:
 * {title}, {quality}, {ext}, {platform}, {date}, {id}.
 */
object TemplateParser {

    /**
     * Standard dummy context used for live settings preview.
     */
    val SAMPLE_PREVIEW_CONTEXT = VideoTemplateContext(
        title = "دورة احتراف أندرويد الحديثة",
        quality = "1080p",
        ext = "mp4",
        platform = "YouTube",
        date = "2026-10-12",
        id = "vid_893274"
    )

    /**
     * Validates whether template only contains allowed tokens.
     */
    fun validateTemplate(template: String): Boolean {
        if (template.isBlank()) return false
        val tokenRegex = Regex("\\{[^}]+\\}")
        val tokensFound = tokenRegex.findAll(template).map { it.value }.toSet()
        return tokensFound.all { it in NamingTemplateConfig.ALLOWED_PLACEHOLDERS }
    }

    /**
     * Applies template formatting with given video metadata context.
     */
    fun parse(template: String, context: VideoTemplateContext): String {
        val safeTemplate = if (validateTemplate(template)) template else NamingTemplateConfig.DEFAULT_TEMPLATE

        var result = safeTemplate
            .replace("{title}", context.title)
            .replace("{quality}", context.quality)
            .replace("{ext}", context.ext.removePrefix("."))
            .replace("{platform}", context.platform)
            .replace("{date}", context.date)
            .replace("{id}", context.id)

        // Ensure extension is not duplicated if present in both body and ext
        val cleanExt = context.ext.removePrefix(".")
        if (!result.endsWith(".$cleanExt")) {
            result = "$result.$cleanExt"
        }

        return result
    }

    /**
     * Generates a live preview formatted string for preferences screen.
     */
    fun preview(template: String): String {
        return parse(template, SAMPLE_PREVIEW_CONTEXT)
    }
}
