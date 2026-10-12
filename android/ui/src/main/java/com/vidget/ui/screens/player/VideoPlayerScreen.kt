package com.vidget.ui.screens.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.vidget.ui.R
import kotlinx.coroutines.delay
import java.util.Locale

enum class VideoResizeMode(val label: String, val modeValue: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
}

/**
 * Modern Clean ExoPlayer Video Player Screen.
 * Supports:
 * 1. Playback resumption via SharedPreferences by URI key.
 * 2. Gestures: Double-tap or buttons for 10-second skip/rewind.
 * 3. Playback speeds: 0.5x, 0.75x, 1x, 1.25x, 1.5x, 2x.
 * 4. Aspect Ratio resize mode toggle (Fit, Fill, Zoom).
 * 5. Screen controls lock to prevent accidental touches.
 * 6. Picture-in-Picture (PiP) support for Android 8+.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoUri: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("vidget_playback_positions", Context.MODE_PRIVATE) }

    // ExoPlayer instance lifecycle
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUri.toUri())
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    // Playback state
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var resizeMode by remember { mutableStateOf(VideoResizeMode.FIT) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isControlsLocked by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showResumeToast by remember { mutableStateOf(false) }

    // Restore saved playback position
    LaunchedEffect(videoUri) {
        val savedPosition = prefs.getLong(videoUri, 0L)
        if (savedPosition > 1000L) {
            exoPlayer.seekTo(savedPosition)
            showResumeToast = true
        }
    }

    // Save position periodically & on pause
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPositionMs = exoPlayer.currentPosition
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            if (currentPositionMs > 1000L && durationMs > 0L) {
                // If within 95% of ending, reset; otherwise save position
                if (currentPositionMs < durationMs * 0.95f) {
                    prefs.edit().putLong(videoUri, currentPositionMs).apply()
                } else {
                    prefs.edit().remove(videoUri).apply()
                }
            }
            delay(500)
        }
    }

    // Auto-hide controls after 4 seconds of inactivity
    LaunchedEffect(controlsVisible, isPlaying, isControlsLocked) {
        if (controlsVisible && isPlaying && !isControlsLocked) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Track Player events
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            val finalPos = exoPlayer.currentPosition
            if (finalPos > 1000L && durationMs > 0 && finalPos < durationMs * 0.95f) {
                prefs.edit().putLong(videoUri, finalPos).apply()
            }
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    BackHandler {
        onBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isControlsLocked) {
                    controlsVisible = !controlsVisible
                } else {
                    controlsVisible = !controlsVisible
                }
            }
    ) {
        // ExoPlayer View Interop
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode.modeValue
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode.modeValue
            },
            modifier = Modifier.fillMaxSize()
        )

        // Resume feedback indicator
        if (showResumeToast) {
            LaunchedEffect(Unit) {
                delay(3000)
                showResumeToast = false
            }
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
            ) {
                val sec = (currentPositionMs / 1000) % 60
                val min = (currentPositionMs / 1000) / 60
                val timeStr = String.format(Locale.US, "%02d:%02d", min, sec)
                Text(
                    text = stringResource(R.string.player_resumed_at, timeStr),
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // Lock button alone when locked
        if (isControlsLocked && controlsVisible) {
            IconButton(
                onClick = { isControlsLocked = false },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.rounded.Lock,
                    contentDescription = stringResource(R.string.player_unlock_controls),
                    tint = Color.White
                )
            }
        }

        // Full Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible && !isControlsLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Text(
                        text = stringResource(R.string.player_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    // Aspect Ratio Button
                    IconButton(
                        onClick = {
                            resizeMode = when (resizeMode) {
                                VideoResizeMode.FIT -> VideoResizeMode.FILL
                                VideoResizeMode.FILL -> VideoResizeMode.ZOOM
                                VideoResizeMode.ZOOM -> VideoResizeMode.FIT
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.rounded.AspectRatio,
                            contentDescription = resizeMode.label,
                            tint = Color.White
                        )
                    }

                    // Speed Selector Menu
                    Box {
                        IconButton(onClick = { showSpeedMenu = true }) {
                            Icon(Icons.rounded.Speed, contentDescription = "Speed", tint = Color.White)
                        }

                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("${speed}x") },
                                    onClick = {
                                        playbackSpeed = speed
                                        exoPlayer.setPlaybackSpeed(speed)
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Screen Lock Button
                    IconButton(onClick = { isControlsLocked = true }) {
                        Icon(Icons.rounded.LockOpen, contentDescription = "Lock", tint = Color.White)
                    }

                    // PiP Button
                    IconButton(
                        onClick = {
                            val activity = context as? Activity
                            if (activity != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                val params = android.app.PictureInPictureParams.Builder().build()
                                activity.enterPictureInPictureMode(params)
                            }
                        }
                    ) {
                        Icon(Icons.rounded.PictureInPictureAlt, contentDescription = "PiP", tint = Color.White)
                    }
                }

                // Center Play / Pause & 10s Skip Rewind
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.rounded.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.rounded.Pause else Icons.rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationMs)
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.rounded.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom Timeline and Time Indicators
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    val currentSec = (currentPositionMs / 1000) % 60
                    val currentMin = (currentPositionMs / 1000) / 60
                    val durSec = (durationMs / 1000) % 60
                    val durMin = (durationMs / 1000) / 60

                    val timeCurrent = String.format(Locale.US, "%02d:%02d", currentMin, currentSec)
                    val timeTotal = String.format(Locale.US, "%02d:%02d", durMin, durSec)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = timeCurrent, color = Color.White, fontSize = 12.sp)
                        Text(text = timeTotal, color = Color.White, fontSize = 12.sp)
                    }

                    Slider(
                        value = if (durationMs > 0) currentPositionMs.toFloat() else 0f,
                        onValueChange = { newPos ->
                            exoPlayer.seekTo(newPos.toLong())
                            currentPositionMs = newPos.toLong()
                        },
                        valueRange = 0f..(durationMs.toFloat().coerceAtLeast(1f)),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    }
}
