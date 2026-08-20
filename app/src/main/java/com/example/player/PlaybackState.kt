package com.example.player

import com.example.decoder.DecoderType
import com.example.media.VideoItem

enum class AspectRatioMode(val displayName: String) {
    FIT("Fit to Screen"),
    CROP("Fill / Crop"),
    STRETCH("Stretch"),
    ORIGINAL("Original 1:1")
}

data class PlaybackState(
    val currentVideo: VideoItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val brightness: Float = 0.5f,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val areControlsLocked: Boolean = false,
    val isControlsVisible: Boolean = true,
    val isDeveloperOverlayVisible: Boolean = false,
    val selectedDecoderType: DecoderType = DecoderType.HARDWARE_MEDIACODEC,
    val decoderExplanation: String = "Hardware Decoder Active",
    val isHardwareAccelerated: Boolean = true,
    val activeAudioTrackIndex: Int = 0,
    val activeSubtitleTrackIndex: Int = -1, // -1 means off
    val audioDelayMs: Long = 0L,
    val errorMessage: String? = null
)

interface PlayerController {
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun seekForward(offsetMs: Long = 10000L)
    fun seekBackward(offsetMs: Long = 10000L)
    fun setPlaybackSpeed(speed: Float)
    fun setVolume(volume: Float)
    fun setBrightness(brightness: Float)
    fun setAspectRatio(mode: AspectRatioMode)
    fun toggleControlsLock()
    fun toggleDeveloperOverlay()
    fun selectAudioTrack(index: Int)
    fun selectSubtitleTrack(index: Int)
    fun setAudioDelay(delayMs: Long)
    fun retryWithFallbackDecoder()
    fun release()
}
