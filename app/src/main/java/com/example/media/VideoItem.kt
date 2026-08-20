package com.example.media

import android.net.Uri

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val displayName: String,
    val path: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val resolutionCategory: ResolutionCategory,
    val mimeType: String,
    val codec: String,
    val frameRate: Float = 0f,
    val bitrateBps: Long = 0L,
    val hdrType: String? = null,
    val audioChannels: Int = 2,
    val audioCodec: String = "AAC",
    val containerFormat: String = "MP4",
    val folderName: String = "Videos",
    val bucketId: String = "",
    val dateAddedMs: Long = System.currentTimeMillis(),
    val dateModifiedMs: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val lastPlayedPositionMs: Long = 0L,
    val lastPlayedTimestampMs: Long = 0L,
    val isHardwareDecodable: Boolean = true,
    val recommendedDecoderMode: String = "Hardware (MediaCodec)"
) {
    val resolutionString: String
        get() = if (width > 0 && height > 0) "${width}×${height}" else "Unknown"

    val progressFraction: Float
        get() = if (durationMs > 0 && lastPlayedPositionMs > 0) {
            (lastPlayedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val isPartiallyPlayed: Boolean
        get() = lastPlayedPositionMs > 1000L && progressFraction < 0.95f

    val isCompleted: Boolean
        get() = progressFraction >= 0.95f

    val isHdr: Boolean
        get() = !hdrType.isNullOrEmpty()
}
