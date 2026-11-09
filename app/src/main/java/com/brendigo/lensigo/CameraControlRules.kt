package com.brendigo.lensigo

import java.util.Locale

/**
 * Pure UI rules for camera controls. Avoid showing actions that cannot run,
 * and never claim support for zoom ratios outside the active lens range.
 */
internal object CameraControlRules {
    // Microphone permission alone must never override the user's preference.
    fun recordAudioEnabled(enabledByUser: Boolean, hasPermission: Boolean): Boolean =
        enabledByUser && hasPermission

    /** Camera2 advertised ranges are necessary, not sufficient, for 60 FPS. */
    fun supports60Fps(ranges: List<IntRange>): Boolean =
        ranges.any { 60 in it }

    fun shutterEnabled(
        permission: Boolean, processing: Boolean, countdown: Int,
        videoMode: Boolean, recording: Boolean, captureReady: Boolean
    ): Boolean {
        if (!permission || processing || countdown > 0) return false
        return if (videoMode) recording || captureReady else captureReady
    }

    fun zoomPresets(min: Float, max: Float): List<Float> {
        if (!min.isFinite() || !max.isFinite() || min <= 0f || max < min) {
            return emptyList()
        }
        val candidates = listOf(.6f, 1f, 2f, 5f)
            .filter { it >= min - .001f && it <= max + .001f }
        // An unusual optical zoom range (e.g. 1.2–1.8) should still expose
        // one real control instead of a blank strip.
        return if (candidates.isNotEmpty()) candidates else listOf(min)
    }

    fun zoomLabel(ratio: Float): String =
        if (kotlin.math.abs(ratio - .6f) < .001f) "0.6"
        else if (ratio == ratio.toInt().toFloat()) "${ratio.toInt()}x"
        else String.format(Locale.ROOT, "%.1fx", ratio)
}
