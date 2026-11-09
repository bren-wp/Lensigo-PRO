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

    suspend fun save(context: Context, frames: List<File>, quality: Int,
                     lowMemory: Boolean): Uri = withContext(Dispatchers.IO) {
        require(frames.size in MIN_FRAMES..MAX_FRAMES) { "Potrebno je 2–5 fotografija" }
        val targetHeight = if (lowMemory) 560 else 760
        var output: Bitmap? = null
        var lastScaled: Bitmap? = null
        try {
            var accumulated = Matrix()
            var maxRight = 0f
            var minTop = Float.POSITIVE_INFINITY
            var maxBottom = 0f
            val margin = targetHeight / 5
            var fullWidth = 0
            var overlap = 0
            var advance = 0
            var canvas: Canvas? = null
            for (index in frames.indices) {
                val decoded = FilterEngine.decode(frames[index], 1500)
                var scaled: Bitmap? = null
                try {
                    if (index == 0) {
                        fullWidth = (decoded.width *
                            (targetHeight / decoded.height.toFloat()))
                            .toInt().coerceIn(320, 1300)
                        overlap = (fullWidth * OVERLAP).toInt()
                        advance = fullWidth - overlap
                        val maxEstimatedAdvance = advance + fullWidth / 6
                        val outWidth = fullWidth +
                            maxEstimatedAdvance * (frames.size - 1)
                        require(outWidth <= 6200) { "Prevelika panorama" }
                        val surface = Bitmap.createBitmap(outWidth,
                            targetHeight + margin * 2, Bitmap.Config.ARGB_8888)
                        output = surface
                        surface.eraseColor(Color.BLACK)
                        canvas = Canvas(surface)
                        accumulated.setTranslate(0f, margin.toFloat())
                    }
                    val currentScaled = Bitmap.createScaledBitmap(
                        decoded, fullWidth, targetHeight, true
                    )
                    scaled = currentScaled
                    if (index != 0) {
                        // True planar 3x3 homography from feature matches with
                        // deterministic RANSAC; fall back to safe fixed overlap
                        // when a scene lacks stable, textured correspondence.
                        val relative = lastScaled?.let {
                            PanoramaRegistration.estimate(it,currentScaled,advance)
                        } ?: Matrix().apply { setTranslate(advance.toFloat(),0f) }
                        accumulated = Matrix().apply {
                            setConcat(accumulated,relative)
                        }
                    }
                    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                    if (index == 0) {
                        canvas!!.drawBitmap(currentScaled, accumulated, paint)
                    } else {
                        val drawing = canvas!!
                        drawing.save()
                        drawing.concat(accumulated)
                        val pieces = 22
                        for (i in 0 until pieces) {
                            val left = overlap * i / pieces
                            val right = overlap * (i + 1) / pieces
                            if(right<=left)continue
                            drawing.save()
                            drawing.clipRect(left,0,right,targetHeight)
                            paint.alpha=(255f*(i+.5f)/pieces).toInt()
                            drawing.drawBitmap(currentScaled,0f,0f,paint)
                            drawing.restore()
                        }
                        drawing.save()
                        drawing.clipRect(overlap,0,fullWidth,targetHeight)
                        paint.alpha=255
                        drawing.drawBitmap(currentScaled,0f,0f,paint)
                        drawing.restore()
                        drawing.restore()
                    }
                    val corners=floatArrayOf(0f,0f,fullWidth.toFloat(),0f,
                        0f,targetHeight.toFloat(),fullWidth.toFloat(),targetHeight.toFloat())
                    accumulated.mapPoints(corners)
                    maxRight=maxOf(maxRight,corners[0],corners[2],corners[4],corners[6])
                    minTop=minOf(minTop,corners[1],corners[3],corners[5],corners[7])
                    maxBottom=maxOf(maxBottom,corners[1],corners[3],corners[5],corners[7])
                    lastScaled?.let { if(!it.isRecycled)it.recycle() }
                    lastScaled=currentScaled
                    scaled=null
                } finally {
                    if(scaled?.isRecycled==false)scaled.recycle()
                    if(decoded !== lastScaled && !decoded.isRecycled)decoded.recycle()
                }
            }
            val result=checkNotNull(output)
            val top=minTop.toInt().coerceIn(0,result.height-1)
            val right=(maxRight.toInt()+2).coerceIn(1,result.width)
            val bottom=(maxBottom.toInt()+2).coerceIn(top+1,result.height)
            val cropped=Bitmap.createBitmap(result,0,top,right,bottom-top)
            try {
                MediaLibrary.saveBitmap(context,cropped,"PANO",quality)
            } finally {
                if(cropped!==result && !cropped.isRecycled)cropped.recycle()
            }
        } finally {
            if(lastScaled?.isRecycled==false)lastScaled.recycle()
            if(output?.isRecycled==false)output.recycle()
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
