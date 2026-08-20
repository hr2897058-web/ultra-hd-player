package com.example.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Query("SELECT * FROM videos ORDER BY dateAddedMs DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :videoId")
    suspend fun getVideoById(videoId: Long): VideoEntity?

    @Query("SELECT * FROM videos WHERE uriString = :uriString LIMIT 1")
    suspend fun getVideoByUri(uriString: String): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id NOT IN (:currentIds)")
    suspend fun pruneDeletedVideos(currentIds: List<Long>)

    // Playback History
    @Query("SELECT * FROM playback_history ORDER BY timestampMs DESC LIMIT 50")
    fun getRecentPlaybackHistory(): Flow<List<PlaybackHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePlaybackHistory(history: PlaybackHistoryEntity)

    @Query("UPDATE videos SET lastPlayedPositionMs = :positionMs, lastPlayedTimestampMs = :timestampMs WHERE id = :videoId")
    suspend fun updateVideoPlaybackPosition(videoId: Long, positionMs: Long, timestampMs: Long)

    // Favorites
    @Query("SELECT videoId FROM favorites")
    fun getFavoriteIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE videoId = :videoId")
    suspend fun removeFavorite(videoId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE videoId = :videoId)")
    suspend fun isFavorite(videoId: Long): Boolean
}
