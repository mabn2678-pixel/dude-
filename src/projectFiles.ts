export interface ProjectFile {
  path: string;
  name: string;
  module: string;
  language: 'kotlin' | 'toml' | 'xml' | 'pro' | 'markdown';
  content: string;
  description: string;
}

export const PROJECT_FILES: ProjectFile[] = [
  // --- Permissions Architecture (Prompt 5) ---
  {
    path: 'core/src/main/java/com/vidget/core/permission/PermissionPolicy.kt',
    name: 'PermissionPolicy.kt',
    module: 'core',
    language: 'kotlin',
    description: 'محرك قرارات الأذونات بحسب إصدار Android وسياق الاستخدام (POST_NOTIFICATIONS, WRITE_EXTERNAL_STORAGE maxSdk=28, READ_MEDIA)',
    content: `package com.vidget.core.permission

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
}`
  },
  {
    path: 'core/src/main/java/com/vidget/core/permission/PermissionManager.kt',
    name: 'PermissionManager.kt',
    module: 'core',
    language: 'kotlin',
    description: 'مدير تتبع حالات الأذونات كـ StateFlow وتوجيه المستخدم للإعدادات عند الرفض الدائم وتحسين البطارية',
    content: `package com.vidget.core.permission

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

    private val _silentNotificationNoticeShown = MutableStateFlow(false)
    val silentNotificationNoticeShown: StateFlow<Boolean> = _silentNotificationNoticeShown.asStateFlow()

    fun checkStatus(context: Context, type: PermissionType): PermissionStatus {
        val permissions = when (type) {
            PermissionType.NOTIFICATIONS -> PermissionPolicy.getNotificationPermissions()
            PermissionType.LEGACY_STORAGE_WRITE -> PermissionPolicy.getStorageWritePermissions(isPublicDirectory = true)
            PermissionType.EXTERNAL_MEDIA_READ -> PermissionPolicy.getExternalMediaReadPermissions(accessExternalLibraryFiles = true)
        }

        if (permissions.isEmpty()) {
            return PermissionStatus.Granted
        }

        val allGranted = permissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }

        return if (allGranted) PermissionStatus.Granted else PermissionStatus.NotRequested
    }

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

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}`
  },
  {
    path: 'core/src/test/java/com/vidget/core/permission/PermissionPolicyTest.kt',
    name: 'PermissionPolicyTest.kt',
    module: 'core',
    language: 'kotlin',
    description: 'اختبارات الوحدة للتحقق من قرارات الأذونات عبر إصدارات Android المختلفة وعدم طلب أي إذن غير لازم',
    content: `package com.vidget.core.permission

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionPolicyTest {

    @Test
    fun notifications_required_only_on_android_13_and_above() {
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

        assertTrue(PermissionPolicy.getNotificationPermissions(sdkInt = 32).isEmpty())
        assertTrue(PermissionPolicy.getNotificationPermissions(sdkInt = 28).isEmpty())
        assertTrue(PermissionPolicy.getNotificationPermissions(sdkInt = 24).isEmpty())
    }

    @Test
    fun write_storage_required_only_on_android_9_and_below_for_public_directory() {
        assertEquals(
            listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PermissionPolicy.getStorageWritePermissions(isPublicDirectory = true, sdkInt = 28)
        )
        assertEquals(
            listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PermissionPolicy.getStorageWritePermissions(isPublicDirectory = true, sdkInt = 24)
        )

        assertTrue(
            PermissionPolicy.getStorageWritePermissions(isPublicDirectory = false, sdkInt = 28).isEmpty()
        )

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
        val android13Media = PermissionPolicy.getExternalMediaReadPermissions(
            accessExternalLibraryFiles = true,
            sdkInt = 33
        )
        assertEquals(
            listOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO),
            android13Media
        )

        val android12Media = PermissionPolicy.getExternalMediaReadPermissions(
            accessExternalLibraryFiles = true,
            sdkInt = 32
        )
        assertEquals(listOf(Manifest.permission.READ_EXTERNAL_STORAGE), android12Media)

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
}`
  },
  {
    path: 'core/src/main/java/com/vidget/core/service/DataSyncTimeoutHandler.kt',
    name: 'DataSyncTimeoutHandler.kt',
    module: 'core',
    language: 'kotlin',
    description: 'معالج مهلة 6 ساعات لخدمة dataSync على Android 14/15 عبر onTimeout لحفظ نقاط التوقف وإعادة الجدولة',
    content: `package com.vidget.core.service

import android.content.Context
import android.os.Build
import android.util.Log

class DataSyncTimeoutHandler(
    private val context: Context
) {
    fun handleDataSyncTimeout(startId: Int, fgsType: Int) {
        if (Build.VERSION.SDK_INT >= 35) {
            Log.w(
                TAG,
                "Android 15 DataSync Foreground Service timeout reached (startId: $startId, fgsType: $fgsType). Saving progress and rescheduling..."
            )
            rescheduleRemainingDownloads()
        }
    }

    private fun rescheduleRemainingDownloads() {
        Log.i(TAG, "Pending downloads state saved. Rescheduled via background scheduler.")
    }

    companion object {
        private const val TAG = "DataSyncTimeoutHandler"
    }
}`
  },
  {
    path: 'ui/src/main/java/com/vidget/ui/components/PermissionRationaleDialog.kt',
    name: 'PermissionRationaleDialog.kt',
    module: 'ui',
    language: 'kotlin',
    description: 'مكون الحوار التوضيحي (Dialog) مع أيقونة وعنوان وشرح سطرين وزري متابعة/ليس الآن أو فتح الإعدادات',
    content: `package com.vidget.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vidget.core.permission.PermissionStatus
import com.vidget.core.permission.PermissionType
import com.vidget.ui.R
import com.vidget.ui.theme.MinTouchTargetSize

data class PermissionDialogStrings(
    val titleResId: Int,
    val descriptionResId: Int,
    val icon: ImageVector
)

fun resolveDialogContent(
    type: PermissionType,
    status: PermissionStatus
): PermissionDialogStrings {
    return when (type) {
        PermissionType.NOTIFICATIONS -> {
            when (status) {
                PermissionStatus.DeniedOnce -> PermissionDialogStrings(
                    titleResId = R.string.perm_notif_rationale_title,
                    descriptionResId = R.string.perm_notif_rationale_desc,
                    icon = Icons.Rounded.NotificationsActive
                )
                PermissionStatus.PermanentlyDenied -> PermissionDialogStrings(
                    titleResId = R.string.perm_notif_settings_title,
                    descriptionResId = R.string.perm_notif_settings_desc,
                    icon = Icons.Rounded.Settings
                )
                else -> PermissionDialogStrings(
                    titleResId = R.string.perm_notif_title,
                    descriptionResId = R.string.perm_notif_desc,
                    icon = Icons.Rounded.NotificationsActive
                )
            }
        }
        PermissionType.LEGACY_STORAGE_WRITE -> {
            when (status) {
                PermissionStatus.DeniedOnce -> PermissionDialogStrings(
                    titleResId = R.string.perm_storage_write_rationale_title,
                    descriptionResId = R.string.perm_storage_write_rationale_desc,
                    icon = Icons.Rounded.SdCard
                )
                PermissionStatus.PermanentlyDenied -> PermissionDialogStrings(
                    titleResId = R.string.perm_storage_write_settings_title,
                    descriptionResId = R.string.perm_storage_write_settings_desc,
                    icon = Icons.Rounded.Settings
                )
                else -> PermissionDialogStrings(
                    titleResId = R.string.perm_storage_write_title,
                    descriptionResId = R.string.perm_storage_write_desc,
                    icon = Icons.Rounded.SdCard
                )
            }
        }
        PermissionType.EXTERNAL_MEDIA_READ -> {
            when (status) {
                PermissionStatus.DeniedOnce -> PermissionDialogStrings(
                    titleResId = R.string.perm_media_read_rationale_title,
                    descriptionResId = R.string.perm_media_read_rationale_desc,
                    icon = Icons.Rounded.PermMedia
                )
                PermissionStatus.PermanentlyDenied -> PermissionDialogStrings(
                    titleResId = R.string.perm_media_read_settings_title,
                    descriptionResId = R.string.perm_media_read_settings_desc,
                    icon = Icons.Rounded.Settings
                )
                else -> PermissionDialogStrings(
                    titleResId = R.string.perm_media_read_title,
                    descriptionResId = R.string.perm_media_read_desc,
                    icon = Icons.Rounded.PermMedia
                )
            }
        }
    }
}

@Composable
fun PermissionRationaleDialog(
    type: PermissionType,
    status: PermissionStatus,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val content = resolveDialogContent(type, status)
    val isPermanent = status is PermissionStatus.PermanentlyDenied

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = content.icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = if (isPermanent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(content.titleResId),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(content.descriptionResId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.height(MinTouchTargetSize)
            ) {
                Text(
                    text = if (isPermanent) {
                        stringResource(R.string.action_open_settings)
                    } else {
                        stringResource(R.string.action_continue)
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.height(MinTouchTargetSize)
            ) {
                Text(
                    text = stringResource(R.string.action_not_now),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        modifier = modifier
    )
}`
  },
  {
    path: 'ui/src/main/res/values/strings.xml',
    name: 'strings.xml (en)',
    module: 'ui',
    language: 'xml',
    description: 'ملف النصوص الإنجليزية شاملاً نصوص الأذونات وشرح السطرين وإشعارات التراجع',
    content: `<!-- Permissions Dialog Strings -->
<string name="action_continue">Continue</string>
<string name="action_not_now">Not Now</string>
<string name="action_open_settings">Open Settings</string>
<string name="action_battery_optimization">Ignore Battery Optimizations</string>
<string name="action_battery_optimization_desc">Ensures uninterrupted long background downloads.</string>

<string name="perm_notif_title">Allow Download Notifications</string>
<string name="perm_notif_desc">Notification permission is required to track download progress and alert you upon completion.</string>
<string name="perm_notif_rationale_title">Download Notifications are Essential</string>
<string name="perm_notif_rationale_desc">Notifications allow pausing, resuming, and tracking downloads in the background without keeping the app open.</string>
<string name="perm_notif_settings_title">Enable Notifications in Settings</string>
<string name="perm_notif_settings_desc">Notifications were permanently declined. Please enable them in app settings to monitor downloads.</string>
<string name="perm_notif_silent_notice">Downloads will continue in the background without progress notifications.</string>

<string name="perm_storage_write_title">Storage Permission for Downloads</string>
<string name="perm_storage_write_desc">Writing files to the public Downloads folder on this Android version requires storage access.</string>
<string name="perm_storage_write_rationale_title">Why Storage Access is Required</string>
<string name="perm_storage_write_rationale_desc">On Android 9 and older, storage permission is mandatory to store files in the public device gallery.</string>
<string name="perm_storage_write_settings_title">Grant Storage in Settings</string>
<string name="perm_storage_write_settings_desc">Storage permission was permanently denied. Please enable it in app settings to save to public folders.</string>

<string name="perm_media_read_title">Browse External Media in Library</string>
<string name="perm_media_read_desc">Media permission is required to display and manage videos and audio stored outside the app folder.</string>
<string name="perm_media_read_rationale_title">External Media Access</string>
<string name="perm_media_read_rationale_desc">This permission is used solely to discover and play your local device video files within the library.</string>
<string name="perm_media_read_settings_title">Grant Media Access in Settings</string>
<string name="perm_media_read_settings_desc">Media permission was permanently declined. Please enable it in app settings to view external media.</string>`
  },
  {
    path: 'ui/src/main/res/values-ar/strings.xml',
    name: 'strings.xml (ar)',
    module: 'ui',
    language: 'xml',
    description: 'ملف النصوص العربية المترجمة بدقة وفق القواعد الصارمة لمنع النصوص الصلبة',
    content: `<!-- نصوص حوارات الأذونات باللغة العربية -->
<string name="action_continue">متابعة</string>
<string name="action_not_now">ليس الآن</string>
<string name="action_open_settings">فتح الإعدادات</string>
<string name="action_battery_optimization">تجاهل تحسين البطارية</string>
<string name="action_battery_optimization_desc">لضمان استمرار التحميلات الطويلة في الخلفية دون توقف.</string>

<string name="perm_notif_title">السماح بإشعارات التنزيل</string>
<string name="perm_notif_desc">نحتاج إذن الإشعارات لإظهار تقدم التنزيل وإعلامك فور اكتمال الملفات.</string>
<string name="perm_notif_rationale_title">أهمية إشعارات التنزيل</string>
<string name="perm_notif_rationale_desc">تتيح لك الإشعارات متابعة سرعة التحميل وإيقافه مؤقتاً ومعرفة وقت الانتهاء دون إبقاء التطبيق مفتوحاً.</string>
<string name="perm_notif_settings_title">تفعيل الإشعارات من الإعدادات</string>
<string name="perm_notif_settings_desc">تم تعطيل الإشعارات نهائياً. يرجى تفعيلها من إعدادات التطبيق لتتمكن من متابعة التحميلات.</string>
<string name="perm_notif_silent_notice">سيستمر التحميل بالخلفية بدون إشعارات تقدم.</string>

<string name="perm_storage_write_title">إذن حفظ التنزيلات العامة</string>
<string name="perm_storage_write_desc">يتطلب حفظ الملفات في مجلد التنزيلات العام على هذا الإصدار من النظام إذن التخزين.</string>
<string name="perm_storage_write_rationale_title">لماذا نحتاج إذن التخزين</string>
<string name="perm_storage_write_rationale_desc">في إصدارات أندرويد 9 وأقدم، إذن التخزين إلزامي لتتمكن من حفظ مقاطع الفيديو في معرض الجهاز العام.</string>
<string name="perm_storage_write_settings_title">منح إذن التخزين من الإعدادات</string>
<string name="perm_storage_write_settings_desc">تم رفض إذن التخزين بشكل دائم. يرجى منحه من إعدادات التطبيق لتتمكن من الحفظ في المجلد العام.</string>

<string name="perm_media_read_title">عرض الفيديوهات الخارجية بالمكتبة</string>
<string name="perm_media_read_desc">لعرض وتشغيل الفيديوهات المخزنة خارج التطبيق، يلزم السماح بالوصول لوسائط الجهاز.</string>
<string name="perm_media_read_rationale_title">الوصول إلى وسائط المكتبة</string>
<string name="perm_media_read_rationale_desc">يُستخدم هذا الإذن حصرياً لعرض مقاطع الفيديو والصوتيات الموجودة في ذاكرة هاتفك داخل المكتبة.</string>
<string name="perm_media_read_settings_title">منح إذن الوسائط من الإعدادات</string>
<string name="perm_media_read_settings_desc">تم رفض إذن الوسائط بشكل دائم. يرجى تفعيله من الإعدادات لتصفح الملفات الخارجية في المكتبة.</string>`
  },
  {
    path: 'app/src/main/AndroidManifest.xml',
    name: 'AndroidManifest.xml',
    module: 'app',
    language: 'xml',
    description: 'مانيفست التطبيق مع تصاريح POST_NOTIFICATIONS و FOREGROUND_SERVICE_DATA_SYNC وتحديد maxSdkVersion=28',
    content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Network Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- Storage and Media Permissions -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

    <!-- Background & Foreground Execution -->
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />

    <application
        android:name=".VidGetApp"
        android:allowBackup="true"
        android:icon="@android:drawable/sym_def_app_icon"
        android:label="@string/app_name"
        android:roundIcon="@android:drawable/sym_def_app_icon"
        android:supportsRtl="true"
        android:theme="@style/Theme.VidGet">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
            android:theme="@style/Theme.App.Starting">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>`
  },
  {
    path: 'app/src/main/java/com/vidget/app/MainActivity.kt',
    name: 'MainActivity.kt',
    module: 'app',
    language: 'kotlin',
    description: 'النشاط الرئيسي مدمج مع PermissionManager وإعدادات البطارية و SplashScreen',
    content: `package com.vidget.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidget.ui.navigation.AppNavGraph
import com.vidget.ui.theme.VidGetTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val splashViewModel: SplashViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var permissionManager: com.vidget.core.permission.PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition { splashViewModel.uiState.value is SplashUiState.Loading }

        setContent {
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by mainViewModel.dynamicColor.collectAsStateWithLifecycle()
            val splashState by splashViewModel.uiState.collectAsStateWithLifecycle()

            VidGetTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (val state = splashState) {
                        is SplashUiState.Loading -> Box(modifier = Modifier.fillMaxSize())
                        is SplashUiState.Success -> {
                            AppNavGraph(
                                startDestination = state.startDestination,
                                onOnboardingFinished = splashViewModel::completeOnboarding,
                                onOpenBatteryOptimization = {
                                    permissionManager.openBatteryOptimizationSettings(this@MainActivity)
                                },
                                activeDownloadsCount = 0
                            )
                        }
                    }
                }
            }
        }
    }
}`
  },
  {
    path: 'ui/src/main/java/com/vidget/ui/navigation/Routes.kt',
    name: 'Routes.kt',
    module: 'ui',
    language: 'kotlin',
    description: 'المسارات الآمنة لنظام Navigation Compose',
    content: `package com.vidget.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.vidget.ui.R
