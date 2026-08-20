package com.example.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: Long,
    val uriString: String,
    val title: String,
    val displayName: String,
    val path: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val resolutionCategory: String,
    val mimeType: String,
    val codec: String,
    val frameRate: Float,
    val bitrateBps: Long,
    val hdrType: String?,
    val audioChannels: Int,
    val audioCodec: String,
    val containerFormat: String,
    val folderName: String,
    val bucketId: String,
    val dateAddedMs: Long,
    val dateModifiedMs: Long,
    val lastPlayedPositionMs: Long = 0L,
    val lastPlayedTimestampMs: Long = 0L,
    val isHardwareDecodable: Boolean = true,
    val recommendedDecoderMode: String = "Hardware"
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey val videoId: Long,
    val positionMs: Long,
    val durationMs: Long,
    val timestampMs: Long,
    val completed: Boolean = false
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val videoId: Long,
    val addedTimestampMs: Long = System.currentTimeMillis()
)
