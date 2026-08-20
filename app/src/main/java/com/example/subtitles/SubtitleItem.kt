package com.example.subtitles

import android.net.Uri

data class SubtitleItem(
    val id: String,
    val label: String,
    val language: String,
    val mimeType: String,
    val isEmbedded: Boolean,
    val uri: Uri? = null
)
