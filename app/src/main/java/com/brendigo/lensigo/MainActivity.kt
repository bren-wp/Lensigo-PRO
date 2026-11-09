package com.brendigo.lensigo

import android.Manifest
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Range
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.RenderEffect
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.net.Uri
import android.widget.Toast
import android.media.MediaActionSound
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ConcurrentCamera
import androidx.camera.core.UseCaseGroup
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.extensions.ExtensionMode
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private val Black = Color(0xFF03080F)
private val Navy = Color(0xFF0B2639)
private val Cyan = Color(0xFF00D4FF)
private val Blue = Color(0xFF2E83FF)
private val Muted = Color(0xFF9DB5C4)
private val White = Color(0xFFF7FCFF)

private enum class Screen { SPLASH, WELCOME, PERMISSIONS, HOME, CAMERA, GALLERY, VIEWER, SETTINGS, CAMERA_SETTINGS, ABOUT, LICENCES }
internal enum class CameraMode(val title: String) {
    VIDEO("VIDEO"), PHOTO("FOTO"), PRO("PRO"), NIGHT("NOĆ"),
    PORTRAIT("PORTRET"), PANORAMA("PANORAMA"), DUAL("DUAL"), FILTERS("FILTERI")
}

// Match the actual 05_photo and 06_video reference strip rather than enum order:
// FILTERI · VIDEO · FOTO · PRO · NOĆ are visible around the selected photo mode.
internal val cameraModeOrder = listOf(
    CameraMode.FILTERS, CameraMode.VIDEO, CameraMode.PHOTO,
    CameraMode.PRO, CameraMode.NIGHT, CameraMode.PORTRAIT,
    CameraMode.PANORAMA, CameraMode.DUAL
)

// Only the visual navigation edges wrap: the eight CameraX modes themselves
// remain unique. Source snapshots show five labels around the selected mode.
// Two left / two right labels are provided for all eight modes.
internal val cameraModeStrip =
    listOf(CameraMode.PANORAMA, CameraMode.DUAL) + cameraModeOrder +
        listOf(CameraMode.FILTERS, CameraMode.VIDEO)

internal fun cameraModeScrollIndex(mode: CameraMode): Int {
    val index = when (mode) {
        CameraMode.PANORAMA, CameraMode.DUAL -> cameraModeStrip.lastIndexOf(mode)
        else -> cameraModeStrip.indexOf(mode)
    }
    return (index - 2).coerceAtLeast(0)
}

/** Positive means content must move left to center the chosen mode. */
internal fun cameraModeCenterDeltaPx(
    itemStart: Int, itemSize: Int, viewportStart: Int, viewportEnd: Int
): Float = itemStart + itemSize / 2f - (viewportStart + viewportEnd) / 2f

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        setContent { LensigoApplication() }
    }
}

