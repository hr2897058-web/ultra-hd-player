package com.example

import com.example.decoder.DecoderManager
import com.example.decoder.DecoderType
import com.example.media.ResolutionCategory
import com.example.utils.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ResolutionAndDecoderTest {

    @Test
    fun testResolutionCategoryClassification() {
        // 16K Detection
        val res16K = ResolutionCategory.fromDimensions(15360, 8640)
        assertEquals(ResolutionCategory.ULTRA_16K, res16K)
        assertTrue(res16K.isUltraHighRes)
        assertEquals("16K", res16K.label)

        // 8K Detection
        val res8K = ResolutionCategory.fromDimensions(7680, 4320)
        assertEquals(ResolutionCategory.UHD_8K, res8K)
        assertTrue(res8K.isUltraHighRes)
        assertEquals("8K", res8K.label)

        // 4K Detection
        val res4K = ResolutionCategory.fromDimensions(3840, 2160)
        assertEquals(ResolutionCategory.UHD_4K, res4K)
        assertTrue(res4K.isUltraHighRes)
        assertEquals("4K", res4K.label)

        // 1080p FHD Detection
        val res1080 = ResolutionCategory.fromDimensions(1920, 1080)
        assertEquals(ResolutionCategory.FHD_1080P, res1080)
        assertEquals(false, res1080.isUltraHighRes)
        assertEquals("1080p", res1080.label)

        // 720p HD Detection
        val res720 = ResolutionCategory.fromDimensions(1280, 720)
        assertEquals(ResolutionCategory.HD_720P, res720)
    }

    @Test
    fun testDecoderManagerSelection() {
        val decoderManager = DecoderManager()

        // Test 16K fallback routing
        val result16K = decoderManager.selectBestDecoder("video/hevc", 15360, 8640)
        assertNotNull(result16K)
        assertTrue(result16K.needsDownscalingForRendering)
        assertEquals(DecoderType.SAFE_FALLBACK, result16K.selectedDecoder)

        // Test Forced software decoding
        val forceSw = decoderManager.selectBestDecoder("video/avc", 1920, 1080, forceSoftware = true)
        assertEquals(DecoderType.OPTIMIZED_SOFTWARE_FFMPEG, forceSw.selectedDecoder)
    }

    @Test
    fun testFormatters() {
        assertEquals("01:15", Formatters.formatDuration(75000L))
        assertEquals("1:00:00", Formatters.formatDuration(3600000L))
        assertEquals("10.5 MB", Formatters.formatFileSize(11010048L))
        assertEquals("15360×8640 (16K Ultra-Res)", Formatters.formatResolutionLabel(15360, 8640))
    }
}
