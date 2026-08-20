package com.example.decoder

enum class DecoderType(
    val displayName: String,
    val priority: Int,
    val description: String
) {
    HARDWARE_MEDIACODEC(
        displayName = "Hardware (MediaCodec)",
        priority = 1,
        description = "Native on-chip GPU/DSP hardware acceleration"
    ),
    COMPATIBLE_HW_PROFILE(
        displayName = "Hardware Compatible",
        priority = 2,
        description = "Secondary hardware decoder matched by profile/level"
    ),
    OPTIMIZED_SOFTWARE_FFMPEG(
        displayName = "Software (FFmpeg)",
        priority = 3,
        description = "CPU-optimized multi-threaded software decoding"
    ),
    SAFE_FALLBACK(
        displayName = "Safe Fallback Mode",
        priority = 4,
        description = "Fault-tolerant decoding path for extreme resolutions or non-standard streams"
    )
}
