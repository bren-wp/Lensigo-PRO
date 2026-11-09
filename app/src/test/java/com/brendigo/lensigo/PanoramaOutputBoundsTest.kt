package com.brendigo.lensigo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanoramaOutputBoundsTest {
    @Test fun permitsAllFourCornersIncludingBoundaries() {
        assertTrue(PanoramaOutputBounds.fits(
            floatArrayOf(0f, 0f, 450f, 0f, 0f, 300f, 450f, 300f),
            450, 300))
    }

    @Test fun rejectsHorizonClippingAndVerticalDrift() {
        val safe = floatArrayOf(10f, 10f, 450f, 11f, 11f, 270f, 448f, 275f)
        assertTrue(PanoramaOutputBounds.fits(safe, 500, 300))
        assertFalse(PanoramaOutputBounds.fits(
            safe.copyOf().apply { this[6] = 501f }, 500, 300))
        assertFalse(PanoramaOutputBounds.fits(
            safe.copyOf().apply { this[1] = -1f }, 500, 300))
    }

    @Test fun rejectsInvalidAndUnboundedCoordinates() {
        assertFalse(PanoramaOutputBounds.fits(floatArrayOf(), 500, 300))
        assertFalse(PanoramaOutputBounds.fits(FloatArray(8), 0, 300))
        assertFalse(PanoramaOutputBounds.fits(
            floatArrayOf(Float.NaN, 0f, 1f, 1f, 1f, 1f, 1f, 1f),
            500, 300))
        assertFalse(PanoramaOutputBounds.fits(
            floatArrayOf(Float.POSITIVE_INFINITY, 0f, 1f, 1f, 1f, 1f, 1f, 1f),
            500, 300))
    }
}
