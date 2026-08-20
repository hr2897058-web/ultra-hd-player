package com.example.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.performance.PerformanceMetrics
import com.example.ui.theme.Badge16K
import com.example.ui.theme.Badge8K
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.utils.Formatters

@Composable
fun PerformanceOverlay(
    metrics: PerformanceMetrics,
    isVisible: Boolean,
    onClose: () -> Unit,
    onToggleDecoder: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .width(320.dp)
                .padding(16.dp)
                .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .testTag("performance_overlay_hud"),
            colors = CardDefaults.cardColors(containerColor = Color(0xEB0A0E17)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NERD STATS / HUD",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanNeon,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(24.dp).testTag("close_nerd_stats_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close HUD",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Stats Rows
                HudStatRow(label = "Stream Resolution", value = metrics.videoResolution)
                HudStatRow(label = "Codec", value = metrics.codec)
                HudStatRow(
                    label = "Active Decoder",
                    value = metrics.decoderName,
                    valueColor = if (metrics.isHardwareAccelerated) CyanNeon else Color(0xFFFFB74D)
                )
                HudStatRow(
                    label = "Decoder Mode",
                    value = metrics.decoderMode,
                    valueColor = if (metrics.isHardwareAccelerated) Color(0xFF4ADE80) else Color(0xFFFFB74D)
                )

                if (metrics.is16KOptimizationActive) {
                    Surface(
                        color = Badge16K.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "⚡ 16K Ultra-Res Progressive Pipeline Active",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Badge16K
                        )
                    }
                }

                HudStatRow(
                    label = "Render / Target FPS",
                    value = "${metrics.renderFps.toInt()} / ${metrics.targetFps.toInt()} fps"
                )
                HudStatRow(
                    label = "Dropped Frames",
                    value = "${metrics.droppedFrames} (${String.format("%.1f", metrics.dropRatePercent)}%)",
                    valueColor = if (metrics.dropRatePercent > 5f) Color(0xFFFF5252) else TextPrimary
                )
                HudStatRow(
                    label = "Buffer Health",
                    value = "${metrics.bufferStatusMs} ms"
                )
                HudStatRow(
                    label = "A/V Sync Offset",
                    value = "${metrics.audioVideoSyncOffsetMs} ms"
                )
                HudStatRow(
                    label = "Memory Usage",
                    value = "${metrics.memoryUsageMb} MB"
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = DarkBorder, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Switch Decoder Button
                Button(
                    onClick = onToggleDecoder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .testTag("toggle_decoder_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (metrics.isHardwareAccelerated) Color(0xFF1E293B) else CyanNeon.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (metrics.isHardwareAccelerated) "Force Software Decoder" else "Use Hardware Decoder",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanNeon
                    )
                }
            }
        }
    }
}

@Composable
private fun HudStatRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = valueColor
        )
    }
}
