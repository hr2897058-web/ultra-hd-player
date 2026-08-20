package com.example.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.player.GestureOverlayState
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurface
import com.example.utils.Formatters

@Composable
fun PlayerHudOverlays(
    gestureState: GestureOverlayState,
    zoomScale: Float,
    onResetZoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Brightness HUD (Center Left)
        AnimatedVisibility(
            visible = gestureState.isBrightnessAdjusting,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 48.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("brightness_hud")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.BrightnessMedium,
                        contentDescription = "Brightness",
                        tint = CyanNeon,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(100.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        LinearProgressIndicator(
                            progress = { gestureState.brightnessPercent / 100f },
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp)),
                            color = CyanNeon,
                            trackColor = Color.Transparent
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${gestureState.brightnessPercent}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Volume HUD (Center Right)
        AnimatedVisibility(
            visible = gestureState.isVolumeAdjusting,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 48.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xCC0F172A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("volume_hud")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Volume",
                        tint = CyanNeon,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(100.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        LinearProgressIndicator(
                            progress = { gestureState.volumePercent / 100f },
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp)),
                            color = CyanNeon,
                            trackColor = Color.Transparent
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${gestureState.volumePercent}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Seek Scrubbing HUD (Center)
        AnimatedVisibility(
            visible = gestureState.isSeekScrubbing,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xDD0F172A)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("scrub_hud")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = Formatters.formatDuration(gestureState.seekTargetMs),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyanNeon
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val sign = if (gestureState.seekDeltaMs >= 0) "+" else ""
                    Text(
                        text = "[$sign${Formatters.formatDuration(Math.abs(gestureState.seekDeltaMs))}]",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (gestureState.seekDeltaMs >= 0) Color(0xFF4ADE80) else Color(0xFFFF5252)
                    )
                }
            }
        }

        // Double Tap Seek Ripple Indicator (Left / Right)
        gestureState.doubleTapSeekSide?.let { side ->
            val isLeft = side == "left"
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp),
                contentAlignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Surface(
                    color = Color(0xCC0F172A),
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isLeft) Icons.Default.FastRewind else Icons.Default.FastForward,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = if (isLeft) "-10s" else "+10s",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Zoom Scale Reset Pill (Top Center)
        if (zoomScale > 1.05f) {
            Surface(
                color = Color(0xCC0F172A),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onResetZoom)
                    .testTag("reset_zoom_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOutMap,
                        contentDescription = "Reset Zoom",
                        tint = CyanNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Zoom: ${String.format("%.1f", zoomScale)}x (Tap to reset)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
