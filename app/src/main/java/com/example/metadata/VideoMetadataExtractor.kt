package com.example.metadata

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.decoder.DecoderCapabilities
import com.example.decoder.DecoderManager
import com.example.media.ResolutionCategory
import com.example.media.VideoItem
import java.io.File

data class DetailedVideoMetadata(
    val title: String,
    val container: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val resolutionCategory: ResolutionCategory,
    val videoMimeType: String,
    val videoCodecName: String,
    val frameRate: Float,
    val bitrateBps: Long,
    val rotationDegrees: Int,
    val hdrType: String?,
    val colorStandard: String?,
    val colorTransfer: String?,
    val audioTrackCount: Int,
    val audioCodec: String,
    val audioChannels: Int,
    val audioSampleRate: Int,
    val subtitleTrackCount: Int,
    val isHardwareDecodable: Boolean,
    val recommendedDecoderMode: String
)

object VideoMetadataExtractor {
    private const val TAG = "VideoMetadataExtractor"
    private val decoderManager = DecoderManager()

    fun extractDetailedMetadata(context: Context, uri: Uri, fallbackSize: Long = 0L): DetailedVideoMetadata {
        val retriever = MediaMetadataRetriever()
        var width = 0
        var height = 0
        var durationMs = 0L
        var bitrateBps = 0L
        var rotation = 0
        var videoMime = "video/mp4"
        var videoCodec = "AVC / H.264"
        var audioCodec = "AAC"
        var audioChannels = 2
        var audioSampleRate = 48000
        var audioTrackCount = 0
        var subtitleTrackCount = 0
        var frameRate = 30f
        var hdrType: String? = null
        var colorStandard: String? = null
        var colorTransfer: String? = null
        var title = uri.lastPathSegment ?: "Video"

        try {
            retriever.setDataSource(context, uri)

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val mimeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            val titleStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)

            width = widthStr?.toIntOrNull() ?: 0
            height = heightStr?.toIntOrNull() ?: 0
            durationMs = durationStr?.toLongOrNull() ?: 0L
            bitrateBps = bitrateStr?.toLongOrNull() ?: 0L
            rotation = rotationStr?.toIntOrNull() ?: 0
            if (!mimeStr.isNullOrEmpty()) videoMime = mimeStr
            if (!titleStr.isNullOrEmpty()) title = titleStr

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val colorStd = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_STANDARD)
                val colorTr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_TRANSFER)
                colorStandard = colorStd
                colorTransfer = colorTr
                if (colorTr == "6" || colorTr == "7" || colorStd == "6") {
                    hdrType = "HDR10"
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaMetadataRetriever extraction partial error for $uri", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore release errors
            }
        }

        // Use MediaExtractor for deep track inspection (codec specifics, audio streams, frame rate)
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val trackCount = extractor.trackCount
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

                if (mime.startsWith("video/")) {
                    videoMime = mime
                    if (format.containsKey(MediaFormat.KEY_WIDTH)) {
                        val w = format.getInteger(MediaFormat.KEY_WIDTH)
                        if (w > width) width = w
                    }
                    if (format.containsKey(MediaFormat.KEY_HEIGHT)) {
                        val h = format.getInteger(MediaFormat.KEY_HEIGHT)
                        if (h > height) height = h
                    }
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
                    }
                    if (format.containsKey(MediaFormat.KEY_COLOR_STANDARD)) {
                        val std = format.getInteger(MediaFormat.KEY_COLOR_STANDARD)
                        if (std == MediaFormat.COLOR_STANDARD_BT2020) {
                            hdrType = hdrType ?: "HDR (BT.2020)"
                        }
                    }

                    videoCodec = parseCodecName(mime)
                } else if (mime.startsWith("audio/")) {
                    audioTrackCount++
                    audioCodec = parseAudioCodecName(mime)
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        audioChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        audioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                } else if (mime.startsWith("text/") || mime.contains("subtitle") || mime.contains("vtt")) {
                    subtitleTrackCount++
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaExtractor inspection failed for $uri", e)
        } finally {
            try {
                extractor.release()
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Adjust dimensions if rotated 90 or 270 degrees
        val effectiveWidth = if (rotation == 90 || rotation == 270) height else width
        val effectiveHeight = if (rotation == 90 || rotation == 270) width else height

        val resCategory = ResolutionCategory.fromDimensions(effectiveWidth, effectiveHeight)
        val decoderResult = decoderManager.selectBestDecoder(videoMime, effectiveWidth, effectiveHeight, frameRate)

        val container = when {
            uri.toString().endsWith(".mkv", ignoreCase = true) -> "MKV (Matroska)"
            uri.toString().endsWith(".webm", ignoreCase = true) -> "WebM"
            uri.toString().endsWith(".mov", ignoreCase = true) -> "QuickTime (MOV)"
            uri.toString().endsWith(".avi", ignoreCase = true) -> "AVI"
            uri.toString().endsWith(".ts", ignoreCase = true) -> "MPEG-TS"
            else -> "MP4 / ISO"
        }

        return DetailedVideoMetadata(
            title = title,
            container = container,
            durationMs = durationMs,
            sizeBytes = fallbackSize,
            width = effectiveWidth,
            height = effectiveHeight,
            resolutionCategory = resCategory,
            videoMimeType = videoMime,
            videoCodecName = videoCodec,
            frameRate = frameRate,
            bitrateBps = bitrateBps,
            rotationDegrees = rotation,
            hdrType = hdrType,
            colorStandard = colorStandard,
            colorTransfer = colorTransfer,
            audioTrackCount = maxOf(audioTrackCount, 1),
            audioCodec = audioCodec,
            audioChannels = audioChannels,
            audioSampleRate = audioSampleRate,
            subtitleTrackCount = subtitleTrackCount,
            isHardwareDecodable = decoderResult.isHardwareAccelerated,
            recommendedDecoderMode = decoderResult.selectedDecoder.displayName
        )
    }

    private fun parseCodecName(mime: String): String {
        return when {
            mime.contains("hevc", ignoreCase = true) || mime.contains("h265", ignoreCase = true) -> "HEVC / H.265"
            mime.contains("avc", ignoreCase = true) || mime.contains("h264", ignoreCase = true) -> "AVC / H.264"
            mime.contains("av01", ignoreCase = true) || mime.contains("av1", ignoreCase = true) -> "AV1"
            mime.contains("vp9", ignoreCase = true) -> "VP9"
            mime.contains("vp8", ignoreCase = true) -> "VP8"
            mime.contains("mp4v", ignoreCase = true) -> "MPEG-4"
            mime.contains("prores", ignoreCase = true) -> "Apple ProRes"
            else -> mime.removePrefix("video/").uppercase()
        }
    }

    private fun parseAudioCodecName(mime: String): String {
        return when {
            mime.contains("mp4a-latm", ignoreCase = true) || mime.contains("aac", ignoreCase = true) -> "AAC"
            mime.contains("ac3", ignoreCase = true) || mime.contains("eac3", ignoreCase = true) -> "Dolby Digital (AC3/EAC3)"
            mime.contains("opus", ignoreCase = true) -> "Opus"
            mime.contains("flac", ignoreCase = true) -> "FLAC Lossless"
            mime.contains("vorbis", ignoreCase = true) -> "Vorbis"
            mime.contains("raw", ignoreCase = true) -> "PCM Audio"
            else -> mime.removePrefix("audio/").uppercase()
        }
    }
}
