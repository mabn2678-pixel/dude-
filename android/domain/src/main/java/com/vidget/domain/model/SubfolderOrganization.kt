package com.vidget.domain.model

import kotlinx.serialization.Serializable

/**
 * Subfolder organization scheme for downloads.
 */
@Serializable
enum class SubfolderOrganization {
    /**
     * Files saved directly in the destination root folder without subfolders (Default).
     */
    NONE,

    /**
     * Subfolder named after source platform (e.g., YouTube, Twitter, Instagram).
     */
    PLATFORM,

    /**
     * Subfolder named after download date in year-month format (e.g., 2026-10).
     */
    DATE
}
