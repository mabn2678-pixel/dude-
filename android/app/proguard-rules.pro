# ==============================================================================
# VidGet ProGuard / R8 Configuration Rules
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. youtubedl-android, FFmpeg, and aria2c (JNI & Reflection Rules)
# ------------------------------------------------------------------------------
-keep class com.yausername.youtubedl_android.** { *; }
-keepclassmembers class com.yausername.youtubedl_android.** { *; }

-keep class com.yausername.ffmpeg.** { *; }
-keepclassmembers class com.yausername.ffmpeg.** { *; }

-keep class com.yausername.aria2c.** { *; }
-keepclassmembers class com.yausername.aria2c.** { *; }

-keepclassmembers class * {
    native <methods>;
}

# ------------------------------------------------------------------------------
# 2. Hilt / Dagger Dependency Injection
# ------------------------------------------------------------------------------
-keepattributes *Annotation*
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class androidx.hilt.work.** { *; }
-keepclassmembers,allowobfuscation class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
    @javax.inject.Inject <init>(...);
}

# ------------------------------------------------------------------------------
# 3. Room Persistence Library
# ------------------------------------------------------------------------------
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.migration.Migration

# ------------------------------------------------------------------------------
# 4. Kotlinx Serialization
# ------------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class * {
    <init>(...);
}

# ------------------------------------------------------------------------------
# 5. OkHttp & Okio Networking
# ------------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ------------------------------------------------------------------------------
# 6. Jsoup HTML Parser
# ------------------------------------------------------------------------------
-keep public class org.jsoup.** { public *; }
-dontwarn org.jsoup.**

# ------------------------------------------------------------------------------
# 7. AndroidX Media3 / ExoPlayer
# ------------------------------------------------------------------------------
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.ui.** { *; }
-keep class androidx.media3.common.** { *; }
-dontwarn androidx.media3.**

# ------------------------------------------------------------------------------
# 8. WorkManager
# ------------------------------------------------------------------------------
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ------------------------------------------------------------------------------
# 9. Kotlin Coroutines
# ------------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
