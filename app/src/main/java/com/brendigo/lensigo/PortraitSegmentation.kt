package com.brendigo.lensigo

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteOrder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device person segmentation. Uses the statically bundled model,
 * NEVER uploads a frame or downloads a model at run time.
 *
 * A real confidence mask is used to isolate the subject. The old artistic
 * center-weighted blur remains as a graceful no-model/device fallback only.
 */
internal object PortraitSegmentation {
    suspend fun render(source: Bitmap, strength: Float): Bitmap {
        if (strength <= 0f) return source
        val target = 512f / maxOf(source.width, source.height)
        val w = (source.width * minOf(1f, target)).toInt().coerceAtLeast(1)
        val h = (source.height * minOf(1f, target)).toInt().coerceAtLeast(1)
        val input = Bitmap.createScaledBitmap(source, w, h, true)
        val segmenter = Segmentation.getClient(
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
        )
        try {
            val mask = suspendCancellableCoroutine<SegmentationMask> { continuation ->
                segmenter.process(InputImage.fromBitmap(input, 0))
                    .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resumeWithException(it)
                    }
            }
            return blend(source, mask, strength)
        } catch (_: Exception) {
            // Explicit fallback: the offline beta model may be unavailable
            // on individual OEM devices; never block taking a photo.
            return FilterEngine.apply(source, PhotoLook(portraitBlur = strength))
        } finally {
            segmenter.close()
            if (input !== source && !input.isRecycled) input.recycle()
        }
    }

    private fun blend(source: Bitmap, mask: SegmentationMask, strength: Float): Bitmap {
        val width = source.width
        val height = source.height
        val maskWidth = mask.width
        val maskHeight = mask.height
        require(maskWidth > 0 && maskHeight > 0) { "Empty segmentation mask" }
        val conf = FloatArray(maskWidth * maskHeight)
        mask.buffer.order(ByteOrder.nativeOrder()).asFloatBuffer().get(conf)

        val tiny = Bitmap.createScaledBitmap(
            source, (width / 22).coerceAtLeast(1), (height / 22).coerceAtLeast(1), true
        )
        val blurred = try {
            Bitmap.createScaledBitmap(tiny, width, height, true)
        } finally {
            if (tiny !== source && !tiny.isRecycled) tiny.recycle()
        }
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val original = IntArray(width)
        val soft = IntArray(width)
        val out = IntArray(width)
        try {
            val amount = strength.coerceIn(0f, 1f)
            for (y in 0 until height) {
                source.getPixels(original, 0, width, 0, y, width, 1)
                blurred.getPixels(soft, 0, width, 0, y, width, 1)
                val maskY = (y.toFloat() / (height - 1).coerceAtLeast(1)) *
                    (maskHeight - 1)
                for (x in 0 until width) {
                    // Bilinear mask sampling avoids jagged nearest-neighbor
                    // person outlines when the 512px ML mask is displayed
                    // and exported at multi-megapixel resolution.
                    val maskX = (x.toFloat() / (width - 1).coerceAtLeast(1)) *
                        (maskWidth - 1)
                    val foreground = SegmentationMaskSampling.foreground(
                        conf, maskWidth, maskHeight, maskX, maskY
                    )
                    val backgroundAlpha = (1f - foreground) * amount
                    val a = original[x]
                    val b = soft[x]
                    val inv = 1f - backgroundAlpha
                    out[x] = Color.rgb(
                        (Color.red(a) * inv + Color.red(b) * backgroundAlpha).toInt()
                            .coerceIn(0, 255),
                        (Color.green(a) * inv + Color.green(b) * backgroundAlpha).toInt()
                            .coerceIn(0, 255),
                        (Color.blue(a) * inv + Color.blue(b) * backgroundAlpha).toInt()
                            .coerceIn(0, 255)
                    )
                }
                result.setPixels(out, 0, width, 0, y, width, 1)
            }
            return result
        } catch (e: Exception) {
            result.recycle()
            throw e
        } finally {
            if (blurred !== source && !blurred.isRecycled) blurred.recycle()
        }
    }
}
