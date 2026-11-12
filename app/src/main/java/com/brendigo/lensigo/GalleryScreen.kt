package com.brendigo.lensigo

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import android.content.Intent
import android.graphics.Bitmap
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val GalleryInk = Color(0xFF020508)
private val GalleryCyan = Color(0xFF00D4FF)
private val GalleryText = Color(0xFFF7FCFF)
private val GalleryMuted = Color(0xFF9DB5C4)

@Composable
fun GalleryScreen(onBack: () -> Unit, onOpen: (LensMedia) -> Unit,
                  onSettings: () -> Unit = {}) {
    val context = LocalContext.current
    var media by remember { mutableStateOf<List<LensMedia>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var loadAttempt by remember { mutableIntStateOf(0) }
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Sve", "Fotografije", "Videozapisi", "Favoriti")
    LaunchedEffect(loadAttempt) {
        loading = true
        loadFailed = false
        try {
            media = withContext(Dispatchers.IO) { MediaLibrary.list(context) }
        } catch (_: Exception) {
            loadFailed = true
        } finally {
            loading = false
        }
    }
    val selected = media.filter { item ->
        when(tab) {
            1 -> !item.isVideo
            2 -> item.isVideo
            3 -> MediaActions.isFavorite(context, item.uri)
            else -> true
        }
    }
    Column(Modifier.fillMaxSize().background(LensigoReferenceBackground)) {
        Row(Modifier.fillMaxWidth().height(65.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Natrag", tint = GalleryText)
            }
            Text("Galerija", modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                color = GalleryText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, "Postavke", tint = GalleryText)
            }
        }
        Column(Modifier.padding(horizontal = 18.dp, vertical = 17.dp)) {
            Text("Moji trenutci", color = GalleryText, fontSize = 29.sp,
                fontWeight = FontWeight.Bold)
            Text("Fotografije i videozapisi na uređaju.",
                color = GalleryMuted, fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 340.dp || (maxWidth < 550.dp && LocalDensity.current.fontScale >= 1.3f)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        items(tabs.size) { index ->
                            GalleryFilterTab(
                                tabs[index], selected = tab == index,
                                onClick = { tab = index },
                                modifier = Modifier.width(88.dp)
                            )
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        tabs.forEachIndexed { index, name ->
                            GalleryFilterTab(
                                name, selected = tab == index,
                                onClick = { tab = index },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GalleryCyan)
            }
            loadFailed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Galeriju nije moguće učitati", color = GalleryMuted, fontSize = 14.sp)
                    OutlinedButton(onClick = { loadAttempt++ }) {
                        Text("Pokušaj ponovno")
                    }
                }
            }
            selected.isEmpty() -> Box(Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center) {
                Text(if (tab == 3) "Nema favorita" else "Nema spremljenih LENSIGO snimki",
                    color = GalleryMuted, fontSize = 14.sp)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.padding(horizontal = 17.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(selected, key = { it.uri.toString() }) { item ->
                    val image by produceState<Bitmap?>(null, item.uri) {
                        // A cancelled withContext may drop a bitmap generated by
                        // the worker before it can be handed to Compose.
                        var unpublished: Bitmap? = null
                        try {
                            withContext(Dispatchers.IO) {
                                unpublished = MediaLibrary.thumbnail(context, item, 320)
                            }
                            value = unpublished
                            unpublished = null // DisposableEffect owns it now.
                        } finally {
                            unpublished?.let { if (!it.isRecycled) it.recycle() }
                        }
                    }
                    DisposableEffect(image) {
                        val thumbnail = image
                        onDispose {
                            if (thumbnail?.isRecycled == false) thumbnail.recycle()
                        }
                    }
                    Box(Modifier.aspectRatio(.72f)
                        .background(Color(0xFF153247), RoundedCornerShape(9.dp))
                        .clickable(
                            role = Role.Button,
                            onClickLabel = if (item.isVideo) "Otvori videozapis" else "Otvori fotografiju"
                        ) { onOpen(item) }) {
                        if (image != null) Image(image!!.asImageBitmap(), null,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        if (item.isVideo) Icon(Icons.Default.PlayArrow, "Video",
                            tint = GalleryText,
                            modifier = Modifier.align(Alignment.TopEnd).padding(5.dp))
                        if (MediaActions.isFavorite(context, item.uri)) {
                            Icon(Icons.Default.Favorite, "Favorit", tint = GalleryCyan,
                                modifier = Modifier.align(Alignment.BottomEnd)
                                    .padding(6.dp).size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryFilterTab(
    name: String, selected: Boolean,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Box(
        modifier.heightIn(min = 40.dp)
            .background(
                if (selected) Color(0xFF0E4A68) else Color(0xFF132A3C),
                RoundedCornerShape(22.dp)
            )
            .border(1.dp, Color(0xFF29546C), RoundedCornerShape(22.dp))
            .clickable(
                role = androidx.compose.ui.semantics.Role.Tab,
                onClickLabel = "Prikaži $name", onClick = onClick
            )
            .semantics {
                stateDescription = if (selected) "Odabrano" else "Nije odabrano"
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            name, color = if (selected) GalleryCyan else GalleryText,
            fontSize = 10.sp, maxLines = 1, textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ViewerScreen(
    item: LensMedia, quality: Int, lowMemory: Boolean,
    onBack: () -> Unit, onSaved: () -> Unit,
    onSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editing by remember(item.uri) { mutableStateOf(false) }
    var tool by remember { mutableIntStateOf(0) }
    var bitmap by remember(item.uri) { mutableStateOf<Bitmap?>(null) }
    var imageLoading by remember(item.uri) { mutableStateOf(true) }
    var viewerScale by remember(item.uri) { mutableFloatStateOf(1f) }
    var viewerPanX by remember(item.uri) { mutableFloatStateOf(0f) }
    var viewerPanY by remember(item.uri) { mutableFloatStateOf(0f) }
    var busy by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var actionsMenu by remember(item.uri) { mutableStateOf(false) }
    var favorite by remember(item.uri) {
        mutableStateOf(MediaActions.isFavorite(context, item.uri))
    }
    var strength by remember { mutableFloatStateOf(.85f) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var chosen by remember { mutableStateOf(LensFilter.ORIGINAL) }
    var rotation by remember { mutableIntStateOf(0) }
    var crop by remember { mutableIntStateOf(0) }
    val cropAspects = listOf(null, 1f, 4f / 3f, 3f / 2f, 9f / 16f)
    val cropNames = listOf("Izvorno", "1:1", "4:3", "3:2", "9:16")
    val look = PhotoLook(filter = chosen, intensity = strength,
        brightness = brightness, contrast = contrast, saturation = saturation)

    // Preview and saved output share the same crop / rotate / filter pipeline.
    // Debounce sliders and work on <= 840px to stay responsive on low-RAM phones.
    val editedPreview by produceState<Bitmap?>(
        initialValue = null, bitmap, editing, look, crop, rotation
    ) {
        val source = bitmap
        if (!editing || source == null || item.isVideo) {
            value = null
        } else {
            delay(125)
            var unpublished: Bitmap? = null
            try {
                withContext(Dispatchers.Default) {
                    val maxEdge = kotlin.math.max(source.width, source.height)
                    val small = if (maxEdge > 840) {
                        val ratio = 840f / maxEdge
                        Bitmap.createScaledBitmap(source,
                            (source.width * ratio).toInt().coerceAtLeast(1),
                            (source.height * ratio).toInt().coerceAtLeast(1), true)
                    } else source
                    try {
                        unpublished = MediaActions.renderEdit(
                            small, look, cropAspects[crop], rotation
                        )
                    } finally {
                        if (small !== source && !small.isRecycled) small.recycle()
                    }
                }
                value = unpublished
                unpublished = null // Preview disposer now owns the bitmap.
            } finally {
                unpublished?.let { if (!it.isRecycled) it.recycle() }
            }
        }
    }

    // Free native editor previews after a slider recomputation or leaving
    // Viewer. The original gallery thumbnail is owned separately.
    DisposableEffect(editedPreview) {
        val rendered = editedPreview
        onDispose {
            if (rendered != null && rendered !== bitmap && !rendered.isRecycled)
                rendered.recycle()
        }
    }
    DisposableEffect(bitmap) {
        val original = bitmap
        onDispose { if (original?.isRecycled == false) original.recycle() }
    }

    LaunchedEffect(item.uri) {
        imageLoading = true
        var unpublished: Bitmap? = null
        try {
            withContext(Dispatchers.IO) {
                unpublished = MediaLibrary.thumbnail(context, item, 1600)
            }
            bitmap = unpublished
            unpublished = null // Viewer DisposableEffect owns the result.
        } finally {
            unpublished?.let { if (!it.isRecycled) it.recycle() }
            imageLoading = false
        }
    }
    // The system Back gesture closes the editor before leaving the Viewer.
    // While saving, consume Back so an in-flight export is not abandoned.
    BackHandler(enabled = editing) {
        if (!busy) editing = false
    }
    LaunchedEffect(editing) {
        if (editing) {
            viewerScale = 1f
            viewerPanX = 0f
            viewerPanY = 0f
        }
    }
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT == 29) {
                // On Android 10 RecoverableSecurityException only grants
                // access; the app MUST retry the original delete operation.
                try {
                    val rows = context.contentResolver.delete(item.uri, null, null)
                    if (rows > 0) onBack()
                    else Toast.makeText(context, "Snimka nije izbrisana",
                        Toast.LENGTH_LONG).show()
                } catch (_: Exception) {
                    Toast.makeText(context, "Brisanje nakon dopuštenja nije uspjelo",
                        Toast.LENGTH_LONG).show()
                }
            } else {
                // On Android 11+ createDeleteRequest performs deletion itself.
                onBack()
            }
        } else Toast.makeText(context, "Brisanje otkazano", Toast.LENGTH_SHORT).show()
    }
    fun share() {
        try {
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = if (item.isVideo) "video/mp4" else "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, item.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Podijeli snimku"))
        } catch (_: Exception) {
            Toast.makeText(context, "Dijeljenje nije dostupno", Toast.LENGTH_LONG).show()
        }
    }
    fun delete() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                val request = MediaStore.createDeleteRequest(context.contentResolver, listOf(item.uri))
                deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            } else {
                val rows = context.contentResolver.delete(item.uri, null, null)
                if (rows > 0) onBack()
                else Toast.makeText(context, "Snimka nije izbrisana",
                    Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= 29 &&
                e is android.app.RecoverableSecurityException) {
                deleteLauncher.launch(
                    IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
            } else {
                Toast.makeText(context, "Brisanje nije moguće: " + e.localizedMessage,
                    Toast.LENGTH_LONG).show()
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Izbrisati snimku?") },
        text = { Text("Datoteka će biti izbrisana iz galerije uređaja.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; delete() }) {
            Text("Izbriši")
        } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) {
            Text("Odustani")
        } }
    )
    Column(Modifier.fillMaxSize().then(if (editing)
        Modifier.background(LensigoReferenceBackground)
        else Modifier.background(GalleryInk))) {
        Row(Modifier.fillMaxWidth().height(61.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (editing) editing = false else onBack() }) {
                Icon(Icons.Default.ArrowBack, "Natrag", tint = GalleryText)
            }
            Text(if (editing) "Uređivanje" else if (item.isVideo) "Videozapis" else "Fotografija",
                color = GalleryText, fontSize = 18.sp,
                textAlign = TextAlign.Center, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))
            if (editing) {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, "Postavke", tint = GalleryText)
                }
            } else {
                Box {
                    IconButton(onClick = { actionsMenu = true }) {
                        Icon(Icons.Default.MoreHoriz, "Dodatne radnje", tint = GalleryText)
                    }
                    DropdownMenu(
                        expanded = actionsMenu, onDismissRequest = { actionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (favorite) "Ukloni iz favorita" else "Dodaj u favorite") },
                            onClick = {
                                favorite = MediaActions.toggleFavorite(context, item.uri)
                                actionsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Podijeli") },
                            onClick = { actionsMenu = false; share() }
                        )
                        DropdownMenuItem(
                            text = { Text("Spremi kopiju") },
                            enabled = !busy,
                            onClick = {
                                actionsMenu = false
                                busy = true
                                scope.launch {
                                    try {
                                        MediaActions.saveCopy(context, item)
                                        Toast.makeText(context, "Kopija spremljena",
                                            Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context,
                                            "Pogreška spremanja: " + e.localizedMessage,
                                            Toast.LENGTH_LONG).show()
                                    } finally { busy = false }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Izbriši") },
                            onClick = { actionsMenu = false; confirmDelete = true }
                        )
                    }
                }
            }
        }
        if (editing) Column(Modifier.padding(horizontal = 18.dp, vertical = 15.dp)) {
            Text("Uredi fotografiju", fontWeight = FontWeight.Bold,
                fontSize = 27.sp, color = GalleryText)
            Text("Prilagodi svaki detalj kadra.", color = GalleryMuted, fontSize = 14.sp)
        }
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()
            .padding(horizontal = if (editing) 18.dp else 0.dp)
            .then(if (!editing && !item.isVideo) Modifier.pointerInput(item.uri) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val scale = (viewerScale * zoom).coerceIn(.6f, 5f)
                    viewerScale = scale
                    // The viewport itself supplies hard pan bounds; panning
                    // is unavailable at <=1x so a fit-to-screen photo stays centered.
                    val maxX = (size.width * (scale - 1f) * .5f).coerceAtLeast(0f)
                    val maxY = (size.height * (scale - 1f) * .5f).coerceAtLeast(0f)
                    viewerPanX = (viewerPanX + pan.x).coerceIn(-maxX, maxX)
                    viewerPanY = (viewerPanY + pan.y).coerceIn(-maxY, maxY)
                }
            } else Modifier),
            contentAlignment = Alignment.Center) {
            val img = if (editing) editedPreview ?: bitmap else bitmap
            if (img != null) AndroidView(
                factory = { ctx -> ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                } },
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    val scale = if (!editing && !item.isVideo) viewerScale else 1f
                    scaleX = scale
                    scaleY = scale
                    translationX = if (!editing && !item.isVideo) viewerPanX else 0f
                    translationY = if (!editing && !item.isVideo) viewerPanY else 0f
                },
                update = {
                    it.setImageBitmap(img)
                    it.colorFilter = null
                }
            ) else if (imageLoading) {
                CircularProgressIndicator(color = GalleryCyan)
            } else if (!item.isVideo) {
                Text("Pregled fotografije nije dostupan",
                    color = GalleryMuted, fontSize = 14.sp)
            }
            if (!editing && !item.isVideo && img != null) {
                Row(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
                        .background(GalleryInk.copy(alpha = .85f), RoundedCornerShape(32.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(.6f to "0.6", 1f to "1x", 2f to "2x", 5f to "5x")
                        .forEach { (zoom, label) ->
                            val selected = kotlin.math.abs(viewerScale - zoom) < .12f
                            Box(
                                Modifier.size(width = 46.dp, height = 34.dp)
                                    .background(
                                        if (selected) GalleryCyan else Color.Transparent,
                                        RoundedCornerShape(22.dp)
                                    )
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = "Prikaži fotografiju na $label"
                                    ) {
                                        viewerScale = zoom
                                        viewerPanX = 0f
                                        viewerPanY = 0f
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (selected) GalleryInk else GalleryText,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                }
            }
            if (item.isVideo) {
                Button(onClick = {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(item.uri, "video/mp4")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }) } catch (_: Exception) {
                        Toast.makeText(context, "Reproduktor nije dostupan", Toast.LENGTH_SHORT).show()
                    }
                }) { Icon(Icons.Default.PlayArrow, null); Text("Reproduciraj") }
            }
        }
        if (editing) {
            Row(Modifier.fillMaxWidth().height(74.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("Svjetlina","Kontrast","Izreži","Filteri","Rotacija").forEachIndexed { index, name ->
                    Column(Modifier.weight(1f).clickable { tool = index }
                        .padding(vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(when (index) {
                            0 -> Icons.Default.Tune
                            1 -> Icons.Default.WbSunny
                            2 -> Icons.Default.Crop
                            3 -> Icons.Default.AutoAwesome
                            else -> Icons.Default.RotateRight
                        }, name, tint = if (tool == index) GalleryCyan else GalleryMuted,
                            modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(name, fontSize = 10.sp, maxLines = 1,
                            color = if (tool == index) GalleryCyan else GalleryMuted)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().height(106.dp)
                .padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                when (tool) {
                    0 -> {
                        Text("Svjetlina " + (brightness * 100).toInt(), color = GalleryText)
                        Slider(brightness, { brightness = it }, valueRange = -1f..1f)
                    }
                    1 -> {
                        Text("Kontrast " + (contrast * 100).toInt() + "%", color = GalleryText)
                        Slider(contrast, { contrast = it }, valueRange = .5f..1.8f)
                    }
                    2 -> LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        items(cropNames.size) { index ->
                            FilterChip(selected = crop == index, onClick = { crop = index },
                                label = { Text(cropNames[index]) })
                        }
                    }
                    3 -> {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            items(LensFilter.entries.size) { index ->
                                val selected = LensFilter.entries[index]
                                FilterChip(selected = chosen == selected,
                                    onClick = { chosen = selected },
                                    label = { Text(selected.title) })
                            }
                        }
                        Slider(strength, { strength = it },
                            modifier = Modifier.height(24.dp))
                    }
                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rotacija " + rotation + "°", color = GalleryText)
                        Spacer(Modifier.width(16.dp))
                        OutlinedButton(onClick = { rotation = (rotation + 90) % 360 }) {
                            Text("+90°")
                        }
                    }
                }
            }
            Button(
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            MediaActions.saveEdited(context, item.uri, look, cropAspects[crop],
                                rotation, quality, lowMemory)
                            onSaved()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Pogreška: " + e.localizedMessage,
                                Toast.LENGTH_LONG).show()
                        } finally { busy = false }
                    }
                }, enabled = !busy && bitmap != null,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                    .padding(bottom = 16.dp).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D83AB)),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (busy) "Spremanje…" else "Spremi promjene", color = GalleryText) }
        } else {
            Row(Modifier.fillMaxWidth().height(124.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically) {
                BottomAction("Uredi", Icons.Default.Edit, !item.isVideo && bitmap != null) { editing = true }
                BottomAction("Podijeli", Icons.Default.Share) { share() }
                BottomAction("Spremi", Icons.Default.FileDownload, !busy) {
                    busy = true
                    scope.launch {
                        try {
                            MediaActions.saveCopy(context, item)
                            Toast.makeText(context, "Kopija spremljena",
                                Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Pogreška spremanja: " + e.localizedMessage,
                                Toast.LENGTH_LONG).show()
                        } finally { busy = false }
                    }
                }
                BottomAction("Izbriši", Icons.Default.Delete) { confirmDelete = true }
            }
        }
    }
}

@Composable
private fun BottomAction(name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true, onClick: () -> Unit
) {
    Column(Modifier.clickable(enabled = enabled, onClick = onClick).padding(9.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, name, tint = if (enabled) GalleryText else GalleryMuted.copy(alpha = .35f),
            modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(10.dp))
        Text(name, color = if (enabled) GalleryText else GalleryMuted.copy(alpha = .35f),
            fontSize = 12.sp)
    }
}
