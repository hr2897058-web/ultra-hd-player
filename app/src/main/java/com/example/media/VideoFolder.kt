package com.example.media

import android.net.Uri

data class VideoFolder(
    val bucketId: String,
    val name: String,
    val path: String,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val representativeVideoUri: Uri? = null,
    val representativeVideoId: Long = 0L
)

data class PlaybackHistory(
    val videoId: Long,
    val positionMs: Long,
    val durationMs: Long,
    val timestampMs: Long,
    val completed: Boolean
)
