package com.brendigo.lensigo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Guided, fixed-overlap horizontal montage. Deliberately does not claim
 * computer-vision alignment: the user should pan steadily with ~25% overlap.
 * Maximum five frames, output bounded for low-memory devices.
 */
object PanoramaComposer {
    const val MAX_FRAMES = 5
    const val MIN_FRAMES = 2
    private const val OVERLAP = .28f

    suspend fun save(context: Context, frames: List<File>, quality: Int, lowMemory: Boolean): Uri =
        withContext(Dispatchers.IO) {
            require(frames.size in MIN_FRAMES..MAX_FRAMES) { "Potrebno je 2–5 fotografija" }
            val targetHeight = if (lowMemory) 560 else 760
            var output: Bitmap? = null
            try {
                // First frame defines the aspect ratio, all frames normalized.
                val first = FilterEngine.decode(frames.first(), 1500)
                val tileWidth = (first.width * (targetHeight / first.height.toFloat()))
                    .toInt().coerceIn(320, 1300)
                val overlapPixels = (tileWidth * OVERLAP).toInt()
                val advance = tileWidth - overlapPixels
                val outWidth = tileWidth + advance * (frames.size - 1)
                require(outWidth <= 6200) { "Prevelika panorama" }
                val result = Bitmap.createBitmap(outWidth, targetHeight, Bitmap.Config.ARGB_8888)
                output = result
                val canvas = Canvas(result)
                result.eraseColor(Color.BLACK)
                for (index in frames.indices) {
                    val decoded = if (index == 0) first
                        else FilterEngine.decode(frames[index], 1500)
                    var scaled: Bitmap? = null
                    try {
                        scaled = Bitmap.createScaledBitmap(
                            decoded, tileWidth, targetHeight, true
                        )
                        val x = index * advance
                        if (index == 0) {
                            canvas.drawBitmap(scaled, x.toFloat(), 0f, null)
                        } else {
                            // Blend overlap in strips without full-frame masks.
                            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                            val stripCount = 22
                            for (i in 0 until stripCount) {
                                val left = overlapPixels * i / stripCount
                                val right = overlapPixels * (i + 1) / stripCount
                                if (right <= left) continue
                                paint.alpha = (255 * ((i + .5f) / stripCount)).toInt()
                                canvas.drawBitmap(
                                    scaled, Rect(left, 0, right, targetHeight),
                                    RectF((x + left).toFloat(), 0f,
                                        (x + right).toFloat(), targetHeight.toFloat()), paint
                                )
                            }
                            paint.alpha = 255
                            canvas.drawBitmap(scaled,
                                Rect(overlapPixels, 0, tileWidth, targetHeight),
                                RectF((x + overlapPixels).toFloat(), 0f,
                                    (x + tileWidth).toFloat(), targetHeight.toFloat()), paint)
                        }
                    } finally {
                        if (scaled !== decoded && scaled?.isRecycled == false) scaled.recycle()
                        if (!decoded.isRecycled) decoded.recycle()
                    }
                }
                MediaLibrary.saveBitmap(context, result, "PANO", quality)
            } finally {
                output?.recycle()
                frames.forEach { it.delete() }
            }
        }
}

/** Compose two captures: full-size back view with front view in a bordered PIP. */
object DualComposer {
    suspend fun save(context: Context, back: File, front: File, quality: Int,
                     lowMemory: Boolean, pipLeft: Boolean = false): Uri = withContext(Dispatchers.IO) {
        var background: Bitmap? = null
        var selfie: Bitmap? = null
        var output: Bitmap? = null
        try {
            background = FilterEngine.decode(back, if (lowMemory) 1440 else 2160)
            selfie = FilterEngine.decode(front, if (lowMemory) 1024 else 1440)
            val w = background.width
            val h = background.height
            val composed = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            output = composed
            val canvas = Canvas(composed)
            canvas.drawBitmap(background, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
            val insetWidth = (w * .34f).toInt()
            val insetHeight = (h * .31f).toInt()
            val margin = (w * .025f)
            val radius = (w * .018f)
            val left = if (pipLeft) margin else w - insetWidth - margin
            val dest = RectF(left, margin, left + insetWidth, margin + insetHeight)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawRoundRect(RectF(dest.left-3, dest.top-3,dest.right+3,dest.bottom+3),
                radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
            val src = centerCrop(selfie, insetWidth / insetHeight.toFloat())
            canvas.save()
            canvas.clipRect(dest)
            canvas.drawBitmap(selfie, src, dest, paint)
            canvas.restore()
            MediaLibrary.saveBitmap(context, composed, "DUAL", quality)
        } finally {
            output?.recycle()
            background?.recycle()
            selfie?.recycle()
            back.delete()
            front.delete()
        }
    }

    private fun centerCrop(image: Bitmap, aspect: Float): Rect {
        val width = image.width; val height = image.height
        val current = width.toFloat() / height
        return if (current > aspect) {
            val w = (height * aspect).toInt()
            Rect((width - w) / 2, 0, (width + w) / 2, height)
        } else {
            val h = (width / aspect).toInt()
            Rect(0, (height - h) / 2, width, (height + h) / 2)
        }
    }
}
