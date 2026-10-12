package com.vidget.domain.model

import kotlinx.serialization.Serializable

/**
 * File name collision resolution strategies.
 */
@Serializable
enum class NameConflictResolution {
    /**
     * Appends an incremental number (e.g., "video (1).mp4", "video (2).mp4") (Default).
     */
    AUTO_RENAME,

    /**
     * Overwrites existing file in destination.
     */
    OVERWRITE,

    /**
     * Skips file move/download.
     */
    SKIP,

    /**
     * Prompts the user with an interactive dialog when collision occurs.
     */
    ASK_EVERY_TIME
}