@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
@Composable
private fun LensigoApplication() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("lensigo_preferences", 0) }
    var screen by remember { mutableStateOf(Screen.SPLASH) }
    // The reference Splash has no system chrome. Restore OS bars on every
    // interactive screen so navigation gestures remain available.
    LaunchedEffect(screen) {
        (context as? Activity)?.let { activity ->
            val controller = WindowCompat.getInsetsController(
                activity.window, activity.window.decorView
            )
            if (screen == Screen.SPLASH) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    LaunchedEffect(Unit) {
        delay(750)
        screen = if (prefs.getBoolean("onboarded", false)) Screen.CAMERA else Screen.WELCOME
    }
    var mode by remember { mutableStateOf(CameraMode.PHOTO) }
    var settingsReturnScreen by remember { mutableStateOf(Screen.CAMERA) }
    var aboutReturnScreen by remember { mutableStateOf(Screen.SETTINGS) }
    var galleryReturnScreen by remember { mutableStateOf(Screen.CAMERA) }
    var front by remember { mutableStateOf(false) }
    var pipOnLeft by remember { mutableStateOf(false) }
    val modeListState = rememberLazyListState()
    LaunchedEffect(mode) {
        // Source 05–12: active mode text sits exactly in the middle, not
        // merely after two previous variable-width labels. Scroll to the
        // correct cyclic neighborhood, then center its actual measured item.
        val leading = cameraModeScrollIndex(mode)
        modeListState.animateScrollToItem(leading)
        val selected = modeListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == leading + 2 }
        val layout = modeListState.layoutInfo
        if (selected != null) {
            val delta = cameraModeCenterDeltaPx(
                selected.offset, selected.size,
                layout.viewportStartOffset, layout.viewportEndOffset
            )
            if (kotlin.math.abs(delta) > 1f) modeListState.animateScrollBy(delta)
        }
    }
    var photoAspectWide by remember {
        mutableStateOf(prefs.getBoolean("photo_aspect_wide", false))
    }
    var aspectWide by remember { mutableStateOf(photoAspectWide) }
    var preferSD by remember { mutableStateOf(prefs.getBoolean("video_sd", false)) }
    var prefer60Fps by remember { mutableStateOf(prefs.getBoolean("video_60fps", false)) }
    var eisEnabled by remember { mutableStateOf(prefs.getBoolean("video_stabilization", false)) }
    var eisSupported by remember { mutableStateOf(false) }
    var rawEnabled by remember { mutableStateOf(prefs.getBoolean("raw_jpeg", false)) }
    var rawSupported by remember { mutableStateOf(false) }
    var fps60Supported by remember { mutableStateOf(false) }
    var recordAudio by remember { mutableStateOf(prefs.getBoolean("record_audio", true)) }
    var nightProcessing by remember {
        mutableStateOf(prefs.getBoolean("night_processing", true))
    }
    var hdrEnabled by remember {
        mutableStateOf(prefs.getBoolean("hdr_processing", false))
    }
    var hdrSupported by remember { mutableStateOf(false) }
    var lowMemory by remember { mutableStateOf(prefs.getBoolean("low_memory", true)) }
    var softenNoise by remember { mutableStateOf(prefs.getBoolean("noise", false)) }
    var sharpen by remember { mutableStateOf(prefs.getBoolean("sharpen", false)) }
    var jpegQuality by remember { mutableIntStateOf(prefs.getInt("quality", 92)) }
    var filter by remember { mutableStateOf(LensFilter.ORIGINAL) }
    var intensity by remember { mutableFloatStateOf(.85f) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var flash by remember { mutableStateOf(false) }
    var gridEnabled by remember { mutableStateOf(prefs.getBoolean("grid", true)) }
    var timerSeconds by remember { mutableIntStateOf(prefs.getInt("timer", 0)) }
    var countdown by remember { mutableIntStateOf(0) }
    var shutterTimer: Job? by remember { mutableStateOf<Job?>(null) }
    var shutterSound by remember { mutableStateOf(prefs.getBoolean("shutter_sound", false)) }
    val sound = remember { MediaActionSound() }
    DisposableEffect(Unit) { onDispose { sound.release() } }
    var portraitBlur by remember { mutableFloatStateOf(.60f) }
    var vendorEffect by remember { mutableStateOf(false) }
    var nightExposureExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(mode) { nightExposureExpanded = false }
    var proCapabilities by remember { mutableStateOf<ProCapabilities?>(null) }
    var proValues by remember { mutableStateOf(ProValues()) }
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var hasStoragePermission by remember {
        mutableStateOf(Build.VERSION.SDK_INT >= 29 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
    }
    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    // Re-check permissions if the user changes them in Android system settings.
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                hasMicPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                hasStoragePermission = Build.VERSION.SDK_INT >= 29 ||
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    val requestCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasStoragePermission = Build.VERSION.SDK_INT >= 29 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
    val requestCameraOnly = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPermission = it
    }
    val requestStorageOnly = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasStoragePermission = Build.VERSION.SDK_INT >= 29 || it
    }
    val requestMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasMicPermission = it
    }
    fun requestPermissions() {
        requestCamera.launch(if (Build.VERSION.SDK_INT < 29)
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            else arrayOf(Manifest.permission.CAMERA))
    }
    fun openAppSystemSettings() {
        runCatching {
            context.startActivity(Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            ))
        }.onFailure {
            Toast.makeText(context, "Postavke nisu dostupne", Toast.LENGTH_SHORT).show()
        }
    }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var selfieView by remember { mutableStateOf<PreviewView?>(null) }
    var selfieCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var concurrentDual by remember { mutableStateOf(false) }
    var dualStep by remember { mutableIntStateOf(0) }
    var firstDualPhoto by remember { mutableStateOf<File?>(null) }
    val panoramaParts = remember { mutableStateListOf<File>() }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }
    var photoCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationNanos by remember { mutableLongStateOf(0L) }
    var processing by remember { mutableStateOf(false) }
    var currentExposure by remember { mutableIntStateOf(0) }
    var exposureMin by remember { mutableIntStateOf(0) }
    var exposureMax by remember { mutableIntStateOf(0) }
    var zoomMin by remember { mutableFloatStateOf(1f) }
    var zoomMax by remember { mutableFloatStateOf(1f) }
    var currentZoom by remember { mutableFloatStateOf(1f) }
    var lastPhoto by remember { mutableStateOf<Uri?>(null) }
    var lastPhotoThumbnail by remember { mutableStateOf<ImageBitmap?>(null) }
    DisposableEffect(lastPhotoThumbnail) {
        val bitmap = lastPhotoThumbnail?.asAndroidBitmap()
        onDispose { if (bitmap?.isRecycled == false) bitmap.recycle() }
    }
    LaunchedEffect(lastPhoto) {
        lastPhotoThumbnail = null
        lastPhoto?.let { uri ->
            val bitmap = withContext(Dispatchers.IO) {
                MediaLibrary.thumbnail(context, LensMedia(uri, false, 0), 96)
            }
            if (bitmap != null) lastPhotoThumbnail = bitmap.asImageBitmap()
        }
    }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    LaunchedEffect(mode, front) { focusPoint = null }
    var activeMedia by remember { mutableStateOf<LensMedia?>(null) }
    val currentRecording by rememberUpdatedState(recording)
    // CameraX releases camera use cases with the activity lifecycle, but the
    // countdown and active recording also need explicit cancellation on stop.
    DisposableEffect(lifecycle) {
        val stopObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                shutterTimer?.cancel()
                shutterTimer = null
                countdown = 0
                if (isRecording) {
                    runCatching { currentRecording?.stop() }
                }
            }
        }
        lifecycle.lifecycle.addObserver(stopObserver)
        onDispose { lifecycle.lifecycle.removeObserver(stopObserver) }
    }
    DisposableEffect(Unit) { onDispose {
        shutterTimer?.cancel()
        currentRecording?.stop()
    } }

    val look = PhotoLook(
        filter = if (mode == CameraMode.FILTERS) filter else LensFilter.ORIGINAL,
        intensity = intensity,
        brightness = if (mode == CameraMode.NIGHT && nightProcessing && !vendorEffect)
            brightness.coerceAtLeast(.15f) else brightness,
        contrast = contrast, saturation = saturation,
        lowLight = mode == CameraMode.NIGHT && nightProcessing && !vendorEffect,
        softenNoise = softenNoise, sharpen = sharpen,
        portraitBlur = if (mode == CameraMode.PORTRAIT && !vendorEffect) portraitBlur else 0f
    )
    DisposableEffect(previewView, selfieView, screen, front, mode, aspectWide, preferSD,
        prefer60Fps, eisEnabled, rawEnabled, dualStep, nightProcessing,
        hdrEnabled, hasCameraPermission) {
        val view = previewView
        if (view == null || screen != Screen.CAMERA || !hasCameraPermission) {
            onDispose { }
        } else {
            var disposed = false
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                if (!disposed) try {
                    val provider = future.get()
                    provider.unbindAll()
                    val preview = Preview.Builder()
                        .setTargetAspectRatio(if (aspectWide) AspectRatio.RATIO_16_9 else AspectRatio.RATIO_4_3)
                        .build().also { it.surfaceProvider = view.surfaceProvider }
                    val selector = if (mode == CameraMode.DUAL)
                        if (dualStep == 1) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                        else if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                    val recordingMode = mode == CameraMode.VIDEO
                    activeCamera = null
                    photoCapture = null
                    videoCapture = null
                    selfieCapture = null
                    concurrentDual = false
                    fun onBound(camera: Camera, manufacturerExtension: Boolean) {
                        if (disposed) return
                        activeCamera = camera
                        vendorEffect = manufacturerExtension
                        proCapabilities = if (mode == CameraMode.PRO)
                            ProCameraControl.capabilities(camera) else null
                        val exposure = camera.cameraInfo.exposureState
                        exposureMin = exposure.exposureCompensationRange.lower
                        exposureMax = exposure.exposureCompensationRange.upper
                        currentExposure = exposure.exposureCompensationIndex
                        val z = camera.cameraInfo.zoomState.value
                        zoomMin = z?.minZoomRatio ?: 1f
                        zoomMax = z?.maxZoomRatio ?: 1f
                        currentZoom = z?.zoomRatio ?: 1f
                        if (!camera.cameraInfo.hasFlashUnit()) flash = false
                    }
                    if (recordingMode) {
                        val preferred = if (preferSD) Quality.SD else Quality.UHD
                        val selectorQuality = QualitySelector.fromOrderedList(
                            if (preferSD) {
                                listOf(Quality.SD, Quality.HD, Quality.FHD, Quality.UHD)
                            } else {
                                // Use up to UHD/4K if available, then FHD/HD/SD.
                                // Frame rate remains device-controlled; never promise 60 FPS.
                                listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD)
                            },
                            FallbackStrategy.lowerQualityOrHigherThan(preferred)
                        )
                        val recorder = Recorder.Builder()
                            .setQualitySelector(selectorQuality).build()
                        val availableFps = runCatching {
                            Camera2CameraInfo.from(provider.getCameraInfo(selector))
                                .getCameraCharacteristic(
                                    CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
                                )?.toList().orEmpty()
                        }.getOrDefault(emptyList())
                        fps60Supported = CameraControlRules.supports60Fps(
                            availableFps.map { it.lower..it.upper }
                        )
                        eisSupported = runCatching {
                            Camera2CameraInfo.from(provider.getCameraInfo(selector))
                                .getCameraCharacteristic(CameraCharacteristics
                                    .CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
                                ?.contains(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON) == true
                        }.getOrDefault(false)
                        val canRequest60 = prefer60Fps && fps60Supported
                        val requested = VideoCapture.Builder(recorder).apply {
                            if (canRequest60) setTargetFrameRate(Range(60, 60))
                            if (eisEnabled && eisSupported)
                                setVideoStabilizationEnabled(true)
                        }.build()
                        try {
                            onBound(provider.bindToLifecycle(lifecycle, selector, preview, requested), false)
                            videoCapture = requested; photoCapture = null
                        } catch (error: Exception) {
                            if (!canRequest60) throw error
                            // A camera can offer 60fps but reject it together
                            // with the requested UHD/Preview stream. Fall back
                            // to the standard, usable recording configuration.
                            provider.unbindAll()
                            val fallback = VideoCapture.withOutput(recorder)
                            onBound(provider.bindToLifecycle(
                                lifecycle, selector, preview, fallback
                            ), false)
                            videoCapture = fallback; photoCapture = null
                            Toast.makeText(context,
                                "60 FPS nije podržano za ovu kombinaciju, koristi se Auto",
                                Toast.LENGTH_LONG).show()
                        }
                    } else {
                        if (mode == CameraMode.PHOTO) {
                            // Availability depends on the currently selected
                            // real lens. Query even when HDR is disabled so
                            // Settings never offers an unverified switch.
                            val hdrFuture = ExtensionsManager.getInstanceAsync(context, provider)
                            hdrFuture.addListener({
                                if (!disposed) {
                                    hdrSupported = runCatching {
                                        hdrFuture.get().isExtensionAvailable(
                                            selector, ExtensionMode.HDR
                                        )
                                    }.getOrDefault(false)
                                }
                            }, ContextCompat.getMainExecutor(context))
                        } else {
                            hdrSupported = false
                        }
                        val supportsDng = mode == CameraMode.PRO && runCatching {
                            ImageCapture.getImageCaptureCapabilities(
                                provider.getCameraInfo(selector)
                            ).supportedOutputFormats.contains(
                                ImageCapture.OUTPUT_FORMAT_RAW_JPEG
                            )
                        }.getOrDefault(false)
                        rawSupported = supportsDng
                        val capture = ImageCapture.Builder()
                            .setTargetAspectRatio(if (aspectWide) AspectRatio.RATIO_16_9 else AspectRatio.RATIO_4_3)
                            .setCaptureMode(if (mode == CameraMode.NIGHT)
                                ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                                else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .apply {
                                if (rawEnabled && supportsDng)
                                    setOutputFormat(ImageCapture.OUTPUT_FORMAT_RAW_JPEG)
                            }
                            .build()
                        photoCapture = capture; videoCapture = null
                        val ext = when (mode) {
                            CameraMode.PHOTO -> if (hdrEnabled) ExtensionMode.HDR else null
                            CameraMode.NIGHT -> if (nightProcessing) ExtensionMode.NIGHT else null
                            CameraMode.PORTRAIT -> ExtensionMode.BOKEH
                            else -> null
                        }
                        val secondView = selfieView
                        val compatiblePair = if (mode == CameraMode.DUAL && secondView != null &&
                            dualStep == 0) {
                            provider.availableConcurrentCameraInfos.firstOrNull { pair ->
                                pair.size == 2 && pair.any {
                                    it.lensFacing == CameraSelector.LENS_FACING_BACK
                                } && pair.any { it.lensFacing == CameraSelector.LENS_FACING_FRONT }
                            }
                        } else null
                        if (compatiblePair != null) {
                            try {
                                val backCamera = compatiblePair.first {
                                    it.lensFacing == CameraSelector.LENS_FACING_BACK
                                }
                                val frontCamera = compatiblePair.first {
                                    it.lensFacing == CameraSelector.LENS_FACING_FRONT
                                }
                                val secondPreview = Preview.Builder().build().also {
                                    it.surfaceProvider = secondView!!.surfaceProvider
                                }
                                val selfieImage = ImageCapture.Builder().build()
                                val backGroup = UseCaseGroup.Builder()
                                    .addUseCase(preview).addUseCase(capture).build()
                                val frontGroup = UseCaseGroup.Builder()
                                    .addUseCase(secondPreview).addUseCase(selfieImage).build()
                                val pair = provider.bindToLifecycle(
                                    listOf(
                                        ConcurrentCamera.SingleCameraConfig(
                                            backCamera.cameraSelector, backGroup, lifecycle),
                                        ConcurrentCamera.SingleCameraConfig(
                                            frontCamera.cameraSelector, frontGroup, lifecycle)
                                    )
                                )
                                selfieCapture = selfieImage
                                concurrentDual = true
                                onBound(pair.cameras.first(), false)
                            } catch (_: Exception) {
                                provider.unbindAll()
                                onBound(provider.bindToLifecycle(
                                    lifecycle, selector, preview, capture), false)
                            }
                        } else if (ext == null) {
                            onBound(provider.bindToLifecycle(lifecycle, selector, preview, capture), false)
                        } else {
                            // Vendor Night/Bokeh is optional; on incompatible hardware use
                            // the real local enhancement path, never a fake extension.
                            val extensionFuture = ExtensionsManager.getInstanceAsync(context, provider)
                            extensionFuture.addListener({
                                if (!disposed) {
                                    val supportedCamera = try {
                                        val manager = extensionFuture.get()
                                        if (manager.isExtensionAvailable(selector, ext)) {
                                            val enabled = manager.getExtensionEnabledCameraSelector(selector, ext)
                                            provider.bindToLifecycle(lifecycle, enabled, preview, capture)
                                        } else null
                                    } catch (_: Exception) { null }
                                    if (!disposed) try {
                                        if (supportedCamera != null) {
                                            onBound(supportedCamera, true)
                                        } else {
                                            provider.unbindAll()
                                            onBound(provider.bindToLifecycle(lifecycle, selector, preview, capture), false)
                                        }
                                    } catch (e: Exception) {
                                        activeCamera = null
                                        Toast.makeText(context, "Kamera nije dostupna: " + e.localizedMessage,
                                            Toast.LENGTH_LONG).show()
                                    }
                                }
                            }, ContextCompat.getMainExecutor(context))
                        }
                    }
                } catch (e: Exception) {
                    activeCamera = null; photoCapture = null; videoCapture = null
                    Toast.makeText(context, "Kamera nije dostupna: " + e.localizedMessage,
                        Toast.LENGTH_LONG).show()
                }
            }, ContextCompat.getMainExecutor(context))
            onDispose {
                disposed = true
                if (future.isDone) try { future.get().unbindAll() } catch (_: Exception) {}
                activeCamera = null
                photoCapture = null
                videoCapture = null
                selfieCapture = null
                concurrentDual = false
            }
        }
    }
    LaunchedEffect(activeCamera, mode, proValues, proCapabilities) {
        val camera = activeCamera
        val caps = proCapabilities
        if (camera != null && caps != null && mode == CameraMode.PRO) {
            delay(160)
            runCatching { ProCameraControl.apply(camera, caps, proValues) }
        }
    }
    LaunchedEffect(previewView, flash, mode, activeCamera) {
        val hasFlash = activeCamera?.cameraInfo?.hasFlashUnit() == true
        photoCapture?.flashMode = if (flash && hasFlash) ImageCapture.FLASH_MODE_ON
            else ImageCapture.FLASH_MODE_OFF
        if (hasFlash) {
            activeCamera?.cameraControl?.enableTorch(flash && mode == CameraMode.VIDEO)
        }
    }
    LaunchedEffect(previewView, look, mode) {
        val view = previewView ?: return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= 31) {
            view.setRenderEffect(
                if (look.isNeutral) null else RenderEffect.createColorFilterEffect(FilterEngine.colorFilter(look))
            )
        }
    }
    fun clearTemporaryCaptures() {
        panoramaParts.forEach { it.delete() }
        panoramaParts.clear()
        firstDualPhoto?.delete()
        firstDualPhoto = null
        dualStep = 0
    }
    fun setMode(next: CameraMode) {
        if (isRecording || processing) return
        if (mode != next) {
            shutterTimer?.cancel()
            shutterTimer = null
            countdown = 0
            clearTemporaryCaptures()
        }
        if (mode != next) {
            // Remember a separate aspect ratio for video and still photos.
            aspectWide = if (next == CameraMode.VIDEO)
                prefs.getBoolean("video_aspect_wide", true)
                else photoAspectWide
        }
        mode = next
        brightness = 0f; contrast = 1f; saturation = 1f
        flash = false
        vendorEffect = false
        proValues = ProValues()
    }
    // Temporary file creation can fail when internal storage is full or
    // inaccessible. Never crash the camera UI while starting a capture.
    fun createCaptureTempFile(prefix: String): File? = try {
        File.createTempFile(prefix, ".jpg", context.cacheDir)
    } catch (error: Exception) {
        processing = false
        Toast.makeText(context, "Nema dovoljno prostora za snimanje", Toast.LENGTH_LONG).show()
        null
    }
    fun finishPanorama() {
        if (processing || panoramaParts.size < PanoramaComposer.MIN_FRAMES) return
        if (!hasStoragePermission) { requestPermissions(); return }
        processing = true
        val frames = panoramaParts.toList()
        panoramaParts.clear()
        scope.launch {
            try {
                lastPhoto = PanoramaComposer.save(context, frames, jpegQuality, lowMemory)
                Toast.makeText(context, "Panorama spremljena", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                frames.forEach { it.delete() }
                Toast.makeText(context, "Panorama nije uspjela: " + e.localizedMessage,
                    Toast.LENGTH_LONG).show()
            } finally { processing = false }
        }
    }
    fun capturePanorama() {
        if (processing || panoramaParts.size >= PanoramaComposer.MAX_FRAMES) return
        if (!hasStoragePermission) { requestPermissions(); return }
        val capture = photoCapture ?: return
        processing = true
        val file = createCaptureTempFile("lensigo_panorama_") ?: return
        capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    panoramaParts.add(file)
                    processing = false
                    if (panoramaParts.size == PanoramaComposer.MAX_FRAMES) finishPanorama()
                }
                override fun onError(error: ImageCaptureException) {
                    file.delete()
                    processing = false
                    Toast.makeText(context, error.localizedMessage, Toast.LENGTH_LONG).show()
                }
            })
    }
    fun captureDual() {
        if (processing || !hasCameraPermission) return
        if (!hasStoragePermission) { requestPermissions(); return }
        val capture = photoCapture ?: return
        processing = true
        val file = createCaptureTempFile("lensigo_dual_") ?: return
        capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    if (!concurrentDual && dualStep == 0) {
                        firstDualPhoto = file
                        dualStep = 1
                        processing = false
                        Toast.makeText(context,
                            "Stražnja snimka gotova. Snimi prednjom kamerom.",
                            Toast.LENGTH_LONG).show()
                        return
                    }
                    val frontCapture = if (concurrentDual) selfieCapture else null
                    fun composite(back: File, front: File) {
                        scope.launch {
                            try {
                                lastPhoto = DualComposer.save(context, back, front, jpegQuality,
                                    lowMemory, pipOnLeft)
                                Toast.makeText(context, "Dual fotografija spremljena",
                                    Toast.LENGTH_SHORT).show()
                            } catch (error: Exception) {
                                Toast.makeText(context, "Dual neuspješan: " + error.localizedMessage,
                                    Toast.LENGTH_LONG).show()
                            } finally {
                                back.delete(); front.delete()
                                firstDualPhoto = null; dualStep = 0; processing = false
                            }
                        }
                    }
                    if (frontCapture != null) {
                        val selfie = createCaptureTempFile("lensigo_selfie_")
                        if (selfie == null) {
                            file.delete()
                            firstDualPhoto?.delete()
                            firstDualPhoto = null
                            dualStep = 0
                            return
                        }
                        frontCapture.takePicture(ImageCapture.OutputFileOptions.Builder(selfie).build(),
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                    composite(file, selfie)
                                }
                                override fun onError(error: ImageCaptureException) {
                                    file.delete(); selfie.delete(); processing = false
                                    Toast.makeText(context, "Prednja kamera: " + error.message,
                                        Toast.LENGTH_LONG).show()
                                }
                            })
                    } else {
                        val back = firstDualPhoto
                        if (back == null) { file.delete(); processing = false; return }
                        composite(back, file)
                    }
                }
                override fun onError(error: ImageCaptureException) {
                    file.delete(); processing = false
                    Toast.makeText(context, "Dual: " + error.message, Toast.LENGTH_LONG).show()
                }
            })
    }
    fun capturePhoto() {
        if (processing) return
        if (!hasStoragePermission) { requestPermissions(); return }
        val capture = photoCapture ?: return
        processing = true
        val jpeg = createCaptureTempFile("lensigo_capture_") ?: return
        val useRaw = mode == CameraMode.PRO && rawEnabled && rawSupported &&
            capture.outputFormat == ImageCapture.OUTPUT_FORMAT_RAW_JPEG
        val raw = if (useRaw) runCatching {
            File.createTempFile("lensigo_dng_", ".dng", context.cacheDir)
        }.getOrNull() else null
        if (useRaw && raw == null) {
            jpeg.delete()
            processing = false
            Toast.makeText(context, "Nema prostora za DNG snimku", Toast.LENGTH_LONG).show()
            return
        }
        fun clean() { jpeg.delete(); raw?.delete() }
        var callbacks = 0
        var settled = false
        val callback = object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                if (settled) return
                callbacks++
                if (callbacks < if (raw != null) 2 else 1) return
                settled = true
                scope.launch {
                    try {
                        val uri = withContext(Dispatchers.IO) {
                            val jpegUri = MediaLibrary.saveImage(context, jpeg, look,
                                jpegQuality, lowMemory)
                            try {
                                if (raw != null) MediaLibrary.saveRaw(context, raw)
                            } catch (e: Exception) {
                                runCatching { context.contentResolver.delete(jpegUri, null, null) }
                                throw e
                            }
                            jpegUri
                        }
                        lastPhoto = uri
                        Toast.makeText(context,
                            if (raw != null) "JPEG i DNG spremljeni" else "Fotografija spremljena",
                            Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Spremanje nije uspjelo: " + e.localizedMessage,
                            Toast.LENGTH_LONG).show()
                    } finally {
                        clean()
                        processing = false
                    }
                }
            }
            override fun onError(error: ImageCaptureException) {
                if (settled) return
                settled = true
                clean()
                processing = false
                Toast.makeText(context, "Snimanje nije uspjelo: " + error.message,
                    Toast.LENGTH_LONG).show()
            }
        }
        try {
            val options = ImageCapture.OutputFileOptions.Builder(jpeg).build()
            if (raw != null) {
                capture.takePicture(ImageCapture.OutputFileOptions.Builder(raw).build(),
                    options, ContextCompat.getMainExecutor(context), callback)
            } else {
                capture.takePicture(options, ContextCompat.getMainExecutor(context), callback)
            }
        } catch (error: Exception) {
            settled = true
            clean()
            processing = false
            Toast.makeText(context, "Kamera nije mogla snimiti: " + error.localizedMessage,
                Toast.LENGTH_LONG).show()
        }
    }
    fun toggleRecording() {
        if (isRecording) {
            recording?.stop()
            return
        }
        if (!hasStoragePermission) { requestPermissions(); return }
        val capture = videoCapture ?: return
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "LENSIGO_" + System.currentTimeMillis() + ".mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/LENSIGO")
            }
        }
        try {
            val options = MediaStoreOutputOptions.Builder(
                context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).setContentValues(values).build()
            val pending = capture.output.prepareRecording(context, options)
            val configured = if (CameraControlRules.recordAudioEnabled(
                recordAudio, hasMicPermission
            )) pending.withAudioEnabled() else pending
            recordingDurationNanos = 0L
            recording = configured.start(ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Status) {
                    recordingDurationNanos = event.recordingStats.recordedDurationNanos
                }
                if (event is VideoRecordEvent.Finalize) {
                    isRecording = false
                    recording = null
                    if (event.hasError()) {
                        // CameraX can leave partial media behind after a failed
                        // recording. Only remove the URI created by this session.
                        val output = event.outputResults.outputUri
                        if (output != Uri.EMPTY) {
                            runCatching { context.contentResolver.delete(output, null, null) }
                        }
                        Toast.makeText(context, "Snimanje nije uspjelo: " + event.error,
                            Toast.LENGTH_LONG).show()
                    } else {
                        val savedUri = event.outputResults.outputUri
                        scope.launch {
                            val measured = withContext(Dispatchers.IO) {
                                VideoFileInspector.inspect(context, savedUri)
                            }
                            Toast.makeText(context, "Video spremljen · " + measured,
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            isRecording = true
        } catch (e: Exception) {
            isRecording = false
            Toast.makeText(context, "Video nije dostupan: " + e.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }
    fun onShutter() {
        if (countdown > 0 || processing || !hasCameraPermission) return
        if (mode == CameraMode.VIDEO) {
            toggleRecording()
            return
        }
        fun take() {
            if (shutterSound) runCatching {
                sound.play(MediaActionSound.SHUTTER_CLICK)
            }
            when (mode) {
                CameraMode.PANORAMA -> capturePanorama()
                CameraMode.DUAL -> captureDual()
                else -> capturePhoto()
            }
        }
        if (timerSeconds == 0) take()
        else {
            shutterTimer?.cancel()
            shutterTimer = scope.launch {
                try {
                    for (remaining in timerSeconds downTo 1) {
                        countdown = remaining
                        delay(1000)
                    }
                    countdown = 0
                    if (screen == Screen.CAMERA) take()
                } finally {
                    countdown = 0
                    shutterTimer = null
                }
            }
        }
    }
    fun openFromCamera() {
        if (isRecording || processing) return
        shutterTimer?.cancel()
        shutterTimer = null
        countdown = 0
        clearTemporaryCaptures()
        galleryReturnScreen = Screen.CAMERA
        screen = Screen.GALLERY
    }
    fun moveTo(next: Screen) {
        if (isRecording || processing) return
        if (screen != next) {
            shutterTimer?.cancel()
            shutterTimer = null
            countdown = 0
        }
        if (next == Screen.SETTINGS && screen !in setOf(
            Screen.SETTINGS, Screen.CAMERA_SETTINGS,
            Screen.ABOUT, Screen.LICENCES
        )) {
            settingsReturnScreen = screen
        }
        if (next == Screen.ABOUT && screen !in setOf(
            Screen.ABOUT, Screen.LICENCES
        )) {
            aboutReturnScreen = screen
        }
        if (next == Screen.GALLERY && screen !in setOf(
            Screen.GALLERY, Screen.VIEWER, Screen.SETTINGS
        )) {
            galleryReturnScreen = screen
        }
        if (next != Screen.CAMERA) clearTemporaryCaptures()
        screen = next
    }
    BackHandler(enabled = screen !in setOf(
        Screen.SPLASH, Screen.WELCOME, Screen.PERMISSIONS
    ) && !isRecording && !processing) {
        when (screen) {
            Screen.CAMERA -> moveTo(Screen.HOME)
            Screen.HOME -> (context as? Activity)?.finish()
            Screen.GALLERY -> moveTo(galleryReturnScreen)
            Screen.SETTINGS -> moveTo(settingsReturnScreen)
            Screen.VIEWER -> moveTo(Screen.GALLERY)
            Screen.CAMERA_SETTINGS -> moveTo(Screen.SETTINGS)
            Screen.ABOUT -> moveTo(aboutReturnScreen)
            Screen.LICENCES -> moveTo(Screen.ABOUT)
            else -> Unit
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Cyan, background = Black, surface = Navy, onBackground = White,
        onSurface = White
    )) {
        Box(Modifier.fillMaxSize().background(Black).windowInsetsPadding(WindowInsets.safeDrawing)) {
            when (screen) {
                Screen.SPLASH -> LensigoSplash()
                Screen.WELCOME -> LensigoWelcome(
                    onBack = { (context as? Activity)?.finish() },
                    onSettings = { moveTo(Screen.SETTINGS) },
                    onContinue = { screen = Screen.PERMISSIONS }
                )
                Screen.PERMISSIONS -> LensigoPermissions(
                    cameraGranted = hasCameraPermission,
                    storageGranted = hasStoragePermission,
                    storageRequired = Build.VERSION.SDK_INT < 29,
                    micGranted = hasMicPermission,
                    onCamera = { requestCameraOnly.launch(Manifest.permission.CAMERA) },
                    onStorage = {
                        if (Build.VERSION.SDK_INT < 29) {
                            requestStorageOnly.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    },
                    onMic = { requestMic.launch(Manifest.permission.RECORD_AUDIO) },
                    onBack = { screen = Screen.WELCOME },
                    onSettings = { moveTo(Screen.SETTINGS) },
                    onContinue = {
                        // Match the onboarding reference action: open the real
                        // camera immediately. HOME remains available from the
                        // LENSIGO camera header and via system Back.
                        prefs.edit().putBoolean("onboarded", true).apply()
                        screen = Screen.CAMERA
                    }
                )
                Screen.CAMERA -> Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().height(55.dp).background(Black)
                            .padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val flashAvailable = activeCamera?.cameraInfo?.hasFlashUnit() == true
                        IconButton(
                            onClick = { flash = !flash },
                            enabled = flashAvailable && !processing
                        ) {
                            Icon(if (flash) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                "Bljeskalica",
                                tint = if (flashAvailable) White else Muted)
                        }
                        Text(when(mode) {
                            CameraMode.VIDEO -> if (preferSD) "SD" else "AUTO"
                            CameraMode.PRO -> if (rawEnabled && rawSupported) "RAW+JPEG" else "JPEG"
                            CameraMode.PHOTO -> if (hdrEnabled && vendorEffect) "HDR" else "AUTO"
                            CameraMode.NIGHT -> "NOĆ"
                            CameraMode.PORTRAIT -> if (vendorEffect) "BOKEH" else "PORTRET"
                            else -> "AUTO"
                        },
                            color = White, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable(
                                enabled = !processing && !isRecording && (
                                    mode == CameraMode.VIDEO ||
                                    (mode == CameraMode.PRO && rawSupported) ||
                                    (mode == CameraMode.PHOTO && hdrSupported) ||
                                    (mode == CameraMode.NIGHT && !vendorEffect &&
                                        exposureMax > exposureMin)),
                                role = Role.Button,
                                onClickLabel = when (mode) {
                                    CameraMode.PHOTO -> if (hdrEnabled) "Isključi HDR" else "Uključi HDR"
                                    CameraMode.PRO -> if (rawEnabled) "Isključi RAW + JPEG"
                                        else "Uključi RAW + JPEG"
                                    CameraMode.NIGHT -> if (nightExposureExpanded)
                                        "Sakrij ekspoziciju" else "Podesi noćnu ekspoziciju"
                                    else -> "Promijeni video kvalitetu"
                                }
                            ) {
                                when {
                                    mode == CameraMode.VIDEO -> {
                                        preferSD = !preferSD
                                        prefs.edit().putBoolean("video_sd", preferSD).apply()
                                    }
                                    mode == CameraMode.PRO && rawSupported -> {
                                        rawEnabled = !rawEnabled
                                        prefs.edit().putBoolean("raw_jpeg", rawEnabled).apply()
                                    }
                                    mode == CameraMode.PHOTO && hdrSupported -> {
                                        hdrEnabled = !hdrEnabled
                                        prefs.edit().putBoolean("hdr_processing", hdrEnabled).apply()
                                    }
                                    mode == CameraMode.NIGHT && !vendorEffect &&
                                        exposureMax > exposureMin -> {
                                        nightExposureExpanded = !nightExposureExpanded
                                    }
                                }
                            })
                        Text(if (aspectWide) "16:9" else "4:3",
                            color = White, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable(
                                enabled = !isRecording && !processing,
                                role = Role.Button,
                                onClickLabel = "Promijeni omjer slike"
                            ) {
                                aspectWide = !aspectWide
                                if (mode != CameraMode.VIDEO) {
                                    photoAspectWide = aspectWide
                                }
                                prefs.edit().putBoolean(
                                    if (mode == CameraMode.VIDEO) "video_aspect_wide"
                                    else "photo_aspect_wide", aspectWide
                                ).apply()
                            })
                        IconButton(
                            onClick = { moveTo(Screen.SETTINGS) },
                            enabled = !isRecording && !processing
                        ) {
                            Icon(Icons.Default.Settings, "Postavke", tint = White)
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
                        if (hasCameraPermission) {
                            AndroidView(factory = { ctx -> PreviewView(ctx).apply {
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                previewView = this
                            } }, modifier = Modifier.fillMaxSize(), update = { previewView = it })
                            Box(Modifier.fillMaxSize().pointerInput(activeCamera) {
                                detectTapGestures { pos ->
                                    focusPoint = pos
                                    val view = previewView ?: return@detectTapGestures
                                    val point = view.meteringPointFactory.createPoint(pos.x, pos.y)
                                    activeCamera?.cameraControl?.startFocusAndMetering(
                                        FocusMeteringAction.Builder(point).build())
                                }
                            })
                            if (gridEnabled) Canvas(Modifier.fillMaxSize()) {
                                val stroke = 1.dp.toPx()
                                for (step in 1..2) {
                                    val x = size.width * step / 3f
                                    val y = size.height * step / 3f
                                    drawLine(White.copy(alpha = .42f),
                                        Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke)
                                    drawLine(White.copy(alpha = .42f),
                                        Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
                                }
                            }
                            if (mode != CameraMode.PANORAMA && mode != CameraMode.DUAL) {
                                FocusCorners(Modifier.fillMaxSize(), focusPoint,
                                    if (mode == CameraMode.PORTRAIT) 210.dp else 78.dp)
                            }
                        } else {
                            Column(Modifier.align(Alignment.Center).padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Dopusti pristup kameri", color = White, fontSize = 18.sp)
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = ::requestPermissions) { Text("Dopusti") }
                            }
                        }
                        Row(Modifier.align(Alignment.TopStart).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("LENSIGO", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { moveTo(Screen.HOME) })
                            Spacer(Modifier.width(12.dp))
                            Text("PRO", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (mode == CameraMode.VIDEO && isRecording) {
                            val seconds = (recordingDurationNanos / 1_000_000_000L).coerceAtLeast(0L)
                            val elapsed = String.format(Locale.ROOT, "%02d:%02d:%02d",
                                seconds / 3600, (seconds / 60) % 60, seconds % 60)
                            Text("●  $elapsed", color = White,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.TopCenter)
                                    .padding(top = 17.dp)
                                    .background(Color(0xFFD72E44), RoundedCornerShape(22.dp))
                                    .padding(horizontal = 17.dp, vertical = 8.dp))
                        }
                        if (countdown > 0) {
                            Text(countdown.toString(), color = White, fontSize = 64.sp,
                                fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                        }
                        if (mode == CameraMode.PANORAMA) {
                            PanoramaGuide(
                                frameCount = panoramaParts.size,
                                busy = processing,
                                onFinish = ::finishPanorama,
                                onCancel = {
                                    panoramaParts.forEach { it.delete() }
                                    panoramaParts.clear()
                                }
                            )
                        }
                        if (mode == CameraMode.DUAL) {
                            DualPreview(
                                step = dualStep, concurrent = concurrentDual,
                                pipLeft = pipOnLeft, onView = { selfieView = it }
                            )
                        }
                        if (mode == CameraMode.FILTERS) {
                            Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp)) {
                                LensigoFilterPreviewStrip(
                                    selected = filter, intensity = intensity,
                                    previewView = previewView,
                                    onSelected = { filter = it }
                                )
                                Slider(value = intensity, onValueChange = { intensity = it },
                                    modifier = Modifier.padding(horizontal = 23.dp).height(32.dp))
                            }
                        }
                        if (mode == CameraMode.PRO) {
                            ProControlOverlay(
                                capabilities = proCapabilities, values = proValues,
                                onValues = { proValues = it },
                                exposure = currentExposure, minExposure = exposureMin,
                                maxExposure = exposureMax,
                                onExposure = { ev ->
                                    activeCamera?.cameraControl?.setExposureCompensationIndex(ev)
                                    currentExposure = ev
                                },
                                modifier = Modifier.align(Alignment.BottomCenter)
                                    .fillMaxWidth().padding(horizontal = 9.dp, vertical = 1.dp)
                                    .padding(bottom = 61.dp)
                            )
                        }
                        if (mode == CameraMode.PORTRAIT) {
                            Row(Modifier.align(Alignment.BottomCenter)
                                .fillMaxWidth().padding(horizontal = 14.dp)
                                .padding(bottom = 67.dp)
                                .background(Navy, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(if (vendorEffect) "BOKEH UREĐAJA" else "ZAMUĆENJE",
                                    color = White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                if (!vendorEffect) {
                                    Slider(value = portraitBlur, onValueChange = { portraitBlur = it },
                                        modifier = Modifier.weight(1f).padding(start = 9.dp))
                                } else {
                                    Text("Aktivno", color = Cyan, fontSize = 12.sp,
                                        modifier = Modifier.padding(14.dp))
                                }
                            }

                        }
                        Row(
                            Modifier.align(Alignment.BottomCenter).padding(bottom = 13.dp)
                                .background(Black.copy(alpha = .83f), RoundedCornerShape(40.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CameraControlRules.zoomPresets(zoomMin, zoomMax).forEach { ratio ->
                                    val selected = kotlin.math.abs(currentZoom - ratio) < .15f
                                    Box(Modifier.size(width = 48.dp, height = 34.dp)
                                        .background(if (selected) Cyan else Color.Transparent, CircleShape)
                                        .clickable(
                                            enabled = activeCamera != null && !processing,
                                            role = Role.Button,
                                            onClickLabel = "Zumiraj na " + CameraControlRules.zoomLabel(ratio)
                                        ) {
                                            val camera = activeCamera
                                            if (camera != null) {
                                                val request = camera.cameraControl.setZoomRatio(ratio)
                                                request.addListener({
                                                    try {
                                                        request.get()
                                                        currentZoom = camera.cameraInfo.zoomState.value
                                                            ?.zoomRatio ?: ratio
                                                    } catch (_: Exception) {
                                                        Toast.makeText(context, "Zumiranje nije dostupno",
                                                            Toast.LENGTH_SHORT).show()
                                                    }
                                                }, ContextCompat.getMainExecutor(context))
                                            }
                                        }, contentAlignment = Alignment.Center) {
                                        Text(CameraControlRules.zoomLabel(ratio),
                                            color = if (selected) Black else White,
                                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }
                        }
                    }
                    if (mode == CameraMode.NIGHT && !vendorEffect && nightExposureExpanded) {
                        val min = exposureMin
                        val max = exposureMax
                        if (max > min) {
                            Row(Modifier.fillMaxWidth().height(45.dp).padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("EV", fontSize = 12.sp, color = Cyan)
                                Slider(value = currentExposure.toFloat(), valueRange = min.toFloat()..max.toFloat(),
                                    steps = (max - min - 1).coerceAtLeast(0),
                                    onValueChange = { v ->
                                        val value = v.toInt().coerceIn(min, max)
                                        activeCamera?.cameraControl?.setExposureCompensationIndex(value)
                                        currentExposure = value
                                    }, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                                Text(currentExposure.toString(), color = White, fontSize = 12.sp)
                            }
                        }
                    }
                    LazyRow(
                        Modifier.fillMaxWidth().height(51.dp).background(Black),
                        state = modeListState,
                        horizontalArrangement = Arrangement.spacedBy(27.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(cameraModeStrip) { item ->
                            Text(item.title, fontSize = 13.sp,
                                fontWeight = if (mode == item) FontWeight.Bold else FontWeight.Normal,
                                color = if (mode == item) Cyan else Muted,
                                modifier = Modifier.clickable(
                                    enabled = !isRecording && !processing,
                                    role = Role.Button,
                                    onClickLabel = "Odaberi način " + item.title
                                ) { setMode(item) })
                        }
                    }
                    Row(Modifier.fillMaxWidth().height(115.dp).background(Black)
                        .padding(bottom = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = ::openFromCamera,
                            enabled = !isRecording && !processing,
                            modifier = Modifier.size(54.dp)) {
                            val thumb = lastPhotoThumbnail
                            if (thumb != null) {
                                Image(
                                    bitmap = thumb, contentDescription = "Galerija",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(45.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, White.copy(alpha = .7f), RoundedCornerShape(10.dp))
                                )
                            } else {
                                Icon(Icons.Default.PhotoLibrary, "Galerija", tint = White,
                                    modifier = Modifier.size(28.dp))
                            }
                        }
                        Box(Modifier.size(78.dp).border(3.dp, White, CircleShape).padding(6.dp)
                            .background(if (mode == CameraMode.VIDEO) Color(0xFFEB3D43) else White, CircleShape)
                            .clickable(
                                enabled = CameraControlRules.shutterEnabled(
                                    hasCameraPermission, processing, countdown,
                                    mode == CameraMode.VIDEO, isRecording,
                                    if (mode == CameraMode.VIDEO)
                                        videoCapture != null else photoCapture != null
                                ),
                                role = Role.Button,
                                onClickLabel = if (mode == CameraMode.VIDEO) {
                                    if (isRecording) "Zaustavi snimanje" else "Pokreni snimanje"
                                } else "Snimi fotografiju"
                            ) {
                                onShutter()
                            }, contentAlignment = Alignment.Center) {
                            if (isRecording) Box(Modifier.size(26.dp)
                                .background(White, RoundedCornerShape(6.dp)))
                            if (processing) CircularProgressIndicator(Modifier.size(26.dp),
                                color = Black, strokeWidth = 3.dp)
                        }
                        IconButton(onClick = {
                            when (mode) {
                                CameraMode.PANORAMA -> {
                                    panoramaParts.forEach { it.delete() }
                                    panoramaParts.clear()
                                }
                                CameraMode.DUAL -> pipOnLeft = !pipOnLeft
                                else -> {
                                    flash = false
                                    front = !front
                                }
                            }
                        }, enabled = !isRecording && !processing,
                            modifier = Modifier.size(54.dp)) {
                            Icon(when (mode) {
                                CameraMode.PANORAMA -> Icons.Default.RestartAlt
                                CameraMode.DUAL -> Icons.Default.FlipCameraAndroid
                                else -> Icons.Default.Cameraswitch
                            }, when (mode) {
                                CameraMode.PANORAMA -> "Ponovi panoramu"
                                CameraMode.DUAL -> "Premjesti prednji pregled"
                                else -> "Promijeni kameru"
                            }, tint = White, modifier = Modifier.size(31.dp))
                        }
                    }
                }
                Screen.HOME -> LensigoHomeScreen(
                    onMode = { selected ->
                        setMode(selected)
                        moveTo(Screen.CAMERA)
                    },
                    onGallery = { moveTo(Screen.GALLERY) },
                    onSettings = { moveTo(Screen.SETTINGS) },
                    onAbout = { moveTo(Screen.ABOUT) }
                )
                Screen.GALLERY -> GalleryScreen(
                    onBack = { moveTo(galleryReturnScreen) },
                    onOpen = { activeMedia = it; moveTo(Screen.VIEWER) },
                    onSettings = { moveTo(Screen.SETTINGS) }
                )
                Screen.VIEWER -> {
                    val item = activeMedia
                    if (item != null) {
                        ViewerScreen(item = item, quality = jpegQuality, lowMemory = lowMemory,
                            onBack = { moveTo(Screen.GALLERY) },
                            onSettings = { moveTo(Screen.SETTINGS) },
                            onSaved = {
                                Toast.makeText(context, "Uređena fotografija spremljena", Toast.LENGTH_SHORT).show()
                                moveTo(Screen.GALLERY)
                            })
                    }
                }
                Screen.SETTINGS -> LensigoSettingsScreen(
                    grid = gridEnabled, timerSeconds = timerSeconds,
                    shutterSound = shutterSound, preferSD = preferSD,
                    jpegQuality = jpegQuality,
                    onGrid = { gridEnabled = it; prefs.edit().putBoolean("grid", it).apply() },
                    onTimer = { timerSeconds = it; prefs.edit().putInt("timer", it).apply() },
                    onSound = {
                        shutterSound = it
                        prefs.edit().putBoolean("shutter_sound", it).apply()
                    },
                    onSD = { preferSD = it; prefs.edit().putBoolean("video_sd", it).apply() },
                    onCameraSettings = { moveTo(Screen.CAMERA_SETTINGS) },
                    onAbout = { moveTo(Screen.ABOUT) },
                    onBack = { moveTo(settingsReturnScreen) }
                )
                Screen.CAMERA_SETTINGS -> LensigoCameraSettingsScreen(
                    jpegQuality = jpegQuality,
                    grid = gridEnabled, photoAspectWide = photoAspectWide,
                    preferSD = preferSD, prefer60Fps = prefer60Fps,
                    fps60Supported = fps60Supported,
                    eisEnabled = eisEnabled, eisSupported = eisSupported,
                    noise = softenNoise, sharpen = sharpen,
                    lowMemory = lowMemory, hasMicrophone = hasMicPermission,
                    recordAudio = recordAudio, nightProcessing = nightProcessing,
                    hdrEnabled = hdrEnabled, hdrSupported = hdrSupported,
                    rawEnabled = rawEnabled, rawSupported = rawSupported,
                    onQuality = { jpegQuality = it },
                    onQualityFinished = {
                        prefs.edit().putInt("quality", jpegQuality).apply()
                    },
                    onPhotoAspect = { newRatio ->
                        photoAspectWide = newRatio
                        prefs.edit().putBoolean("photo_aspect_wide", newRatio).apply()
                        if (mode != CameraMode.VIDEO) aspectWide = newRatio
                    },
                    onGrid = { gridEnabled = it; prefs.edit().putBoolean("grid", it).apply() },
                    onSD = { preferSD = it; prefs.edit().putBoolean("video_sd", it).apply() },
                    on60Fps = {
                        prefer60Fps = it
                        prefs.edit().putBoolean("video_60fps", it).apply()
                    },
                    onEis = {
                        eisEnabled = it
                        prefs.edit().putBoolean("video_stabilization", it).apply()
                    },
                    onNoise = { softenNoise = it; prefs.edit().putBoolean("noise", it).apply() },
                    onSharpen = { sharpen = it; prefs.edit().putBoolean("sharpen", it).apply() },
                    onLowMemory = {
                        lowMemory = it
                        prefs.edit().putBoolean("low_memory", it).apply()
                    },
                    onMicrophone = {
                        if (!hasMicPermission) {
                            requestMic.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            openAppSystemSettings()
                        }
                    },
                    onRecordAudio = {
                        recordAudio = it
                        prefs.edit().putBoolean("record_audio", it).apply()
                    },
                    onNightProcessing = {
                        nightProcessing = it
                        prefs.edit().putBoolean("night_processing", it).apply()
                    },
                    onHdrEnabled = {
                        hdrEnabled = it
                        prefs.edit().putBoolean("hdr_processing", it).apply()
                    },
                    onRawEnabled = {
                        rawEnabled = it
                        prefs.edit().putBoolean("raw_jpeg", it).apply()
                    },
                    onBack = { moveTo(Screen.SETTINGS) }
                )
                Screen.ABOUT -> LensigoAboutScreen(
                    version = BuildConfig.VERSION_NAME,
                    onBack = { moveTo(aboutReturnScreen) },
                    onSettings = { moveTo(Screen.SETTINGS) },
                    onLicences = { moveTo(Screen.LICENCES) },
                    onWebsite = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://brendigo.com")))
                        }.onFailure {
                            Toast.makeText(context, "Preglednik nije dostupan",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                Screen.LICENCES -> LensigoLicencesScreen(
                    onBack = { moveTo(Screen.ABOUT) },
                    onSettings = { moveTo(Screen.SETTINGS) }
                )
            }
        }
    }
}

@Composable
private fun FocusCorners(modifier: Modifier = Modifier, touch: Offset? = null,
                         diameter: androidx.compose.ui.unit.Dp = 78.dp) {
    Canvas(modifier) {
        val frame = diameter.toPx().coerceAtMost(size.minDimension)
        val half = frame / 2f
        val cx = (touch?.x ?: size.width / 2f).coerceIn(half, size.width - half)
        val cy = (touch?.y ?: size.height / 2f).coerceIn(half, size.height - half)
        val l = cx - half; val r = cx + half
        val t = cy - half; val b = cy + half
        val corner = frame * .25f
        val stroke = 2.dp.toPx()
        listOf(
            Offset(l, t + corner) to Offset(l, t),
            Offset(l, t) to Offset(l + corner, t),
            Offset(r - corner, t) to Offset(r, t),
            Offset(r, t) to Offset(r, t + corner),
            Offset(l, b - corner) to Offset(l, b),
            Offset(l, b) to Offset(l + corner, b),
            Offset(r - corner, b) to Offset(r, b),
            Offset(r, b) to Offset(r, b - corner)
        ).forEach { (a, z) -> drawLine(White, a, z, strokeWidth = stroke) }
    }
}
