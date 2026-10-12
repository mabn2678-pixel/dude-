package com.vidget.core.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class PermissionType {
    NOTIFICATIONS,
    LEGACY_STORAGE_WRITE,
    EXTERNAL_MEDIA_READ
}

sealed interface PermissionStatus {
    data object Granted : PermissionStatus
    data object NotRequested : PermissionStatus
    data object DeniedOnce : PermissionStatus
    data object PermanentlyDenied : PermissionStatus
}

@Singleton
class PermissionManager @Inject constructor() {

    private val _notificationStatus = MutableStateFlow<PermissionStatus>(PermissionStatus.NotRequested)
    val notificationStatus: StateFlow<PermissionStatus> = _notificationStatus.asStateFlow()

    private val _storageWriteStatus = MutableStateFlow<PermissionStatus>(PermissionStatus.NotRequested)
    val storageWriteStatus: StateFlow<PermissionStatus> = _storageWriteStatus.asStateFlow()

    private val _mediaReadStatus = MutableStateFlow<PermissionStatus>(PermissionStatus.NotRequested)
    val mediaReadStatus: StateFlow<PermissionStatus> = _mediaReadStatus.asStateFlow()

    // Non-intrusive one-time reminder flag when notifications are denied
    private val _silentNotificationNoticeShown = MutableStateFlow(false)
    val silentNotificationNoticeShown: StateFlow<Boolean> = _silentNotificationNoticeShown.asStateFlow()

    /**
     * Inspects current runtime status of a given PermissionType against Android system.
     */
    fun checkStatus(context: Context, type: PermissionType): PermissionStatus {
        val permissions = when (type) {
            PermissionType.NOTIFICATIONS -> PermissionPolicy.getNotificationPermissions()
            PermissionType.LEGACY_STORAGE_WRITE -> PermissionPolicy.getStorageWritePermissions(isPublicDirectory = true)
            PermissionType.EXTERNAL_MEDIA_READ -> PermissionPolicy.getExternalMediaReadPermissions(accessExternalLibraryFiles = true)
        }

        if (permissions.isEmpty()) {
            return PermissionStatus.Granted // Not needed on this Android version
        }

        val allGranted = permissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }

        return if (allGranted) PermissionStatus.Granted else PermissionStatus.NotRequested
    }

    /**
     * Updates recorded state after request result is received from the system.
     */
    fun onPermissionResult(
        activity: Activity,
        type: PermissionType,
        isGranted: Boolean,
        permissionsRequested: List<String>
    ) {
        val newStatus = when {
            isGranted -> PermissionStatus.Granted
            permissionsRequested.any { perm ->
                ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
            } -> PermissionStatus.DeniedOnce
            else -> PermissionStatus.PermanentlyDenied
        }

        when (type) {
            PermissionType.NOTIFICATIONS -> _notificationStatus.value = newStatus
            PermissionType.LEGACY_STORAGE_WRITE -> _storageWriteStatus.value = newStatus
            PermissionType.EXTERNAL_MEDIA_READ -> _mediaReadStatus.value = newStatus
        }
    }

    fun markSilentNotificationNoticeShown() {
        _silentNotificationNoticeShown.value = true
    }

    /**
     * Opens system app details settings screen for permanent denial recovery.
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Opens battery optimization overview screen safely without restricted Play Store permissions.
     */
    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
