package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.media.VideoItem
import com.example.player.AspectRatioMode
import com.example.ui.player.components.AspectRatioSheet
import com.example.ui.player.components.AudioSubtitleSheet
import com.example.ui.player.components.PerformanceOverlay
import com.example.ui.player.components.PlaybackSpeedSheet
import com.example.ui.player.components.PlayerControlsOverlay
import com.example.ui.player.components.PlayerGestureDetector
import com.example.ui.player.components.PlayerHudOverlays
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    video: VideoItem,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val performanceMetrics by viewModel.performanceMetrics.collectAsStateWithLifecycle()
    val audioTracks by viewModel.audioTracks.collectAsStateWithLifecycle()
    val subtitleTracks by viewModel.subtitleTracks.collectAsStateWithLifecycle()
    val gestureState by viewModel.gestureState.collectAsStateWithLifecycle()
    val activeSheet by viewModel.activeSheet.collectAsStateWithLifecycle()
    val zoomScale by viewModel.zoomScale.collectAsStateWithLifecycle()
    val panOffsetX by viewModel.panOffsetX.collectAsStateWithLifecycle()
    val panOffsetY by viewModel.panOffsetY.collectAsStateWithLifecycle()

    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    // Keep screen ON and enter Immersive Fullscreen Mode
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Load video on initial composition or video change
    LaunchedEffect(video.id) {
        viewModel.loadVideo(video)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .testTag("player_screen_root")
    ) {
        // Video Surface / PlayerView with Zoom & Pan graphics layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffsetX
                    translationY = panOffsetY
                }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        player = viewModel.getPlayerEngine().getPlayer()
                        resizeMode = when (playbackState.aspectRatioMode) {
                            AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            AspectRatioMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            AspectRatioMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    }
                },
                update = { playerView ->
                    playerView.player = viewModel.getPlayerEngine().getPlayer()
                    playerView.resizeMode = when (playbackState.aspectRatioMode) {
                        AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatioMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        AspectRatioMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Multi-touch Gesture Layer (Brightness, Volume, Seek, Zoom)
        PlayerGestureDetector(
            onSingleTap = { viewModel.onUserInteraction() },
            onDoubleTapLeft = { viewModel.seekBackward(10000L) },
            onDoubleTapRight = { viewModel.seekForward(10000L) },
            onDoubleTapCenter = { viewModel.togglePlayPause() },
            onBrightnessDrag = { deltaPercent ->
                val current = (activity?.window?.attributes?.screenBrightness ?: 0.5f).coerceIn(0.01f, 1f)
                val newBrightness = (current + deltaPercent / 100f).coerceIn(0.01f, 1f)
                activity?.window?.attributes = activity?.window?.attributes?.apply {
                    screenBrightness = newBrightness
                }
                viewModel.updateBrightnessGesture((newBrightness * 100).toInt())
            },
            onBrightnessEnd = { viewModel.finishBrightnessGesture() },
            onVolumeDrag = { deltaPercent ->
                audioManager?.let { am ->
                    val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val newVol = (curVol + (deltaPercent / 10f)).toInt().coerceIn(0, maxVol)
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                    val percent = (newVol.toFloat() / maxVol.toFloat() * 100).toInt()
                    viewModel.updateVolumeGesture(percent)
                }
            },
            onVolumeEnd = { viewModel.finishVolumeGesture() },
            onSeekScrubDrag = { deltaPixels ->
                val dur = playbackState.durationMs
                if (dur > 0) {
                    val deltaMs = (deltaPixels * 150).toLong()
                    val targetMs = (playbackState.currentPositionMs + deltaMs).coerceIn(0L, dur)
                    viewModel.updateSeekScrubGesture(targetMs, deltaMs)
                }
            },
            onSeekScrubEnd = { viewModel.finishSeekScrubGesture(executeSeek = true) },
            onZoomAndPan = { zoomChange, panX, panY ->
                viewModel.updateZoomAndPan(zoomChange, panX, panY)
            },
            modifier = Modifier.fillMaxSize()
        )

        // HUD Overlays (Brightness, Volume, Scrub, Double Tap Seek, Zoom Reset)
        PlayerHudOverlays(
            gestureState = gestureState,
            zoomScale = zoomScale,
            onResetZoom = { viewModel.resetZoom() },
            modifier = Modifier.fillMaxSize()
        )

        // Main Controls Overlay (Top Bar, Center Play/Pause, Bottom Seekbar & Chips)
        PlayerControlsOverlay(
            playbackState = playbackState,
            onBack = onBack,
            onPlayPause = { viewModel.togglePlayPause() },
            onSeek = { viewModel.seekTo(it) },
            onSeekForward = { viewModel.seekForward(10000L) },
            onSeekBackward = { viewModel.seekBackward(10000L) },
            onToggleLock = { viewModel.toggleControlsLock() },
            onToggleNerdStats = { viewModel.toggleDeveloperOverlay() },
            onOpenSpeedSheet = { viewModel.showSheet(QuickActionSheet.SPEED) },
            onOpenAspectSheet = { viewModel.showSheet(QuickActionSheet.ASPECT_RATIO) },
            onOpenAudioSubtitleSheet = { viewModel.showSheet(QuickActionSheet.AUDIO_SUBTITLES) },
            modifier = Modifier.fillMaxSize()
        )

        // Nerd Stats / Technical HUD Overlay (Floating window)
        PerformanceOverlay(
            metrics = performanceMetrics,
            isVisible = playbackState.isDeveloperOverlayVisible,
            onClose = { viewModel.toggleDeveloperOverlay() },
            onToggleDecoder = { viewModel.toggleHardwareSoftwareDecoder() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 16.dp)
        )

        // Error message card (if any playback/decoder error)
        playbackState.errorMessage?.let { errorMsg ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp, start = 24.dp, end = 24.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE2A0808)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMsg,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.toggleHardwareSoftwareDecoder() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Switch Decoder Engine", color = CyanNeon, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Bottom Sheets for quick adjustments
        when (activeSheet) {
            QuickActionSheet.SPEED -> {
                PlaybackSpeedSheet(
                    currentSpeed = playbackState.playbackSpeed,
                    onSelectSpeed = { viewModel.setPlaybackSpeed(it) },
                    onDismiss = { viewModel.showSheet(QuickActionSheet.NONE) }
                )
            }
            QuickActionSheet.ASPECT_RATIO -> {
                AspectRatioSheet(
                    currentMode = playbackState.aspectRatioMode,
                    onSelectMode = { viewModel.setAspectRatio(it) },
                    onDismiss = { viewModel.showSheet(QuickActionSheet.NONE) }
                )
            }
            QuickActionSheet.AUDIO_SUBTITLES -> {
                AudioSubtitleSheet(
                    audioTracks = audioTracks,
                    activeAudioIndex = playbackState.activeAudioTrackIndex,
                    subtitleTracks = subtitleTracks,
                    activeSubtitleIndex = playbackState.activeSubtitleTrackIndex,
                    audioDelayMs = playbackState.audioDelayMs,
                    onSelectAudio = { viewModel.selectAudioTrack(it) },
                    onSelectSubtitle = { viewModel.selectSubtitleTrack(it) },
                    onAudioDelayChanged = { viewModel.setAudioDelay(it) },
                    onDismiss = { viewModel.showSheet(QuickActionSheet.NONE) }
                )
            }
            else -> Unit
        }
    }
}
