package com.example.ui.player

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.media.VideoItem
import com.example.player.AspectRatioMode
import com.example.player.PlaybackState
import com.example.player.PlayerEngine
import com.example.performance.PerformanceMetrics
import com.example.settings.PlayerPreferences
import com.example.storage.LocalVideoRepository
import com.example.subtitles.SubtitleItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class QuickActionSheet {
    NONE,
    SPEED,
    ASPECT_RATIO,
    AUDIO_SUBTITLES,
    SETTINGS
}

data class GestureOverlayState(
    val isBrightnessAdjusting: Boolean = false,
    val brightnessPercent: Int = 50,
    val isVolumeAdjusting: Boolean = false,
    val volumePercent: Int = 50,
    val isSeekScrubbing: Boolean = false,
    val seekTargetMs: Long = 0L,
    val seekDeltaMs: Long = 0L,
    val doubleTapSeekSide: String? = null // "left", "right"
)

@OptIn(UnstableApi::class)
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocalVideoRepository(application)
    private val preferences = PlayerPreferences(application)

    private val playerEngine = PlayerEngine(application, viewModelScope)

    val playbackState: StateFlow<PlaybackState> = playerEngine.playbackState
    val performanceMetrics: StateFlow<PerformanceMetrics> = playerEngine.performanceMetrics
    val audioTracks: StateFlow<List<String>> = playerEngine.audioTracks
    val subtitleTracks: StateFlow<List<SubtitleItem>> = playerEngine.subtitleTracks

    private val _gestureState = MutableStateFlow(GestureOverlayState())
    val gestureState: StateFlow<GestureOverlayState> = _gestureState.asStateFlow()

    private val _activeSheet = MutableStateFlow(QuickActionSheet.NONE)
    val activeSheet: StateFlow<QuickActionSheet> = _activeSheet.asStateFlow()

    // Interactive Zoom / Pan State for inspecting 4K/8K/16K detail
    private val _zoomScale = MutableStateFlow(1.0f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _panOffsetX = MutableStateFlow(0f)
    val panOffsetX: StateFlow<Float> = _panOffsetX.asStateFlow()

    private val _panOffsetY = MutableStateFlow(0f)
    val panOffsetY: StateFlow<Float> = _panOffsetY.asStateFlow()

    private var autoHideControlsJob: Job? = null
    private var progressPersistenceJob: Job? = null

    fun getPlayerEngine(): PlayerEngine = playerEngine

    fun loadVideo(video: VideoItem) {
        val startPosition = if (preferences.resumePlayback && video.lastPlayedPositionMs > 0 && !video.isCompleted) {
            video.lastPlayedPositionMs
        } else {
            0L
        }

        val defaultAspect = try {
            AspectRatioMode.valueOf(preferences.defaultAspectRatio)
        } catch (e: Exception) {
            AspectRatioMode.FIT
        }

        playerEngine.initializePlayer(
            video = video,
            startPositionMs = startPosition,
            forceSoftwareDecoder = !preferences.isHardwareAccelerationEnabled
        )

        playerEngine.setAspectRatio(defaultAspect)

        if (preferences.isShowPerformanceOverlayByDefault) {
            playerEngine.toggleDeveloperOverlay()
        }

        startAutoHideTimer()
        startPeriodicProgressSaving()
    }

    fun play() {
        playerEngine.play()
        startAutoHideTimer()
    }

    fun pause() {
        playerEngine.pause()
        saveCurrentProgress()
    }

    fun togglePlayPause() {
        if (playbackState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        playerEngine.seekTo(positionMs)
        startAutoHideTimer()
    }

    fun seekForward(offsetMs: Long = 10000L) {
        playerEngine.seekForward(offsetMs)
        showDoubleTapSeek("right")
        startAutoHideTimer()
    }

    fun seekBackward(offsetMs: Long = 10000L) {
        playerEngine.seekBackward(offsetMs)
        showDoubleTapSeek("left")
        startAutoHideTimer()
    }

    fun setPlaybackSpeed(speed: Float) {
        playerEngine.setPlaybackSpeed(speed)
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        playerEngine.setAspectRatio(mode)
        preferences.defaultAspectRatio = mode.name
    }

    fun setVolume(vol: Float) {
        playerEngine.setVolume(vol)
    }

    fun toggleControlsLock() {
        playerEngine.toggleControlsLock()
        if (!playbackState.value.areControlsLocked) {
            startAutoHideTimer()
        }
    }

    fun toggleDeveloperOverlay() {
        playerEngine.toggleDeveloperOverlay()
    }

    fun toggleHardwareSoftwareDecoder() {
        playerEngine.toggleHardwareSoftwareDecoder()
    }

    fun selectAudioTrack(index: Int) {
        playerEngine.selectAudioTrack(index)
    }

    fun selectSubtitleTrack(index: Int) {
        playerEngine.selectSubtitleTrack(index)
    }

    fun setAudioDelay(delayMs: Long) {
        playerEngine.setAudioDelay(delayMs)
    }

    fun showSheet(sheet: QuickActionSheet) {
        _activeSheet.value = sheet
        if (sheet != QuickActionSheet.NONE) {
            playerEngine.setControlsVisible(true)
            autoHideControlsJob?.cancel()
        } else {
            startAutoHideTimer()
        }
    }

    fun onUserInteraction() {
        if (!playbackState.value.areControlsLocked) {
            val wasVisible = playbackState.value.isControlsVisible
            playerEngine.setControlsVisible(!wasVisible)
            if (!wasVisible) {
                startAutoHideTimer()
            } else {
                autoHideControlsJob?.cancel()
            }
        } else {
            // Flash unlock icon if controls locked
            playerEngine.setControlsVisible(true)
            startAutoHideTimer(durationMs = 2000L)
        }
    }

    fun updateBrightnessGesture(percent: Int) {
        _gestureState.value = _gestureState.value.copy(
            isBrightnessAdjusting = true,
            brightnessPercent = percent.coerceIn(0, 100)
        )
    }

    fun finishBrightnessGesture() {
        viewModelScope.launch {
            delay(500)
            _gestureState.value = _gestureState.value.copy(isBrightnessAdjusting = false)
        }
    }

    fun updateVolumeGesture(percent: Int) {
        _gestureState.value = _gestureState.value.copy(
            isVolumeAdjusting = true,
            volumePercent = percent.coerceIn(0, 100)
        )
        setVolume(percent / 100f)
    }

    fun finishVolumeGesture() {
        viewModelScope.launch {
            delay(500)
            _gestureState.value = _gestureState.value.copy(isVolumeAdjusting = false)
        }
    }

    fun updateSeekScrubGesture(targetPositionMs: Long, deltaMs: Long) {
        _gestureState.value = _gestureState.value.copy(
            isSeekScrubbing = true,
            seekTargetMs = targetPositionMs,
            seekDeltaMs = deltaMs
        )
    }

    fun finishSeekScrubGesture(executeSeek: Boolean = true) {
        if (executeSeek) {
            seekTo(_gestureState.value.seekTargetMs)
        }
        _gestureState.value = _gestureState.value.copy(isSeekScrubbing = false)
    }

    fun updateZoomAndPan(zoomChange: Float, panChangeX: Float, panChangeY: Float) {
        val newScale = (_zoomScale.value * zoomChange).coerceIn(1.0f, 5.0f)
        _zoomScale.value = newScale

        if (newScale <= 1.05f) {
            _panOffsetX.value = 0f
            _panOffsetY.value = 0f
            _zoomScale.value = 1.0f
        } else {
            val maxPan = 500f * (newScale - 1f)
            _panOffsetX.value = (_panOffsetX.value + panChangeX).coerceIn(-maxPan, maxPan)
            _panOffsetY.value = (_panOffsetY.value + panChangeY).coerceIn(-maxPan, maxPan)
        }
    }

    fun resetZoom() {
        _zoomScale.value = 1.0f
        _panOffsetX.value = 0f
        _panOffsetY.value = 0f
    }

    private fun showDoubleTapSeek(side: String) {
        _gestureState.value = _gestureState.value.copy(doubleTapSeekSide = side)
        viewModelScope.launch {
            delay(650)
            if (_gestureState.value.doubleTapSeekSide == side) {
                _gestureState.value = _gestureState.value.copy(doubleTapSeekSide = null)
            }
        }
    }

    private fun startAutoHideTimer(durationMs: Long = 4000L) {
        autoHideControlsJob?.cancel()
        autoHideControlsJob = viewModelScope.launch {
            delay(durationMs)
            if (playbackState.value.isPlaying && _activeSheet.value == QuickActionSheet.NONE) {
                playerEngine.setControlsVisible(false)
            }
        }
    }

    private fun startPeriodicProgressSaving() {
        progressPersistenceJob?.cancel()
        progressPersistenceJob = viewModelScope.launch {
            while (isActive) {
                delay(4000)
                saveCurrentProgress()
            }
        }
    }

    private fun saveCurrentProgress() {
        val currentVideo = playbackState.value.currentVideo ?: return
        val pos = playbackState.value.currentPositionMs
        val dur = playbackState.value.durationMs
        if (dur > 0 && pos > 0) {
            viewModelScope.launch {
                repository.updatePlaybackProgress(currentVideo.id, pos, dur)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveCurrentProgress()
        playerEngine.release()
    }
}
