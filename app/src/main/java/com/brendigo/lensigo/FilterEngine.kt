package com.brendigo.lensigo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Matrix
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max

/** Reusable local, offline filters. Never claims to improve sensor resolution. */
enum class LensFilter(val title: String) {
    ORIGINAL("Original"), VIVID("Vivid"), FILM("Film"),
    MONO("Mono"), WARM("Warm"), COOL("Cool"),
    VINTAGE("Vintage"), FADE("Fade"), DRAMATIC("Drama"),
    SEPIA("Sepia"), NOIR("Noir"), SUNSET("Sunset")
}

data class PhotoLook(
    val filter: LensFilter = LensFilter.ORIGINAL,
    val intensity: Float = 1f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val lowLight: Boolean = false,
    val softenNoise: Boolean = false,
    val sharpen: Boolean = false,
    val portraitBlur: Float = 0f
) {
    val isNeutral: Boolean get() = filter == LensFilter.ORIGINAL &&
        brightness == 0f && contrast == 1f && saturation == 1f &&
        !lowLight && !softenNoise && !sharpen && portraitBlur <= 0f
}

object FilterEngine {
    private val identity = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    private fun preset(filter: LensFilter): FloatArray {
        val matrix = ColorMatrix()
        when (filter) {
            LensFilter.ORIGINAL -> Unit
            LensFilter.VIVID -> {
                matrix.setSaturation(1.48f)
                matrix.postConcat(ColorMatrix(floatArrayOf(
                    1.07f,0f,0f,0f,-5f, 0f,1.07f,0f,0f,-5f,
                    0f,0f,1.07f,0f,-5f, 0f,0f,0f,1f,0f
                )))
            }
            LensFilter.FILM -> {
                matrix.setSaturation(.82f)
                matrix.postConcat(ColorMatrix(floatArrayOf(
                    1.08f,0f,0f,0f,5f, 0f,1.01f,0f,0f,1f,
                    0f,0f,.87f,0f,7f, 0f,0f,0f,1f,0f
                )))
            }
            LensFilter.MONO -> matrix.setSaturation(0f)
            LensFilter.WARM -> matrix.set(floatArrayOf(
                1.10f,0f,0f,0f,5f, 0f,1.03f,0f,0f,0f,
                0f,0f,.87f,0f,-4f, 0f,0f,0f,1f,0f
            ))
            LensFilter.COOL -> matrix.set(floatArrayOf(
                .90f,0f,0f,0f,0f, 0f,1.02f,0f,0f,0f,
                0f,0f,1.16f,0f,3f, 0f,0f,0f,1f,0f
            ))
            LensFilter.VINTAGE -> matrix.set(floatArrayOf(
                .92f,.08f,.06f,0f,4f, .06f,.89f,.04f,0f,4f,
                .01f,.11f,.76f,0f,6f, 0f,0f,0f,1f,0f
            ))
            LensFilter.FADE -> {
                matrix.setSaturation(.72f)
                matrix.postConcat(ColorMatrix(floatArrayOf(
                    .84f,0f,0f,0f,25f, 0f,.84f,0f,0f,25f,
                    0f,0f,.84f,0f,25f, 0f,0f,0f,1f,0f
                )))
            }
            LensFilter.DRAMATIC -> {
                matrix.setSaturation(.72f)
                matrix.postConcat(ColorMatrix(floatArrayOf(
                    1.26f,0f,0f,0f,-30f, 0f,1.26f,0f,0f,-30f,
                    0f,0f,1.26f,0f,-30f, 0f,0f,0f,1f,0f
                )))
            }
            LensFilter.SEPIA -> matrix.set(floatArrayOf(
                .393f,.769f,.189f,0f,0f,
                .349f,.686f,.168f,0f,0f,
                .272f,.534f,.131f,0f,0f,
                0f,0f,0f,1f,0f
            ))
            LensFilter.NOIR -> {
                matrix.setSaturation(0f)
                matrix.postConcat(ColorMatrix(floatArrayOf(
                    1.4f,0f,0f,0f,-50f, 0f,1.4f,0f,0f,-50f,
                    0f,0f,1.4f,0f,-50f, 0f,0f,0f,1f,0f
                )))
            }
            LensFilter.SUNSET -> matrix.set(floatArrayOf(
                1.2f,.06f,0f,0f,5f, 0f,1.02f,0f,0f,1f,
                0f,0f,.75f,0f,0f, 0f,0f,0f,1f,0f
            ))
        }
        return matrix.array
    }

    fun matrix(look: PhotoLook): ColorMatrix {
        val preset = preset(look.filter)
        val amount = look.intensity.coerceIn(0f, 1f)
        val mixed = FloatArray(20) { i -> identity[i] + (preset[i] - identity[i]) * amount }
        val result = ColorMatrix(mixed)
        val sat = ColorMatrix().apply { setSaturation(look.saturation.coerceIn(0f, 2f)) }
        result.postConcat(sat)
        val lift = if (look.lowLight) 24f else 0f
        val contrast = look.contrast.coerceIn(.5f, 1.8f)
        val offset = 128f * (1f - contrast) + (look.brightness * 55f) + lift
        result.postConcat(ColorMatrix(floatArrayOf(
            contrast,0f,0f,0f,offset, 0f,contrast,0f,0f,offset,
            0f,0f,contrast,0f,offset, 0f,0f,0f,1f,0f
        )))
        return result
    }

    fun colorFilter(look: PhotoLook): ColorMatrixColorFilter =
        ColorMatrixColorFilter(matrix(look))

