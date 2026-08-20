package com.example.decoder

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Log

data class CodecCapabilityInfo(
    val codecName: String,
    val mimeType: String,
    val isHardwareAccelerated: Boolean,
    val isSoftwareOnly: Boolean,
    val maxWidth: Int,
    val maxHeight: Int,
    val maxBitrateBps: Int,
    val maxFps: Int,
    val supportedColorFormats: List<Int>
)

data class DeviceDecoderProfile(
    val supports4KHardware: Boolean,
    val supports8KHardware: Boolean,
    val supports16KHardware: Boolean,
    val maxHardwareWidth: Int,
    val maxHardwareHeight: Int,
    val supportedVideoCodecs: List<CodecCapabilityInfo>,
    val chipArchitecture: String = Build.HARDWARE
)

object DecoderCapabilities {
    private const val TAG = "DecoderCapabilities"

    val cachedProfile: DeviceDecoderProfile by lazy {
        inspectDeviceDecoders()
    }

    private fun inspectDeviceDecoders(): DeviceDecoderProfile {
        val codecList = try {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query regular codecs", e)
            emptyArray<MediaCodecInfo>()
        }

        val capabilities = mutableListOf<CodecCapabilityInfo>()
        var maxHwWidth = 0
        var maxHwHeight = 0
        var has4kHw = false
        var has8kHw = false
        var has16kHw = false

        for (info in codecList) {
            if (info.isEncoder) continue
            val types = info.supportedTypes ?: continue

            for (mime in types) {
                if (!mime.startsWith("video/")) continue

                try {
                    val caps = info.getCapabilitiesForType(mime)
                    val videoCaps = caps.videoCapabilities

                    val isHw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        info.isHardwareAccelerated
                    } else {
                        !info.name.lowercase().contains("google") &&
                                !info.name.lowercase().contains("android") &&
                                !info.name.lowercase().contains("sw")
                    }

                    val isSw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        info.isSoftwareOnly
                    } else {
                        !isHw
                    }

                    val maxWidth = videoCaps?.supportedWidths?.upper ?: 1920
                    val maxHeight = videoCaps?.supportedHeights?.upper ?: 1080
                    val maxBitrate = videoCaps?.bitrateRange?.upper ?: 20_000_000
                    val maxFps = videoCaps?.supportedFrameRates?.upper?.toInt() ?: 60
                    val colorFormats = caps.colorFormats?.toList() ?: emptyList()

                    if (isHw) {
                        if (maxWidth > maxHwWidth) maxHwWidth = maxWidth
                        if (maxHeight > maxHwHeight) maxHwHeight = maxHeight
                        if (maxWidth >= 3840 || maxHeight >= 2160) has4kHw = true
                        if (maxWidth >= 7680 || maxHeight >= 4320) has8kHw = true
                        if (maxWidth >= 15360 || maxHeight >= 8640) has16kHw = true
                    }

                    capabilities.add(
                        CodecCapabilityInfo(
                            codecName = info.name,
                            mimeType = mime,
                            isHardwareAccelerated = isHw,
                            isSoftwareOnly = isSw,
                            maxWidth = maxWidth,
                            maxHeight = maxHeight,
                            maxBitrateBps = maxBitrate,
                            maxFps = maxFps,
                            supportedColorFormats = colorFormats
                        )
                    )
                } catch (e: Exception) {
                    // Specific codec capabilities query may throw on certain vendor drivers
                }
            }
        }

        return DeviceDecoderProfile(
            supports4KHardware = has4kHw,
            supports8KHardware = has8kHw,
            supports16KHardware = has16kHw,
            maxHardwareWidth = maxHwWidth,
            maxHardwareHeight = maxHwHeight,
            supportedVideoCodecs = capabilities
        )
    }

    fun isFormatHardwareSupported(mimeType: String, width: Int, height: Int): Boolean {
        val hwCodecs = cachedProfile.supportedVideoCodecs.filter {
            it.isHardwareAccelerated && it.mimeType.equals(mimeType, ignoreCase = true)
        }
        if (hwCodecs.isEmpty()) return false

        return hwCodecs.any {
            (width <= it.maxWidth && height <= it.maxHeight) ||
                    (height <= it.maxWidth && width <= it.maxHeight)
        }
    }
}
