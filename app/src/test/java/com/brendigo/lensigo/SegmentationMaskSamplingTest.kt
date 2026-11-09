package com.brendigo.lensigo

import org.junit.Assert.assertEquals
import org.junit.Test

class SegmentationMaskSamplingTest {
    @Test fun interpolatesBetweenAdjacentPixels() {
        val mask = floatArrayOf(0f, 1f, 0f, 1f)
        assertEquals(0f, SegmentationMaskSampling.foreground(mask, 2, 2, 0f, .5f), .001f)
        assertEquals(1f, SegmentationMaskSampling.foreground(mask, 2, 2, 1f, .5f), .001f)
        assertEquals(.5f, SegmentationMaskSampling.foreground(mask, 2, 2, .5f, .5f), .001f)
    }

    @Test fun untrustedConfidenceDoesNotDarkenOrEraseSubject() {
        assertEquals(1f, SegmentationMaskSampling.foreground(
            floatArrayOf(Float.NaN,1f,1f,1f), 2, 2, .5f, .5f), .001f)
        assertEquals(1f, SegmentationMaskSampling.foreground(
            floatArrayOf(0f), 2, 2, Float.POSITIVE_INFINITY, 0f), .001f)
        assertEquals(1f, SegmentationMaskSampling.foreground(
            floatArrayOf(), 2, 2, 0f, 0f), .001f)
    }

    @Test fun coordinatesAndConfidenceAreClamped() {
        assertEquals(0f, SegmentationMaskSampling.foreground(
            floatArrayOf(-10f), 1, 1, -100f, -10f), .001f)
        assertEquals(1f, SegmentationMaskSampling.foreground(
            floatArrayOf(10f), 1, 1, 100f, 100f), .001f)
    }
}