    /** Decode with a bounded pixel count to avoid OOM on low-RAM phones. */
    fun decode(file: java.io.File, maxEdge: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Neispravna fotografija" }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxEdge) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = requireNotNull(BitmapFactory.decodeFile(file.absolutePath, options))
        // Also honor mirrored EXIF orientations used by some front cameras.
        // A damaged EXIF segment must not prevent a decodable JPEG from
        // opening in the editor or in the local capture pipeline.
        val (rotation, mirrored) = runCatching {
            ExifInterface(file.absolutePath).let { it.rotationDegrees to it.isFlipped }
        }.getOrDefault(0 to false)
        if (rotation == 0 && !mirrored) return bitmap
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height,
            Matrix().apply {
                if (mirrored) postScale(-1f, 1f)
                if (rotation != 0) postRotate(rotation.toFloat())
            }, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    fun apply(input: Bitmap, look: PhotoLook): Bitmap {
        val result = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
        Canvas(result).drawBitmap(input, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = colorFilter(look)
        })
        if (!look.softenNoise && !look.sharpen) return portraitBlur(result, look.portraitBlur)
        val width = result.width
        val height = result.height
        if (width < 3 || height < 3) return result
        val processed = result.copy(Bitmap.Config.ARGB_8888, true)
        val previous = IntArray(width)
        val current = IntArray(width)
        val next = IntArray(width)
        // Reuse one output scanline rather than allocating a new IntArray
        // for every row of a multi-megapixel photo.
        val out = IntArray(width)
        result.getPixels(previous, 0, width, 0, 0, width, 1)
        result.getPixels(current, 0, width, 0, 1, width, 1)
        for (y in 1 until height - 1) {
            result.getPixels(next, 0, width, 0, y + 1, width, 1)
            java.lang.System.arraycopy(current, 0, out, 0, width)
            for (x in 1 until width - 1) {
                val center = current[x]
                val up = previous[x]
                val left = current[x - 1]
                val right = current[x + 1]
                val down = next[x]
                val sumR = android.graphics.Color.red(up) + android.graphics.Color.red(left) +
                    android.graphics.Color.red(right) + android.graphics.Color.red(down)
                val sumG = android.graphics.Color.green(up) + android.graphics.Color.green(left) +
                    android.graphics.Color.green(right) + android.graphics.Color.green(down)
                val sumB = android.graphics.Color.blue(up) + android.graphics.Color.blue(left) +
                    android.graphics.Color.blue(right) + android.graphics.Color.blue(down)
                var r = android.graphics.Color.red(center)
                var g = android.graphics.Color.green(center)
                var b = android.graphics.Color.blue(center)
                if (look.softenNoise) {
                    val avgR = sumR / 4
                    val avgG = sumG / 4
                    val avgB = sumB / 4
                    if (maxOf(kotlin.math.abs(r - avgR), kotlin.math.abs(g - avgG),
                        kotlin.math.abs(b - avgB)) < 22) {
                        r = (r * 3 + avgR) / 4
                        g = (g * 3 + avgG) / 4
                        b = (b * 3 + avgB) / 4
                    }
                }
                if (look.sharpen) {
                    // Mild unsharp mask; no per-pixel allocations on low-RAM devices.
                    r = ((r * 14 - sumR) / 10).coerceIn(0,255)
                    g = ((g * 14 - sumG) / 10).coerceIn(0,255)
                    b = ((b * 14 - sumB) / 10).coerceIn(0,255)
                }
                out[x] = android.graphics.Color.rgb(r.coerceIn(0,255),
                    g.coerceIn(0,255), b.coerceIn(0,255))
            }
            processed.setPixels(out, 0, width, 0, y, width, 1)
            java.lang.System.arraycopy(current, 0, previous, 0, width)
            java.lang.System.arraycopy(next, 0, current, 0, width)
        }
        result.recycle()
        return portraitBlur(processed, look.portraitBlur)
    }

    /** Artistic center-weighted blur fallback. Not semantic/face segmentation. */
    private fun portraitBlur(source: Bitmap, strength: Float): Bitmap {
        val intensity = strength.coerceIn(0f, 1f)
        if (intensity <= 0f || source.width < 32 || source.height < 32) return source
        val w = source.width; val h = source.height
        val reduction = 9 + (intensity * 24).toInt()
        val tiny = Bitmap.createScaledBitmap(source, (w / reduction).coerceAtLeast(1),
            (h / reduction).coerceAtLeast(1), true)
        val blurred = Bitmap.createScaledBitmap(tiny, w, h, true)
        tiny.recycle()
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val sharpRow = IntArray(w)
        val blurRow = IntArray(w)
        val output = IntArray(w)
        for (y in 0 until h) {
            source.getPixels(sharpRow, 0, w, 0, y, w, 1)
            blurred.getPixels(blurRow, 0, w, 0, y, w, 1)
            val cy = (y / h.toFloat() - .47f) / .43f
            for (x in 0 until w) {
                val cx = (x / w.toFloat() - .50f) / .36f
                val radius = kotlin.math.sqrt(cx * cx + cy * cy)
                val factor = ((radius - .75f) / .55f).coerceIn(0f, 1f) * intensity
                val original = sharpRow[x]; val soft = blurRow[x]
                val r = (android.graphics.Color.red(original) * (1f-factor) +
                    android.graphics.Color.red(soft) * factor).toInt().coerceIn(0,255)
                val g = (android.graphics.Color.green(original) * (1f-factor) +
                    android.graphics.Color.green(soft) * factor).toInt().coerceIn(0,255)
                val b = (android.graphics.Color.blue(original) * (1f-factor) +
                    android.graphics.Color.blue(soft) * factor).toInt().coerceIn(0,255)
                output[x] = android.graphics.Color.rgb(r,g,b)
            }
            result.setPixels(output, 0, w, 0, y, w, 1)
        }
        blurred.recycle(); source.recycle()
        return result
    }
}
