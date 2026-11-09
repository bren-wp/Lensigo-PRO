package com.brendigo.lensigo

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Local operations for media created by LENSIGO. Never uploads files. */
object MediaActions {
    private const val FAVORITES = "lensigo_favorites"

    fun isFavorite(context: Context, uri: Uri): Boolean =
        context.getSharedPreferences(FAVORITES, Context.MODE_PRIVATE)
            .getStringSet("items", emptySet())?.contains(uri.toString()) == true

    fun toggleFavorite(context: Context, uri: Uri): Boolean {
        val prefs = context.getSharedPreferences(FAVORITES, Context.MODE_PRIVATE)
        val values = prefs.getStringSet("items", emptySet()).orEmpty().toMutableSet()
        val added = if (values.contains(uri.toString())) {
            values.remove(uri.toString()); false
        } else { values.add(uri.toString()); true }
        prefs.edit().putStringSet("items", values).apply()
        return added
    }

    suspend fun saveCopy(context: Context, source: LensMedia): Uri =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val stamp = System.currentTimeMillis()
            val mime = if (source.isVideo) "video/mp4" else "image/jpeg"
            val uri = if (source.isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "LENSIGO_COPY_$stamp." +
                    if (source.isVideo) "mp4" else "jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                if (Build.VERSION.SDK_INT >= 29) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH,
                        if (source.isVideo) "Movies/LENSIGO" else "Pictures/LENSIGO")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val destination = resolver.insert(uri, values)
                ?: throw IOException("Nije moguće spremiti kopiju")
            try {
                val input = resolver.openInputStream(source.uri)
                    ?: throw IOException("Izvorna datoteka nije dostupna")
                input.use { ins ->
                    val out = resolver.openOutputStream(destination, "w")
                        ?: throw IOException("Nije moguće pisati kopiju")
                    out.use { ins.copyTo(it, bufferSize = 64 * 1024) }
                }
                if (Build.VERSION.SDK_INT >= 29) {
                    if (resolver.update(destination, ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }, null, null) != 1) {
                        throw IOException("Kopija nije objavljena u galeriji")
                    }
                }
                destination
            } catch (e: Exception) {
                runCatching { resolver.delete(destination, null, null) }
                throw e
            }
        }

    /**
     * The same crop / rotation / filter pipeline is used by the lightweight
     * editor preview and full-resolution export. The caller retains ownership
     * of [source] and must recycle the resulting bitmap when finished.
     */
    fun renderEdit(
        source: Bitmap, look: PhotoLook, cropAspect: Float?, rotation: Int
    ): Bitmap {
        var current = source
        try {
            if (cropAspect != null && cropAspect > 0f) {
                val geometry = EditorGeometry.centerCrop(current.width, current.height, cropAspect)
                if (geometry.width != current.width || geometry.height != current.height) {
                    val next = Bitmap.createBitmap(current, geometry.left, geometry.top,
                        geometry.width, geometry.height)
                    if (current !== source) current.recycle()
                    current = next
                }
            }
            val normalized = ((rotation % 360) + 360) % 360
            if (normalized != 0) {
                val next = Bitmap.createBitmap(current, 0, 0, current.width, current.height,
                    Matrix().apply { postRotate(normalized.toFloat()) }, true)
                if (current !== source && next !== current) current.recycle()
                current = next
            }
            val output = FilterEngine.apply(current, look)
            if (current !== source && output !== current) current.recycle()
            return output
        } catch (error: Exception) {
            if (current !== source && !current.isRecycled) current.recycle()
            throw error
        }
    }

    /** Editing never overwrites the original photo or its MediaStore item. */
    suspend fun saveEdited(context: Context, source: Uri, look: PhotoLook,
                           cropAspect: Float?, rotation: Int,
                           quality: Int, lowMemory: Boolean): Uri =
        withContext(Dispatchers.IO) {
            val original = File.createTempFile("lensigo_editor_", ".jpg", context.cacheDir)
            try {
                context.contentResolver.openInputStream(source)?.use { input ->
                    original.outputStream().use { input.copyTo(it) }
                } ?: throw IOException("Fotografija nije dostupna")
                val decoded = FilterEngine.decode(original, if (lowMemory) 2048 else 3072)
                val edited = try {
                    renderEdit(decoded, look, cropAspect, rotation)
                } finally {
                    decoded.recycle()
                }
                try {
                    MediaLibrary.saveBitmap(context, edited, "EDIT", quality)
                } finally {
                    edited.recycle()
                }
            } finally {
                original.delete()
            }
        }
}
