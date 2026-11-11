package com.brendigo.lensigo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private val FilterCyan = Color(0xFF00D4FF)
private val FilterInk = Color(0xFF081724)

/**
 * The user-supplied offline reference photograph is reused for the thumbnails.
 * Filters are rendered with the same FilterEngine as image export, not arbitrary
 * gradients or static demo thumbnails. A short debounce avoids recomputation
 * on every intermediate slider frame.
 */
@Composable
internal fun LensigoFilterPreviewStrip(
    selected: LensFilter,
    intensity: Float,
    previewView: PreviewView?,
    onSelected: (LensFilter) -> Unit
) {
    val context = LocalContext.current
    var frameNumber by remember(previewView) { mutableIntStateOf(0) }
    LaunchedEffect(previewView) {
        // No camera surface: the startup fallback never changes and should
        // not regenerate all thumbnails every 1.8 seconds.
        if (previewView == null) return@LaunchedEffect
        while (true) {
            delay(1800)
            frameNumber++
        }
    }
    val previews by produceState<Map<LensFilter, ImageBitmap>>(
        initialValue = emptyMap(), key1 = intensity, key2 = frameNumber,
        key3 = previewView
    ) {
        delay(100)
        // PreviewView.bitmap is read on the UI thread, never on a worker.
        // Process a tiny 160px sensor thumbnail, not the full-size camera
        // frame. Only use the original source photo as a startup fallback.
        val frame = runCatching { previewView?.bitmap }.getOrNull()
        val source = if (frame != null) {
            try {
                val maxEdge = maxOf(frame.width, frame.height).coerceAtLeast(1)
                val scale = 160f / maxEdge
                val scaled = Bitmap.createScaledBitmap(
                    frame, (frame.width * scale).toInt().coerceAtLeast(1),
                    (frame.height * scale).toInt().coerceAtLeast(1), true
                )
                // createScaledBitmap may return its input unchanged if the
                // requested size matches the 160px preview. The original
                // PreviewView snapshot is released below, so take ownership
                // of a separate buffer in this edge case.
                if (scaled === frame)
                    frame.copy(Bitmap.Config.ARGB_8888, false) else scaled
            } finally { frame.recycle() }
        } else BitmapFactory.decodeResource(context.resources,
            R.drawable.lensigo_home_hero)
        if (source != null) {
            // Retain ownership across the dispatcher hand-off: if the UI
            // producer is cancelled after the worker finishes but before
            // withContext resumes, its completed bitmaps must be reclaimed.
            var unpublished: Map<LensFilter, ImageBitmap>? = null
            try {
                withContext(Dispatchers.Default) {
                    val owned = LinkedHashMap<LensFilter, ImageBitmap>()
                    try {
                        for (filter in LensFilter.entries) {
                            currentCoroutineContext().ensureActive()
                            owned[filter] = FilterEngine.apply(
                                source, PhotoLook(filter = filter, intensity = intensity)
                            ).asImageBitmap()
                        }
                        currentCoroutineContext().ensureActive()
                        unpublished = owned
                    } catch (error: Throwable) {
                        // A mid-loop processing failure or cancellation also
                        // releases every image completed by this worker.
                        owned.values.forEach { bitmap ->
                            val native = bitmap.asAndroidBitmap()
                            if (!native.isRecycled) native.recycle()
                        }
                        throw error
                    }
                }
                value = unpublished.orEmpty()
                // UI owns these from this point. DisposableEffect releases
                // them when the published preview set leaves composition.
                unpublished = null
            } finally {
                unpublished?.values?.forEach { bitmap ->
                    val native = bitmap.asAndroidBitmap()
                    if (!native.isRecycled) native.recycle()
                }
                if (!source.isRecycled) source.recycle()
            }
        } else value = emptyMap()
    }
    // Every slider update creates a small set of actual locally rendered
    // thumbnails. Dispose previous native bitmaps after Compose releases
    // their image nodes to avoid accumulated memory on low-RAM phones.
    DisposableEffect(previews) {
        val snapshots = previews.values.toList()
        onDispose {
            snapshots.forEach { image ->
                val bitmap = image.asAndroidBitmap()
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(horizontal = 11.dp)
    ) {
        items(LensFilter.entries, key = { it.name }) { item ->
            val chosen = selected == item
            val shape = RoundedCornerShape(7.dp)
            Column(
                Modifier.width(76.dp)
                    .background(FilterInk.copy(alpha = .96f), shape)
                    .border(
                        if (chosen) 2.dp else 1.dp,
                        if (chosen) FilterCyan else Color(0xFF53778A), shape
                    )
                    .clip(shape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Primijeni filtar ${item.title}"
                    ) { onSelected(item) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val bitmap = previews[item]
                if (bitmap != null) {
                    Image(bitmap = bitmap, contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(48.dp))
                } else {
                    Image(
                        painter = painterResource(R.drawable.lensigo_home_hero),
                        contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                }
                Text(
                    item.title, color = if (chosen) FilterCyan else Color.White,
                    fontSize = 10.sp, fontWeight = if (chosen) FontWeight.Bold
                        else FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 3.dp)
                )
            }
        }
    }
}
