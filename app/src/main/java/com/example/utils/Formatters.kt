package com.example.utils

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object Formatters {

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return "00:00"
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val df = DecimalFormat("#,##0.#")
        return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
    }

    fun formatBitrate(bitrateBps: Long): String {
        if (bitrateBps <= 0) return "Unknown bitrate"
        val mbps = bitrateBps.toDouble() / (1000.0 * 1000.0)
        return if (mbps >= 1.0) {
            String.format(Locale.US, "%.1f Mbps", mbps)
        } else {
            val kbps = bitrateBps / 1000
            "$kbps Kbps"
        }
    }

    fun formatDate(timestampMs: Long): String {
        if (timestampMs <= 0) return ""
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }

    fun formatResolutionLabel(width: Int, height: Int): String {
        if (width <= 0 || height <= 0) return "Unknown"
        val maxDim = maxOf(width, height)
        val minDim = minOf(width, height)
        return when {
            maxDim >= 11520 || minDim >= 6480 -> "${width}×${height} (16K Ultra-Res)"
            maxDim >= 5760 || minDim >= 3240 -> "${width}×${height} (8K UHD)"
            maxDim >= 3000 || minDim >= 1800 -> "${width}×${height} (4K UHD)"
            maxDim >= 2000 || minDim >= 1300 -> "${width}×${height} (1440p 2K)"
            maxDim >= 1600 || minDim >= 900 -> "${width}×${height} (1080p FHD)"
            maxDim >= 1100 || minDim >= 650 -> "${width}×${height} (720p HD)"
            else -> "${width}×${height} (SD)"
        }
    }
}