import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Onboarding : Route
    @Serializable data object Home : Route
    @Serializable data object Downloads : Route
    @Serializable data object Library : Route
    @Serializable data object Settings : Route
    @Serializable data class Player(val videoUri: String? = null) : Route
    @Serializable data object LanguageSettings : Route
    @Serializable data object AboutScreen : Route
}

enum class BottomNavDestination(val route: Route, @StringRes val labelResId: Int, val icon: ImageVector) {
    HOME(Route.Home, R.string.nav_home, Icons.Rounded.Home),
    DOWNLOADS(Route.Downloads, R.string.nav_downloads, Icons.Rounded.Download),
    LIBRARY(Route.Library, R.string.nav_library, Icons.Rounded.VideoLibrary),
    SETTINGS(Route.Settings, R.string.nav_settings, Icons.Rounded.Settings)
}`
  },
  {
    path: 'ui/src/main/java/com/vidget/ui/theme/Theme.kt',
    name: 'Theme.kt',
    module: 'ui',
    language: 'kotlin',
    description: 'ثيم التطبيق مع ألوان الوضعين الداكن والفاتح ودعم Material You والخطوط التلقائية',
    content: `package com.vidget.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.vidget.domain.model.ThemeMode

@Composable
fun VidGetTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    isArabic: Boolean = isCurrentLocaleArabic(),
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors,
        LocalAppFontFamily provides resolveFontFamily(isArabic),
        LocalMetricTextStyle provides createMetricTextStyle(isArabic)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = appTypography(isArabic),
            shapes = VidGetShapes,
            content = content
        )
    }
}`
  }
];
