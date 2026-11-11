package com.brendigo.lensigo

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.util.Locale

/**
 * Reads the ACTUAL saved MP4 metadata, not the requested CameraX resolution
 * and frame-rate hints. Some OEM encoders omit capture frame-rate metadata;
 * report "FPS nepoznat" instead of inventing 60 FPS.
 */
internal object VideoFileInspector {
    fun inspect(context: Context, uri: Uri): String {
        if (uri == Uri.EMPTY) return "podaci o snimci nisu dostupni"
        val extractor = MediaMetadataRetriever()
        return try {
            extractor.setDataSource(context, uri)
            val width = extractor.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            val height = extractor.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            val fps = extractor.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()
            describe(width, height, fps)
        } catch (_: Exception) {
            "metapodaci nisu dostupni"
        } finally {
            extractor.release()
        }
    }

    fun describe(width: Int?, height: Int?, fps: Float?): String {
        // A long edge alone is insufficient: 3840 × 200 is not 4K video.
        // Always report the *actual encoded dimensions*, including portrait
        // recordings and uncommon aspect ratios, instead of a quality hint.
        val valid = width != null && height != null && width > 0 && height > 0
        val longEdge = if (valid) maxOf(width!!, height!!) else 0
        val shortEdge = if (valid) minOf(width!!, height!!) else 0
        val quality = when {
            longEdge >= 3800 && shortEdge >= 2000 -> "4K/UHD"
            longEdge >= 2500 && shortEdge >= 1400 -> "QHD"
            longEdge >= 1880 && shortEdge >= 1000 -> "Full HD"
            longEdge >= 1250 && shortEdge >= 650 -> "HD"
            else -> "SD"
        }
        val resolution = if (valid) "${width}×${height} ($quality)"
            else "rezolucija nepoznata"
        val frameRate = if (fps != null && fps.isFinite() && fps in 1f..480f) {
            String.format(Locale.ROOT, "%.1f FPS", fps)
        } else "FPS nepoznat"
        return "$resolution · $frameRate"
    }
}
