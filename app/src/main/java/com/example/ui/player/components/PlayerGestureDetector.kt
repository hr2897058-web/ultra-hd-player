package com.example.ui.player.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun PlayerGestureDetector(
    onSingleTap: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onDoubleTapCenter: () -> Unit,
    onBrightnessDrag: (percentDelta: Float) -> Unit,
    onBrightnessEnd: () -> Unit,
    onVolumeDrag: (percentDelta: Float) -> Unit,
    onVolumeEnd: () -> Unit,
    onSeekScrubDrag: (deltaPixels: Float) -> Unit,
    onSeekScrubEnd: () -> Unit,
    onZoomAndPan: (zoomChange: Float, panX: Float, panY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragDirection by remember { mutableStateOf<String?>(null) } // "left_v", "right_v", "h"
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onSingleTap()
                    },
                    onDoubleTap = { offset ->
                        val screenWidth = size.width
                        val third = screenWidth / 3f
                        when {
                            offset.x < third -> onDoubleTapLeft()
                            offset.x > 2 * third -> onDoubleTapRight()
                            else -> onDoubleTapCenter()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (zoom != 1.0f || pan != Offset.Zero) {
                        onZoomAndPan(zoom, pan.x, pan.y)
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        totalDragX = 0f
                        totalDragY = 0f
                        val screenWidth = size.width
                        dragDirection = if (offset.x < screenWidth / 2f) "left_v" else "right_v"
                    },
                    onDragEnd = {
                        isDragging = false
                        when (dragDirection) {
                            "left_v" -> onBrightnessEnd()
                            "right_v" -> onVolumeEnd()
                            "h" -> onSeekScrubEnd()
                        }
                        dragDirection = null
                    },
                    onDragCancel = {
                        isDragging = false
                        when (dragDirection) {
                            "left_v" -> onBrightnessEnd()
                            "right_v" -> onVolumeEnd()
                            "h" -> onSeekScrubEnd()
                        }
                        dragDirection = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y

                        val isHorizontal = Math.abs(totalDragX) > Math.abs(totalDragY) + 20f

                        if (isHorizontal && dragDirection != "h" && Math.abs(totalDragX) > 30f) {
                            dragDirection = "h"
                        }

                        when (dragDirection) {
                            "left_v" -> {
                                val deltaPercent = -dragAmount.y / 6f
                                onBrightnessDrag(deltaPercent)
                            }
                            "right_v" -> {
                                val deltaPercent = -dragAmount.y / 6f
                                onVolumeDrag(deltaPercent)
                            }
                            "h" -> {
                                onSeekScrubDrag(dragAmount.x)
                            }
                        }
                    }
                )
            }
    )
}
