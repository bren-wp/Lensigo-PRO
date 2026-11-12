package com.brendigo.lensigo

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.CancellationSignal
import android.provider.MediaStore
import android.net.Uri
import android.util.Size
import java.io.File
import java.io.FileInputStream
import java.io.IOException

data class LensMedia(val uri: Uri, val isVideo: Boolean, val timestamp: Long)

object MediaLibrary {
    fun list(context: Context): List<LensMedia> {
        val result = mutableListOf<LensMedia>()
        val sources = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI to false,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI to true
        )
        for ((source, video) in sources) {
            val date = MediaStore.MediaColumns.DATE_ADDED
            val name = MediaStore.MediaColumns.DISPLAY_NAME
            // A filename is not an ownership check. On Android 10+, show only
            // records whose contributing package is this application.
            val ownerColumnName = if (Build.VERSION.SDK_INT >= 29)
                MediaStore.MediaColumns.OWNER_PACKAGE_NAME else null
            val projection = if (ownerColumnName != null)
                arrayOf(MediaStore.MediaColumns._ID, date, name, ownerColumnName)
                else arrayOf(MediaStore.MediaColumns._ID, date, name)
            val relativeDirectory = if (video) "Movies/LENSIGO/" else "Pictures/LENSIGO/"
            val where = if (Build.VERSION.SDK_INT >= 29)
                name + " LIKE ? AND " + MediaStore.MediaColumns.RELATIVE_PATH + " = ?"
                else name + " LIKE ?"
            val args = if (Build.VERSION.SDK_INT >= 29)
                arrayOf("LENSIGO_%", relativeDirectory) else arrayOf("LENSIGO_%")
            try {
                context.contentResolver.query(source, projection,
                    where, args, date + " DESC")
                    ?.use { cursor ->
                        val id = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                        val time = cursor.getColumnIndexOrThrow(date)
                        val display = cursor.getColumnIndexOrThrow(name)
                        val ownerIndex = ownerColumnName?.let(cursor::getColumnIndex)
                        // When ownership is available, missing data fails closed.
                        if (ownerColumnName != null && (ownerIndex == null || ownerIndex < 0))
                            return@use
                        while (cursor.moveToNext()) {
                            // LIKE treats '_' as a wildcard, so verify the literal prefix.
                            val filename = cursor.getString(display) ?: continue
                            if (!filename.startsWith("LENSIGO_") ||
                                filename.endsWith(".dng", ignoreCase = true)) continue
                            // DNG files remain accessible through the device's
                            // file/gallery apps; this in-app Viewer handles JPEG only.
                            if (ownerIndex != null && ownerIndex >= 0 &&
                                cursor.getString(ownerIndex) != context.packageName) continue
                            result.add(LensMedia(
                                ContentUris.withAppendedId(source, cursor.getLong(id)),
                                video, cursor.getLong(time)
                            ))
                        }
                    }
            } catch (_: SecurityException) {
                // Media items not visible to this app are never requested by broad read access.
            }
        }
        return result.sortedByDescending { it.timestamp }
    }

    fun thumbnail(context: Context, item: LensMedia, size: Int = 240): Bitmap? {
        if (Build.VERSION.SDK_INT >= 29) return try {
            context.contentResolver.loadThumbnail(item.uri, Size(size, size), CancellationSignal())
        } catch (_: Exception) { null }
        if (item.isVideo) {
            // The legacy full-size video frame can exhaust RAM. API 27+
            // supports scaled extraction; 24–26 uses MediaStore thumbnails.
            if (Build.VERSION.SDK_INT <= 26) {
                return try {
                    MediaStore.Video.Thumbnails.getThumbnail(
                        context.contentResolver,
                        ContentUris.parseId(item.uri),
                        MediaStore.Video.Thumbnails.MINI_KIND, null)
                } catch (_: Exception) { null }
            }
            val retriever = MediaMetadataRetriever()
            return try {
                retriever.setDataSource(context, item.uri)
                retriever.getScaledFrameAtTime(
                    0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    size.coerceIn(96, 512), size.coerceIn(96, 512))
            } catch (_: Exception) { null } finally {
                // Some OEM codecs throw while closing a corrupt or partial MP4.
                runCatching { retriever.release() }
            }
        }
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(item.uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            val maxEdge = size.coerceIn(96, 512)
            while (kotlin.math.max(bounds.outWidth, bounds.outHeight) / sample > maxEdge * 2) {
                sample *= 2
            }
            resolver.openInputStream(item.uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null,
                    BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.RGB_565
                    })
            }
        } catch (_: Exception) { null }
    }

    suspend fun saveImage(context: Context, file: File, look: PhotoLook,
                  quality: Int, lowMemory: Boolean): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "LENSIGO_" + System.currentTimeMillis() + ".jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/LENSIGO")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Nije moguće kreirati fotografiju")
        try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                // Neutral captures must also respect the JPEG quality slider.
                // Re-encoding removes source EXIF, including possible location
                // and device metadata, rather than copying opaque camera bytes.
                val bitmap = FilterEngine.decode(file,
                    if (look.portraitBlur > 0f) 1600 else if (lowMemory) 2048 else 3072)
                try {
                    // Semantic foreground segmentation replaces the previous
                    // center-weighted fake bokeh where the bundled local model
                    // is available. A device/model failure safely falls back.
                    val standardLook = if (look.portraitBlur > 0f)
                        look.copy(portraitBlur = 0f) else look
                    val color = if (standardLook.isNeutral) bitmap
                        else FilterEngine.apply(bitmap, standardLook)
                    try {
                        val result = if (look.portraitBlur > 0f)
                            PortraitSegmentation.render(color, look.portraitBlur)
                            else color
                        try {
                            if (!result.compress(Bitmap.CompressFormat.JPEG,
                                    quality.coerceIn(70, 100), output)) {
                                throw IOException("JPEG kodiranje nije uspjelo")
                            }
                        } finally {
                            if (result !== color && !result.isRecycled) result.recycle()
                        }
                    } finally {
                        if (color !== bitmap && !color.isRecycled) color.recycle()
                    }
                } finally {
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            } ?: throw IOException("Nije moguće pisati fotografiju")
            if (Build.VERSION.SDK_INT >= 29) {
                if (resolver.update(uri, ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }, null, null) != 1) throw IOException("Objava fotografije nije uspjela")
            }
            return uri
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }

    /** Persist a real sensor DNG only after a successful CameraX RAW output.
     *  File metadata is generated by CameraX/Camera2; JPEG processing does
     *  not touch this separate lossless sensor file. */
    fun saveRaw(context: Context, file: File): Uri {
        require(file.length() > 0L) { "DNG snimka nije zapisana" }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME,
                "LENSIGO_RAW_" + System.currentTimeMillis() + ".dng")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/x-adobe-dng")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/LENSIGO")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Nije moguće otvoriti DNG u MediaStoreu")
        try {
            resolver.openOutputStream(uri, "w")?.use { out ->
                FileInputStream(file).use { it.copyTo(out) }
            } ?: throw IOException("Nije moguće spremiti DNG")
            if (Build.VERSION.SDK_INT >= 29) {
                if (resolver.update(uri, ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }, null, null) != 1) throw IOException("DNG nije objavljen")
            }
            return uri
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }

    /** Saves composed images atomically in MediaStore, with cleanup on any failure. */
    fun saveBitmap(context: Context, image: Bitmap, prefix: String, quality: Int): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME,
                "LENSIGO_" + prefix + "_" + System.currentTimeMillis() + ".jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/LENSIGO")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val destination = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Nije moguće otvoriti izlaznu fotografiju")
        try {
            resolver.openOutputStream(destination, "w")?.use { out ->
                if (!image.compress(Bitmap.CompressFormat.JPEG,
                        quality.coerceIn(70, 100), out)) {
                    throw IOException("JPEG kodiranje nije uspjelo")
                }
            } ?: throw IOException("Nije moguće pisati izlaznu fotografiju")
            if (Build.VERSION.SDK_INT >= 29) {
                if (resolver.update(destination, ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }, null, null) != 1)
                    throw IOException("Objava fotografije nije uspjela")
            }
            return destination
        } catch (e: Exception) {
            runCatching { resolver.delete(destination, null, null) }
            throw e
        }
    }
}
