package com.vidget.core.permission

import android.Manifest
import android.os.Build

/**
 * Pure decision engine defining exact runtime permissions required based on Android SDK level
 * and feature context. Strictly adheres to Google Play privacy policies and scoped storage.
 */
object PermissionPolicy {

    /**
     * Notification permission decision:
     * - Required only on Android 13 (API 33) and above when triggering the first download.
     * - Returns empty list on Android 12 and below (granted automatically at install).
     */
    fun getNotificationPermissions(sdkInt: Int = Build.VERSION.SDK_INT): List<String> {
        return if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList()
        }
    }

    /**
     * Storage write permission decision:
     * - WRITE_EXTERNAL_STORAGE (maxSdkVersion=28) is ONLY required on Android 9 and below (API <= 28)
     *   when writing to the public external Downloads directory.
     * - On Android 10+ (API >= 29), Scoped Storage (MediaStore / SAF) is used and runtime permission is NOT needed.
     */
    fun getStorageWritePermissions(
        isPublicDirectory: Boolean,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): List<String> {
        return if (isPublicDirectory && sdkInt <= Build.VERSION_CODES.P) {
            listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            emptyList()
        }
    }

    /**
     * External media reading decision for the Library:
     * - ONLY required if the user explicitly opts to browse/play files outside the app's dedicated storage.
     * - Android 13+ (API >= 33): requires READ_MEDIA_VIDEO and READ_MEDIA_AUDIO granular permissions.
     * - Android 12 and below (API <= 32): requires READ_EXTERNAL_STORAGE.
     * - Downloading directly to MediaStore or app-scoped SAF folders does NOT require this permission.
     */
    fun getExternalMediaReadPermissions(
        accessExternalLibraryFiles: Boolean,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): List<String> {
        if (!accessExternalLibraryFiles) {
            return emptyList()
        }

        return if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Verifies that forbidden restricted permissions are never requested.
     */
    const val FORBIDDEN_MANAGE_EXTERNAL_STORAGE = "android.permission.MANAGE_EXTERNAL_STORAGE"
    const val FORBIDDEN_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
}
