package com.example.media

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.Badge1080p
import com.example.ui.theme.Badge16K
import com.example.ui.theme.Badge4K
import com.example.ui.theme.Badge720p
import com.example.ui.theme.Badge8K
import com.example.ui.theme.TextMuted

enum class ResolutionCategory(
    val label: String,
    val minWidth: Int,
    val minHeight: Int,
    val badgeColor: Color,
    val isUltraHighRes: Boolean
) {
    ULTRA_16K("16K", 11520, 6480, Badge16K, true),
    UHD_8K("8K", 5760, 3240, Badge8K, true),
    UHD_4K("4K", 3000, 1800, Badge4K, true),
    QHD_1440P("2K / 1440p", 2000, 1300, Badge4K, false),
    FHD_1080P("1080p", 1600, 900, Badge1080p, false),
    HD_720P("720p", 1100, 650, Badge720p, false),
    SD("SD", 0, 0, TextMuted, false),
    UNKNOWN("Unknown", 0, 0, TextMuted, false);

    companion object {
        fun fromDimensions(width: Int, height: Int): ResolutionCategory {
            val maxDim = maxOf(width, height)
            val minDim = minOf(width, height)
            return when {
                maxDim >= 11520 || minDim >= 6480 -> ULTRA_16K
                maxDim >= 5760 || minDim >= 3240 -> UHD_8K
                maxDim >= 3000 || minDim >= 1800 -> UHD_4K
                maxDim >= 2000 || minDim >= 1300 -> QHD_1440P
                maxDim >= 1600 || minDim >= 900 -> FHD_1080P
                maxDim >= 1100 || minDim >= 650 -> HD_720P
                maxDim > 0 && minDim > 0 -> SD
                else -> UNKNOWN
            }
        }
    }
}
