package com.brendigo.lensigo

/**
 * A source-frame homography is safe only if all four projected corners stay
 * inside the already allocated montage. Pairwise RANSAC plausibility alone
 * cannot guarantee this after 2–5 chained transforms.
 *
 * The coordinates are the actual projected quad (x,y pairs). No image
 * allocations or camera APIs are required to test this rule.
 */
internal object PanoramaOutputBounds {
    fun fits(corners: FloatArray, width: Int, height: Int): Boolean {
        if (width < 1 || height < 1 || corners.size != 8) return false
        return corners.indices.step(2).all { i ->
            val x = corners[i]
            val y = corners[i + 1]
            x.isFinite() && y.isFinite() &&
                x >= 0f && x <= width.toFloat() &&
                y >= 0f && y <= height.toFloat()
        }
    }
}
