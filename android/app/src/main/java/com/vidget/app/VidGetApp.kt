package com.vidget.app

import android.app.Application
import android.util.Log
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface InitializationState {
    data object Idle : InitializationState
    data object Initializing : InitializationState
    data object Ready : InitializationState
    data class Error(val throwable: Throwable) : InitializationState
}

@HiltAndroidApp
class VidGetApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _initializationState = MutableStateFlow<InitializationState>(InitializationState.Idle)
    val initializationState: StateFlow<InitializationState> = _initializationState.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        instance = this
        initializeDownloaderTools()
    }

    private fun initializeDownloaderTools() {
        _initializationState.value = InitializationState.Initializing
        applicationScope.launch {
            try {
                Log.d(TAG, "Initializing yt-dlp, FFmpeg, and aria2c on Dispatchers.IO...")

                // 1. Initialize YoutubeDL core native binary (yt-dlp executable & python runtime)
                YoutubeDL.getInstance().init(this@VidGetApp)

                // 2. Initialize FFmpeg native binary
                FFmpeg.getInstance().init(this@VidGetApp)

                // 3. Initialize Aria2c native binary
                Aria2c.getInstance().init(this@VidGetApp)

                _initializationState.value = InitializationState.Ready
                // Acceptance criteria: prints in Logcat that yt-dlp, FFmpeg, and aria2c are ready
                Log.i(TAG, "yt-dlp, FFmpeg, and aria2c are ready.")
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to initialize downloader binaries: ${t.message}", t)
                _initializationState.value = InitializationState.Error(t)
            }
        }
    }

    companion object {
        private const val TAG = "VidGetApp"
        lateinit var instance: VidGetApp
            private set
    }
}
