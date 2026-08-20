package com.example.performance

import com.example.decoder.DecoderType

data class PerformanceMetrics(
    val videoResolution: String = "Unknown",
    val codec: String = "Unknown",
    val decoderType: DecoderType = DecoderType.HARDWARE_MEDIACODEC,
    val decoderName: String = "MediaCodec",
    val decoderMode: String = "Hardware",
    val targetFps: Float = 0f,
    val renderFps: Float = 0f,
    val droppedFrames: Long = 0L,
    val totalFrames: Long = 0L,
    val bitrateBps: Long = 0L,
    val bufferStatusMs: Long = 0L,
    val audioVideoSyncOffsetMs: Long = 0L,
    val memoryUsageMb: Long = 0L,
    val isHardwareAccelerated: Boolean = true,
    val is16KOptimizationActive: Boolean = false
) {
    val dropRatePercent: Float
        get() = if (totalFrames > 0) (droppedFrames.toFloat() / totalFrames.toFloat()) * 100f else 0f
}
