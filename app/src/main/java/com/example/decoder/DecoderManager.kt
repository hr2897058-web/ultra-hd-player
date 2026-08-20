package com.example.decoder

import android.util.Log

data class DecoderSelectionResult(
    val selectedDecoder: DecoderType,
    val isHardwareAccelerated: Boolean,
    val explanation: String,
    val needsDownscalingForRendering: Boolean = false,
    val targetRenderWidth: Int = 0,
    val targetRenderHeight: Int = 0
)

class DecoderManager {

    companion object {
        private const val TAG = "DecoderManager"
    }

    /**
     * Intelligently selects the best available decoder according to priority:
     * 1. Android hardware decoder through Media3/MediaCodec
     * 2. Compatible hardware decoder with supported profile/level
     * 3. Optimized software decoder (FFmpeg)
     * 4. Safe fallback mode
     */
    fun selectBestDecoder(
        mimeType: String,
        width: Int,
        height: Int,
        frameRate: Float = 30f,
        forceSoftware: Boolean = false
    ): DecoderSelectionResult {
        if (forceSoftware) {
            return DecoderSelectionResult(
                selectedDecoder = DecoderType.OPTIMIZED_SOFTWARE_FFMPEG,
                isHardwareAccelerated = false,
                explanation = "User requested software decoding mode"
            )
        }

        val profile = DecoderCapabilities.cachedProfile
        val is16K = width >= 11520 || height >= 6480
        val is8K = width >= 5760 || height >= 3240
        val is4K = width >= 3000 || height >= 1800

        // Check Priority 1: Direct Hardware Decoder
        val directHwSupported = DecoderCapabilities.isFormatHardwareSupported(mimeType, width, height)
        if (directHwSupported) {
            return DecoderSelectionResult(
                selectedDecoder = DecoderType.HARDWARE_MEDIACODEC,
                isHardwareAccelerated = true,
                explanation = "Direct GPU/DSP hardware acceleration active for ${width}×${height}"
            )
        }

        // Priority 2: Compatible Hardware profile / alternative HW decoder
        val anyHwForMime = profile.supportedVideoCodecs.any {
            it.isHardwareAccelerated && it.mimeType.equals(mimeType, ignoreCase = true)
        }

        if (anyHwForMime) {
            val maxHw = profile.supportedVideoCodecs
                .filter { it.isHardwareAccelerated && it.mimeType.equals(mimeType, ignoreCase = true) }
                .maxByOrNull { it.maxWidth * it.maxHeight }

            if (maxHw != null && (width <= maxHw.maxWidth * 1.5 && height <= maxHw.maxHeight * 1.5)) {
                return DecoderSelectionResult(
                    selectedDecoder = DecoderType.COMPATIBLE_HW_PROFILE,
                    isHardwareAccelerated = true,
                    explanation = "Hardware profile compatible decoding (Device Max HW: ${maxHw.maxWidth}×${maxHw.maxHeight})"
                )
            }
        }

        // Handle extreme resolution scenarios (8K / 16K) where device hardware is exceeded
        if (is16K) {
            return DecoderSelectionResult(
                selectedDecoder = DecoderType.SAFE_FALLBACK,
                isHardwareAccelerated = false,
                explanation = "16K Ultra-Res detected ($width×$height). Exceeds device hardware limits (${profile.maxHardwareWidth}×${profile.maxHardwareHeight}). Safe progressive decoding path enabled.",
                needsDownscalingForRendering = true,
                targetRenderWidth = minOf(width, 3840),
                targetRenderHeight = minOf(height, 2160)
            )
        }

        if (is8K && !profile.supports8KHardware) {
            return DecoderSelectionResult(
                selectedDecoder = DecoderType.OPTIMIZED_SOFTWARE_FFMPEG,
                isHardwareAccelerated = false,
                explanation = "8K UHD stream ($width×$height) routed to multi-threaded CPU software decoder (Device HW limit: ${profile.maxHardwareWidth}×${profile.maxHardwareHeight})"
            )
        }

        // Priority 3: Optimized Software Decoder (FFmpeg)
        return DecoderSelectionResult(
            selectedDecoder = DecoderType.OPTIMIZED_SOFTWARE_FFMPEG,
            isHardwareAccelerated = false,
            explanation = "Optimized software decoder selected for $mimeType ($width×$height)"
        )
    }
}
