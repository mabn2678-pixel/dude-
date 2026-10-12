package com.vidget.domain.model

import kotlinx.serialization.Serializable

/**
 * Supported storage destination types for VidGet.
 * Adheres to Scoped Storage and MediaStore on modern Android versions,
 * with Storage Access Framework (SAF) for user-picked custom folders and SD Cards.
 */
@Serializable
sealed interface DestinationType {

    /**
     * Default public downloads: Downloads/VidGet
     * - Android 10+ (API 29+): MediaStore.Downloads (No runtime permissions)
     * - Android 9 and below: Direct filesystem path with WRITE_EXTERNAL_STORAGE
     */
    @Serializable
    data object DownloadsDefault : DestinationType

    /**
     * Dedicated media directories:
     * - Video files: Movies/VidGet via MediaStore.Video
     * - Audio files: Music/VidGet via MediaStore.Audio
     */
    @Serializable
    data object Movies : DestinationType

    /**
     * User-selected directory picked via ACTION_OPEN_DOCUMENT_TREE.
     * Stored with takePersistableUriPermission and verified on each launch.
     */
    @Serializable
    data class CustomFolder(
        val uriString: String,
        val displayName: String
    ) : DestinationType

    /**
     * External Removable SD Card root or directory via SAF.
     * Includes hint guiding user to pick root from storage drawer.
     */
    @Serializable
    data class SdCard(
        val uriString: String,
        val displayName: String
    ) : DestinationType
}
