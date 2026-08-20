package com.example.storage

import android.content.Context
import android.net.Uri
import com.example.media.PlaybackHistory
import com.example.media.ResolutionCategory
import com.example.media.VideoFolder
import com.example.media.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

enum class VideoSortOrder(val displayName: String) {
    DATE_DESC("Date: Newest first"),
    DATE_ASC("Date: Oldest first"),
    NAME_ASC("Name: A to Z"),
    NAME_DESC("Name: Z to A"),
    SIZE_DESC("Size: Largest first"),
    SIZE_ASC("Size: Smallest first"),
    RESOLUTION_DESC("Resolution: Highest (16K/8K/4K)"),
    DURATION_DESC("Duration: Longest first")
}

class LocalVideoRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context),
    private val mediaScanner: MediaScanner = MediaScanner(context)
) {
    private val videoDao = database.videoDao()

    val allVideosFlow: Flow<List<VideoItem>> = combine(
        videoDao.getAllVideos(),
        videoDao.getFavoriteIds()
    ) { entities, favIds ->
        val favSet = favIds.toSet()
        entities.map { entity ->
            val resCategory = try {
                ResolutionCategory.valueOf(entity.resolutionCategory)
            } catch (e: Exception) {
                ResolutionCategory.fromDimensions(entity.width, entity.height)
            }

            VideoItem(
                id = entity.id,
                uri = Uri.parse(entity.uriString),
                title = entity.title,
                displayName = entity.displayName,
                path = entity.path,
                durationMs = entity.durationMs,
                sizeBytes = entity.sizeBytes,
                width = entity.width,
                height = entity.height,
                resolutionCategory = resCategory,
                mimeType = entity.mimeType,
                codec = entity.codec,
                frameRate = entity.frameRate,
                bitrateBps = entity.bitrateBps,
                hdrType = entity.hdrType,
                audioChannels = entity.audioChannels,
                audioCodec = entity.audioCodec,
                containerFormat = entity.containerFormat,
                folderName = entity.folderName,
                bucketId = entity.bucketId,
                dateAddedMs = entity.dateAddedMs,
                dateModifiedMs = entity.dateModifiedMs,
                isFavorite = favSet.contains(entity.id),
                lastPlayedPositionMs = entity.lastPlayedPositionMs,
                lastPlayedTimestampMs = entity.lastPlayedTimestampMs,
                isHardwareDecodable = entity.isHardwareDecodable,
                recommendedDecoderMode = entity.recommendedDecoderMode
            )
        }
    }.flowOn(Dispatchers.IO)

    val foldersFlow: Flow<List<VideoFolder>> = allVideosFlow.map { videos ->
        videos.groupBy { it.bucketId.ifEmpty { it.folderName } }
            .map { (bucketId, folderVideos) ->
                val firstVideo = folderVideos.firstOrNull()
                VideoFolder(
                    bucketId = bucketId,
                    name = folderVideos.firstOrNull()?.folderName ?: "Folder",
                    path = folderVideos.firstOrNull()?.path ?: "",
                    videoCount = folderVideos.size,
                    totalSizeBytes = folderVideos.sumOf { it.sizeBytes },
                    representativeVideoUri = firstVideo?.uri,
                    representativeVideoId = firstVideo?.id ?: 0L
                )
            }
            .sortedByDescending { it.videoCount }
    }.flowOn(Dispatchers.IO)

    val recentlyPlayedFlow: Flow<List<VideoItem>> = allVideosFlow.map { videos ->
        videos.filter { it.lastPlayedTimestampMs > 0 }
            .sortedByDescending { it.lastPlayedTimestampMs }
    }.flowOn(Dispatchers.IO)

    val favoritesFlow: Flow<List<VideoItem>> = allVideosFlow.map { videos ->
        videos.filter { it.isFavorite }
            .sortedByDescending { it.dateAddedMs }
    }.flowOn(Dispatchers.IO)

    suspend fun refreshVideos() = withContext(Dispatchers.IO) {
        val scannedVideos = mediaScanner.scanMediaStore()
        if (scannedVideos.isNotEmpty()) {
            val entities = scannedVideos.map { item ->
                VideoEntity(
                    id = item.id,
                    uriString = item.uri.toString(),
                    title = item.title,
                    displayName = item.displayName,
                    path = item.path,
                    durationMs = item.durationMs,
                    sizeBytes = item.sizeBytes,
                    width = item.width,
                    height = item.height,
                    resolutionCategory = item.resolutionCategory.name,
                    mimeType = item.mimeType,
                    codec = item.codec,
                    frameRate = item.frameRate,
                    bitrateBps = item.bitrateBps,
                    hdrType = item.hdrType,
                    audioChannels = item.audioChannels,
                    audioCodec = item.audioCodec,
                    containerFormat = item.containerFormat,
                    folderName = item.folderName,
                    bucketId = item.bucketId,
                    dateAddedMs = item.dateAddedMs,
                    dateModifiedMs = item.dateModifiedMs,
                    isHardwareDecodable = item.isHardwareDecodable,
                    recommendedDecoderMode = item.recommendedDecoderMode
                )
            }
            videoDao.insertVideos(entities)
        }
    }

    suspend fun addImportedVideo(uri: Uri): VideoItem = withContext(Dispatchers.IO) {
        val item = mediaScanner.scanSingleDocumentUri(uri)
        val entity = VideoEntity(
            id = item.id,
            uriString = item.uri.toString(),
            title = item.title,
            displayName = item.displayName,
            path = item.path,
            durationMs = item.durationMs,
            sizeBytes = item.sizeBytes,
            width = item.width,
            height = item.height,
            resolutionCategory = item.resolutionCategory.name,
            mimeType = item.mimeType,
            codec = item.codec,
            frameRate = item.frameRate,
            bitrateBps = item.bitrateBps,
            hdrType = item.hdrType,
            audioChannels = item.audioChannels,
            audioCodec = item.audioCodec,
            containerFormat = item.containerFormat,
            folderName = item.folderName,
            bucketId = item.bucketId,
            dateAddedMs = item.dateAddedMs,
            dateModifiedMs = item.dateModifiedMs,
            isHardwareDecodable = item.isHardwareDecodable,
            recommendedDecoderMode = item.recommendedDecoderMode
        )
        videoDao.insertVideo(entity)
        item
    }

    suspend fun addImportedFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        val items = mediaScanner.scanDocumentTreeUri(treeUri)
        if (items.isNotEmpty()) {
            val entities = items.map { item ->
                VideoEntity(
                    id = item.id,
                    uriString = item.uri.toString(),
                    title = item.title,
                    displayName = item.displayName,
                    path = item.path,
                    durationMs = item.durationMs,
                    sizeBytes = item.sizeBytes,
                    width = item.width,
                    height = item.height,
                    resolutionCategory = item.resolutionCategory.name,
                    mimeType = item.mimeType,
                    codec = item.codec,
                    frameRate = item.frameRate,
                    bitrateBps = item.bitrateBps,
                    hdrType = item.hdrType,
                    audioChannels = item.audioChannels,
                    audioCodec = item.audioCodec,
                    containerFormat = item.containerFormat,
                    folderName = item.folderName,
                    bucketId = item.bucketId,
                    dateAddedMs = item.dateAddedMs,
                    dateModifiedMs = item.dateModifiedMs,
                    isHardwareDecodable = item.isHardwareDecodable,
                    recommendedDecoderMode = item.recommendedDecoderMode
                )
            }
            videoDao.insertVideos(entities)
        }
        items.size
    }

    suspend fun toggleFavorite(videoId: Long) = withContext(Dispatchers.IO) {
        if (videoDao.isFavorite(videoId)) {
            videoDao.removeFavorite(videoId)
        } else {
            videoDao.addFavorite(FavoriteEntity(videoId))
        }
    }

    suspend fun updatePlaybackProgress(videoId: Long, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        videoDao.updateVideoPlaybackPosition(videoId, positionMs, now)
        videoDao.updatePlaybackHistory(
            PlaybackHistoryEntity(
                videoId = videoId,
                positionMs = positionMs,
                durationMs = durationMs,
                timestampMs = now,
                completed = durationMs > 0 && (positionMs.toFloat() / durationMs) >= 0.95f
            )
        )
    }

    suspend fun getVideoById(videoId: Long): VideoItem? = withContext(Dispatchers.IO) {
        val entity = videoDao.getVideoById(videoId) ?: return@withContext null
        val isFav = videoDao.isFavorite(videoId)
        val resCat = try {
            ResolutionCategory.valueOf(entity.resolutionCategory)
        } catch (e: Exception) {
            ResolutionCategory.fromDimensions(entity.width, entity.height)
        }
        VideoItem(
            id = entity.id,
            uri = Uri.parse(entity.uriString),
            title = entity.title,
            displayName = entity.displayName,
            path = entity.path,
            durationMs = entity.durationMs,
            sizeBytes = entity.sizeBytes,
            width = entity.width,
            height = entity.height,
            resolutionCategory = resCat,
            mimeType = entity.mimeType,
            codec = entity.codec,
            frameRate = entity.frameRate,
            bitrateBps = entity.bitrateBps,
            hdrType = entity.hdrType,
            audioChannels = entity.audioChannels,
            audioCodec = entity.audioCodec,
            containerFormat = entity.containerFormat,
            folderName = entity.folderName,
            bucketId = entity.bucketId,
            dateAddedMs = entity.dateAddedMs,
            dateModifiedMs = entity.dateModifiedMs,
            isFavorite = isFav,
            lastPlayedPositionMs = entity.lastPlayedPositionMs,
            lastPlayedTimestampMs = entity.lastPlayedTimestampMs,
            isHardwareDecodable = entity.isHardwareDecodable,
            recommendedDecoderMode = entity.recommendedDecoderMode
        )
    }
}
