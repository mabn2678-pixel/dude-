package com.vidget.core.permission

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionPolicyTest {

    @Test
    fun notifications_required_only_on_android_13_and_above() {
        // Android 13 (API 33), Android 14 (API 34), Android 15 (API 35)
        assertEquals(
            listOf(Manifest.permission.POST_NOTIFICATIONS),
            PermissionPolicy.getNotificationPermissions(sdkInt = 33)
        )
        assertEquals(
            listOf(Manifest.permission.POST_NOTIFICATIONS),
            PermissionPolicy.getNotificationPermissions(sdkInt = 34)
        )
        assertEquals(
            listOf(Manifest.permission.POST_NOTIFICATIONS),
            PermissionPolicy.getNotificationPermissions(sdkInt = 35)
        )

        // Android 12 (API 32) and below
        assertTrue(PermissionPolicy.getNotificationPermissions(sdkInt = 32).isEmpty())
        assertTrue(PermissionPolicy.getNotificationPermissions(sdkInt = 28).isEmpty())
        assertTrue(PermissionPolicy.getNotificationPermissions(sdkInt = 24).isEmpty())
    }

    @Test
    fun write_storage_required_only_on_android_9_and_below_for_public_directory() {
        // Android 9 (API 28) and Android 8.1 (API 27) for public storage
        assertEquals(
            listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PermissionPolicy.getStorageWritePermissions(isPublicDirectory = true, sdkInt = 28)
        )
        assertEquals(
            listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PermissionPolicy.getStorageWritePermissions(isPublicDirectory = true, sdkInt = 24)
        )

        // Android 9 but private directory
        assertTrue(
            PermissionPolicy.getStorageWritePermissions(isPublicDirectory = false, sdkInt = 28).isEmpty()
        )

        // Android 10+ (API 29..35) Scoped Storage does not require WRITE_EXTERNAL_STORAGE
        assertTrue(
            PermissionPolicy.getStorageWritePermissions(isPublicDirectory = true, sdkInt = 29).isEmpty()
        )
        assertTrue(
            PermissionPolicy.getStorageWritePermissions(isPublicDirectory = true, sdkInt = 34).isEmpty()
        )
        assertTrue(
            PermissionPolicy.getStorageWritePermissions(isPublicDirectory = true, sdkInt = 35).isEmpty()
        )
    }

    @Test
    fun read_media_external_library_granularity_by_android_version() {
        // Android 13+ (API 33+) granular media permissions
        val android13Media = PermissionPolicy.getExternalMediaReadPermissions(
            accessExternalLibraryFiles = true,
            sdkInt = 33
        )
        assertEquals(
            listOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO),
            android13Media
        )

        // Android 12 and below (API <= 32) legacy read permission
        val android12Media = PermissionPolicy.getExternalMediaReadPermissions(
            accessExternalLibraryFiles = true,
            sdkInt = 32
        )
        assertEquals(listOf(Manifest.permission.READ_EXTERNAL_STORAGE), android12Media)

        // When user does not browse external files (app storage only)
        assertTrue(
            PermissionPolicy.getExternalMediaReadPermissions(
                accessExternalLibraryFiles = false,
                sdkInt = 35
            ).isEmpty()
        )
    }

    @Test
    fun forbidden_permissions_are_never_included() {
        val allPermissions = PermissionPolicy.getNotificationPermissions(35) +
            PermissionPolicy.getStorageWritePermissions(true, 28) +
            PermissionPolicy.getExternalMediaReadPermissions(true, 35)

        assertFalse(allPermissions.contains(PermissionPolicy.FORBIDDEN_MANAGE_EXTERNAL_STORAGE))
        assertFalse(allPermissions.contains(PermissionPolicy.FORBIDDEN_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS))
    }
}
