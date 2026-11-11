package com.brendigo.lensigo

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoFileInspectorTest {
    @Test fun reportsActualEncodedQualityNotTheRequestedHint() {
        assertEquals("3840×2160 (4K/UHD) · 59.9 FPS",
            VideoFileInspector.describe(3840, 2160, 59.94f))
        assertEquals("1920×1080 (Full HD) · 30.0 FPS",
            VideoFileInspector.describe(1920,1080,30f))
        assertEquals("1280×720 (HD) · FPS nepoznat",
            VideoFileInspector.describe(1280,720,null))
        assertEquals("rezolucija nepoznata · FPS nepoznat",
            VideoFileInspector.describe(null,null,null))
    }

    @Test fun neverPromotesAnUltraWideOrMalformedVideoTo4K() {
        assertEquals("3840×200 (SD) · FPS nepoznat",
            VideoFileInspector.describe(3840, 200, null))
        assertEquals("1080×1920 (Full HD) · 30.0 FPS",
            VideoFileInspector.describe(1080, 1920, 30f))
        assertEquals("rezolucija nepoznata · FPS nepoznat",
            VideoFileInspector.describe(-1, 2160, Float.NaN))
        assertEquals("2560×1440 (QHD) · FPS nepoznat",
            VideoFileInspector.describe(2560, 1440, Float.POSITIVE_INFINITY))
    }
}
