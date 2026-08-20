package com.example.storage

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.decoder.DecoderManager
import com.example.media.ResolutionCategory
import com.example.media.VideoItem
import com.example.metadata.VideoMetadataExtractor
import com.example.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaScanner(private val context: Context) {

    companion object {
        private const val TAG = "MediaScanner"
    }

    private val decoderManager = DecoderManager()

    suspend fun scanMediaStore(): List<VideoItem> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID
        )

        try {
            val cursor = context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val titleCol = it.getColumnIndex(MediaStore.Video.Media.TITLE)
                val dataCol = it.getColumnIndex(MediaStore.Video.Media.DATA)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthCol = it.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = it.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val mimeCol = it.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val dateAddedCol = it.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val dateModCol = it.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val bucketNameCol = it.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val bucketIdCol = it.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val name = it.getString(nameCol) ?: "Video_$id"
                    val title = if (titleCol != -1) it.getString(titleCol) ?: name else name
                    val path = if (dataCol != -1) it.getString(dataCol) ?: "" else ""
                    val duration = it.getLong(durationCol)
                    val size = it.getLong(sizeCol)
                    var width = if (widthCol != -1) it.getInt(widthCol) else 0
                    var height = if (heightCol != -1) it.getInt(heightCol) else 0
                    val mime = (if (mimeCol != -1) it.getString(mimeCol) else null) ?: "video/mp4"
                    val dateAdded = (if (dateAddedCol != -1) it.getLong(dateAddedCol) else 0L) * 1000L
                    val dateMod = (if (dateModCol != -1) it.getLong(dateModCol) else 0L) * 1000L
                    val folderName = (if (bucketNameCol != -1) it.getString(bucketNameCol) else null) ?: "Videos"
                    val bucketId = (if (bucketIdCol != -1) it.getString(bucketIdCol) else null) ?: folderName

                    var codec = when {
                        mime.contains("hevc", ignoreCase = true) -> "HEVC"
                        mime.contains("av01", ignoreCase = true) -> "AV1"
                        mime.contains("vp9", ignoreCase = true) -> "VP9"
                        else -> "H.264"
                    }

                    // If width and height are 0 or need verification, extract metadata
                    if (width <= 0 || height <= 0) {
                        try {
                            val meta = VideoMetadataExtractor.extractDetailedMetadata(context, contentUri, size)
                            width = meta.width
                            height = meta.height
                            codec = meta.videoCodecName
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to extract metadata for $contentUri", e)
                        }
                    }

                    val resolutionCategory = ResolutionCategory.fromDimensions(width, height)
                    val decoderResult = decoderManager.selectBestDecoder(mime, width, height)

                    videos.add(
                        VideoItem(
                            id = id,
                            uri = contentUri,
                            title = title,
                            displayName = name,
                            path = path,
                            durationMs = duration,
                            sizeBytes = size,
                            width = width,
                            height = height,
                            resolutionCategory = resolutionCategory,
                            mimeType = mime,
                            codec = codec,
                            containerFormat = FileUtils.getExtension(name),
                            folderName = folderName,
                            bucketId = bucketId,
                            dateAddedMs = if (dateAdded > 0) dateAdded else System.currentTimeMillis(),
                            dateModifiedMs = if (dateMod > 0) dateMod else System.currentTimeMillis(),
                            isHardwareDecodable = decoderResult.isHardwareAccelerated,
                            recommendedDecoderMode = decoderResult.selectedDecoder.displayName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning MediaStore", e)
        }

        videos
    }

    suspend fun scanSingleDocumentUri(uri: Uri): VideoItem = withContext(Dispatchers.IO) {
        val fileName = FileUtils.getFileNameFromUri(context, uri)
        val meta = VideoMetadataExtractor.extractDetailedMetadata(context, uri)

        val id = uri.hashCode().toLong()
        VideoItem(
            id = id,
            uri = uri,
            title = if (meta.title.isNotBlank()) meta.title else fileName,
            displayName = fileName,
            path = uri.toString(),
            durationMs = meta.durationMs,
            sizeBytes = meta.sizeBytes,
            width = meta.width,
            height = meta.height,
            resolutionCategory = meta.resolutionCategory,
            mimeType = meta.videoMimeType,
            codec = meta.videoCodecName,
            frameRate = meta.frameRate,
            bitrateBps = meta.bitrateBps,
            hdrType = meta.hdrType,
            audioChannels = meta.audioChannels,
            audioCodec = meta.audioCodec,
            containerFormat = meta.container,
            folderName = "Storage Access",
            bucketId = "saf_opened",
            dateAddedMs = System.currentTimeMillis(),
            dateModifiedMs = System.currentTimeMillis(),
            isHardwareDecodable = meta.isHardwareDecodable,
            recommendedDecoderMode = meta.recommendedDecoderMode
        )
    }

    suspend fun scanDocumentTreeUri(treeUri: Uri): List<VideoItem> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoItem>()
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val folderName = rootDoc.name ?: "Imported Folder"

        suspend fun scanDirectory(dir: DocumentFile) {
            val fileList = dir.listFiles()
            for (file in fileList) {
                if (file.isDirectory) {
                    scanDirectory(file)
                } else {
                    val mime = file.type
                    val name = file.name?.lowercase() ?: ""
                    val isVideo = mime?.startsWith("video/") == true ||
                            name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm") ||
                            name.endsWith(".mov") || name.endsWith(".avi") || name.endsWith(".ts")

                    if (isVideo) {
                        try {
                            val videoItem = scanSingleDocumentUri(file.uri).copy(
                                folderName = folderName,
                                bucketId = treeUri.toString()
                            )
                            videos.add(videoItem)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed scanning document tree item ${file.uri}", e)
                        }
                    }
                }
            }
        }

        scanDirectory(rootDoc)
        videos
    }
}
