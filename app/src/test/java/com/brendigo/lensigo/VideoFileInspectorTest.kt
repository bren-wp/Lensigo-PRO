package com.brendigo.lensigo

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoFileInspectorTest {
    @Test fun reportsActualEncodedQualityNotTheRequestedHint() {
        assertEquals("4K/UHD · 59.9 FPS",
            VideoFileInspector.describe(3840, 2160, 59.94f))
        assertEquals("Full HD · 30.0 FPS",
            VideoFileInspector.describe(1920,1080,30f))
        assertEquals("HD · FPS nepoznat",
            VideoFileInspector.describe(1280,720,null))
        assertEquals("rezolucija nepoznata · FPS nepoznat",
            VideoFileInspector.describe(null,null,null))
    }
}
