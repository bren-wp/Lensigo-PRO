package com.brendigo.lensigo

import kotlin.math.floor

/** Confidence sampling used to blend ML Kit's low-resolution person mask.
 * Invalid floats must never turn a portrait into a black/transparent image.
 */
internal object SegmentationMaskSampling {
    fun foreground(mask: FloatArray, width: Int, height: Int,
                   x: Float, y: Float): Float {
        if (width < 1 || height < 1 || mask.size != width * height ||
            !x.isFinite() || !y.isFinite()) return 1f
        val px = x.coerceIn(0f, (width - 1).toFloat())
        val py = y.coerceIn(0f, (height - 1).toFloat())
        val x0 = floor(px.toDouble()).toInt()
        val y0 = floor(py.toDouble()).toInt()
        val x1 = (x0 + 1).coerceAtMost(width - 1)
        val y1 = (y0 + 1).coerceAtMost(height - 1)
        val fx = px - x0
        val fy = py - y0
        val tl = mask[y0 * width + x0]
        val tr = mask[y0 * width + x1]
        val bl = mask[y1 * width + x0]
        val br = mask[y1 * width + x1]
        if (!tl.isFinite() || !tr.isFinite() ||
            !bl.isFinite() || !br.isFinite()) return 1f
        val top = tl * (1f - fx) + tr * fx
        val bottom = bl * (1f - fx) + br * fx
        val confidence = top * (1f - fy) + bottom * fy
        return ((confidence - .25f) / .50f).coerceIn(0f, 1f)
    }
}
