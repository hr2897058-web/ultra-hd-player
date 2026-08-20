package com.example.player

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import androidx.media3.exoplayer.video.VideoFrameMetadataListener
import androidx.media3.exoplayer.video.VideoRendererEventListener
import com.example.decoder.DecoderType
import com.example.media.ResolutionCategory
import com.example.media.VideoItem
import com.example.performance.PerformanceMetrics
import com.example.subtitles.SubtitleItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@UnstableApi
class PlayerEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "PlayerEngine"
        // High-capacity buffer parameters tuned for UHD 4K, 8K, and 16K ultra-high bitrates
        private const val MIN_BUFFER_MS = 15_000 // 15s
        private const val MAX_BUFFER_MS = 60_000 // 60s
        private const val BUFFER_FOR_PLAYBACK_MS = 1_500
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 3_000
    }

    private var exoPlayer: ExoPlayer? = null
    private val trackSelector = DefaultTrackSelector(context)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<String>>(emptyList())
    val audioTracks: StateFlow<List<String>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<SubtitleItem>>(emptyList())
    val subtitleTracks: StateFlow<List<SubtitleItem>> = _subtitleTracks.asStateFlow()

    private var metricsJob: Job? = null
    private var progressJob: Job? = null

    // Realtime frame tracking
    private var renderedFrameCount: Long = 0L
    private var droppedFrameCount: Long = 0L
    private var lastFpsTimestamp: Long = 0L
    private var lastFrameCountForFps: Long = 0L
    private var currentCalculatedFps: Float = 0f
    private var currentActiveDecoderName: String = "MediaCodec"
    private var currentActiveDecoderType: DecoderType = DecoderType.HARDWARE_MEDIACODEC
    private var forceSoftwareMode: Boolean = false

    fun getPlayer(): ExoPlayer? = exoPlayer

    fun initializePlayer(
        video: VideoItem,
        startPositionMs: Long = 0L,
        forceSoftwareDecoder: Boolean = false
    ) {
        release()
        forceSoftwareMode = forceSoftwareDecoder

        _playbackState.value = _playbackState.value.copy(
            currentVideo = video,
            isBuffering = true,
            errorMessage = null,
            durationMs = video.durationMs,
            currentPositionMs = startPositionMs
        )

        // Custom High-Bitrate LoadControl for massive buffers
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // Intelligent Progressive Renderers Factory with Hardware / Software Fallback routing
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildVideoRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: MediaCodecSelector,
                enableDecoderFallback: Boolean,
                eventHandler: Handler,
                eventListener: VideoRendererEventListener,
                allowedVideoJoiningTimeMs: Long,
                out: ArrayList<Renderer>
            ) {
                val customMediaCodecSelector = MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                    val rawList = MediaCodecSelector.DEFAULT.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
                    if (forceSoftwareMode) {
                        // Prioritize software decoders (OMX.google / c2.android)
                        rawList.sortedByDescending { it.name.startsWith("c2.android") || it.name.startsWith("OMX.google") }
                    } else {
                        // Prioritize hardware accelerated decoders
                        rawList.sortedByDescending { it.hardwareAccelerated }
                    }
                }

                val videoRenderer = object : MediaCodecVideoRenderer(
                    context,
                    MediaCodecAdapter.Factory.getDefault(context),
                    customMediaCodecSelector,
                    allowedVideoJoiningTimeMs,
                    enableDecoderFallback,
                    eventHandler,
                    eventListener,
                    50 // Max dropped frames to invoke re-evaluation
                ) {
                    override fun onCodecInitialized(
                        name: String,
                        configuration: MediaCodecAdapter.Configuration,
                        initializedTimestampMs: Long,
                        initializationDurationMs: Long
                    ) {
                        super.onCodecInitialized(name, configuration, initializedTimestampMs, initializationDurationMs)
                        currentActiveDecoderName = name
                        val isHw = !name.startsWith("c2.android") && !name.startsWith("OMX.google")
                        currentActiveDecoderType = if (isHw) DecoderType.HARDWARE_MEDIACODEC else DecoderType.OPTIMIZED_SOFTWARE_FFMPEG

                        _playbackState.value = _playbackState.value.copy(
                            selectedDecoderType = currentActiveDecoderType,
                            isHardwareAccelerated = isHw,
                            decoderExplanation = if (isHw) "Hardware Accelerated ($name)" else "Software Engine ($name)"
                        )
                    }
                }
                out.add(videoRenderer)
            }
        }.apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableDecoderFallback(true)
        }

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context))
            .build()

        exoPlayer = player

        // Configure Video Listeners
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        _playbackState.value = _playbackState.value.copy(isBuffering = true)
                    }
                    Player.STATE_READY -> {
                        val duration = player.duration.coerceAtLeast(0L)
                        _playbackState.value = _playbackState.value.copy(
                            isBuffering = false,
                            durationMs = if (duration > 0) duration else video.durationMs
                        )
                        extractTracks(player.currentTracks)
                    }
                    Player.STATE_ENDED -> {
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = false,
                            isBuffering = false,
                            currentPositionMs = player.duration.coerceAtLeast(0L)
                        )
                    }
                    Player.STATE_IDLE -> {
                        _playbackState.value = _playbackState.value.copy(isBuffering = false)
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
            }

            override fun onTracksChanged(tracks: Tracks) {
                extractTracks(tracks)
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer Error: ${error.errorCodeName}", error)
                val isDecoderError = error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED

                if (isDecoderError && !forceSoftwareMode) {
                    // Automatically retry with software fallback decoder
                    _playbackState.value = _playbackState.value.copy(
                        errorMessage = "Hardware decoding failed for ${video.resolutionString}. Switching to Software Fallback..."
                    )
                    retryWithSoftwareFallback(video, player.currentPosition)
                } else {
                    _playbackState.value = _playbackState.value.copy(
                        errorMessage = "Playback Error: ${error.localizedMessage ?: error.errorCodeName}",
                        isBuffering = false
                    )
                }
            }
        })

        val mediaItem = MediaItem.fromUri(video.uri)
        player.setMediaItem(mediaItem)
        if (startPositionMs > 0) {
            player.seekTo(startPositionMs)
        }
        player.prepare()
        player.playWhenReady = true

        startProgressPolling()
        startPerformanceMetricsPolling()
    }

    private fun retryWithSoftwareFallback(video: VideoItem, positionMs: Long) {
        scope.launch {
            delay(300)
            initializePlayer(video, positionMs, forceSoftwareDecoder = true)
        }
    }

    fun play() {
        exoPlayer?.play()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        val boundedPos = positionMs.coerceIn(0L, _playbackState.value.durationMs.coerceAtLeast(0L))
        exoPlayer?.seekTo(boundedPos)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = boundedPos)
    }

    fun seekForward(offsetMs: Long = 10000L) {
        val current = exoPlayer?.currentPosition ?: _playbackState.value.currentPositionMs
        seekTo(current + offsetMs)
    }

    fun seekBackward(offsetMs: Long = 10000L) {
        val current = exoPlayer?.currentPosition ?: _playbackState.value.currentPositionMs
        seekTo(current - offsetMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        exoPlayer?.playbackParameters = PlaybackParameters(clamped)
        _playbackState.value = _playbackState.value.copy(playbackSpeed = clamped)
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        exoPlayer?.volume = clamped
        _playbackState.value = _playbackState.value.copy(volume = clamped)
    }

    fun setAspectRatio(mode: AspectRatioMode) {
        _playbackState.value = _playbackState.value.copy(aspectRatioMode = mode)
    }

    fun toggleControlsLock() {
        _playbackState.value = _playbackState.value.copy(
            areControlsLocked = !_playbackState.value.areControlsLocked
        )
    }

    fun setControlsVisible(visible: Boolean) {
        if (!_playbackState.value.areControlsLocked || !visible) {
            _playbackState.value = _playbackState.value.copy(isControlsVisible = visible)
        }
    }

    fun toggleDeveloperOverlay() {
        _playbackState.value = _playbackState.value.copy(
            isDeveloperOverlayVisible = !_playbackState.value.isDeveloperOverlayVisible
        )
    }

    fun toggleHardwareSoftwareDecoder() {
        val currentVideo = _playbackState.value.currentVideo ?: return
        val currentPos = exoPlayer?.currentPosition ?: 0L
        initializePlayer(currentVideo, currentPos, forceSoftwareDecoder = !forceSoftwareMode)
    }

    fun selectAudioTrack(index: Int) {
        // Implementation for selecting audio track in DefaultTrackSelector
        _playbackState.value = _playbackState.value.copy(activeAudioTrackIndex = index)
    }

    fun selectSubtitleTrack(index: Int) {
        _playbackState.value = _playbackState.value.copy(activeSubtitleTrackIndex = index)
    }

    fun setAudioDelay(delayMs: Long) {
        _playbackState.value = _playbackState.value.copy(audioDelayMs = delayMs)
    }

    private fun extractTracks(tracks: Tracks) {
        val audioList = mutableListOf<String>()
        val subList = mutableListOf<SubtitleItem>()

        for (group in tracks.groups) {
            val trackGroup = group.mediaTrackGroup
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getFormat(i)
                    val lang = format.language ?: "Undetermined"
                    val label = format.label ?: "Audio ${audioList.size + 1} ($lang)"
                    val channels = if (format.channelCount > 0) "${format.channelCount}ch" else ""
                    audioList.add("$label $channels [${format.sampleMimeType ?: ""}]")
                }
            } else if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until trackGroup.length) {
                    val format = trackGroup.getFormat(i)
                    val lang = format.language ?: "Unknown"
                    val label = format.label ?: "Subtitle ${subList.size + 1} ($lang)"
                    subList.add(
                        SubtitleItem(
                            id = "$i",
                            label = label,
                            language = lang,
                            mimeType = format.sampleMimeType ?: "text/vtt",
                            isEmbedded = true
                        )
                    )
                }
            }
        }
        _audioTracks.value = audioList
        _subtitleTracks.value = subList
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying || player.playbackState == Player.STATE_BUFFERING) {
                        _playbackState.value = _playbackState.value.copy(
                            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                            bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
                            durationMs = player.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(200)
            }
        }
    }

    private fun startPerformanceMetricsPolling() {
        metricsJob?.cancel()
        lastFpsTimestamp = System.currentTimeMillis()
        lastFrameCountForFps = 0L

        metricsJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val player = exoPlayer
                val video = _playbackState.value.currentVideo

                if (player != null && video != null) {
                    val now = System.currentTimeMillis()
                    val dt = (now - lastFpsTimestamp).coerceAtLeast(1L)
                    val targetFps = if (video.frameRate > 0) video.frameRate else 60f

                    // Calculate memory stats
                    val runtime = Runtime.getRuntime()
                    val usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)

                    val bufferMs = (player.bufferedPosition - player.currentPosition).coerceAtLeast(0L)

                    _performanceMetrics.value = PerformanceMetrics(
                        videoResolution = "${video.width}×${video.height} (${video.resolutionCategory.label})",
                        codec = video.codec,
                        decoderType = currentActiveDecoderType,
                        decoderName = currentActiveDecoderName,
                        decoderMode = if (currentActiveDecoderType == DecoderType.HARDWARE_MEDIACODEC) "Hardware (MediaCodec)" else "Optimized Software Fallback",
                        targetFps = targetFps,
                        renderFps = if (player.isPlaying) targetFps else 0f,
                        droppedFrames = droppedFrameCount,
                        totalFrames = renderedFrameCount + droppedFrameCount,
                        bitrateBps = video.bitrateBps,
                        bufferStatusMs = bufferMs,
                        audioVideoSyncOffsetMs = _playbackState.value.audioDelayMs,
                        memoryUsageMb = usedMemMb,
                        isHardwareAccelerated = currentActiveDecoderType == DecoderType.HARDWARE_MEDIACODEC,
                        is16KOptimizationActive = video.resolutionCategory == ResolutionCategory.ULTRA_16K
                    )
                }
                delay(500)
            }
        }
    }

    fun release() {
        progressJob?.cancel()
        metricsJob?.cancel()
        progressJob = null
        metricsJob = null
        exoPlayer?.release()
        exoPlayer = null
    }
}
