package com.example.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

object FileUtils {

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var name = "Video"
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex) ?: name
                    }
                }
            }
        } else if (uri.scheme == "file") {
            name = uri.lastPathSegment ?: File(uri.path ?: "").name
        }
        return name
    }

    fun getExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot >= 0 && lastDot < fileName.length - 1) {
            fileName.substring(lastDot + 1).uppercase()
        } else {
            "MP4"
        }
    }
}
