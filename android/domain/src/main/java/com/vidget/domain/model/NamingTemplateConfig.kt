package com.vidget.domain.model

import kotlinx.serialization.Serializable

/**
 * File naming template configuration and preferences.
 */
@Serializable
data class NamingTemplateConfig(
    val template: String = DEFAULT_TEMPLATE,
    val subfolderOrganization: SubfolderOrganization = SubfolderOrganization.NONE,
    val conflictResolution: NameConflictResolution = NameConflictResolution.AUTO_RENAME
) {
    companion object {
        const val DEFAULT_TEMPLATE = "{title} - {quality}.{ext}"

        /**
         * The only allowed placeholder tokens as explicitly required.
         */
        val ALLOWED_PLACEHOLDERS = setOf(
            "{title}",
            "{quality}",
            "{ext}",
            "{platform}",
            "{date}",
            "{id}"
        )
    }
}
