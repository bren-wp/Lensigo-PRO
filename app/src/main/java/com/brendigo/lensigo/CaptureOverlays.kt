package com.brendigo.lensigo

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val Ice = Color(0xFFF8FCFF)
private val Cyan = Color(0xFF00CFF7)
private val Ink = Color(0xFF0D2133)

@Composable
fun BoxScope.PanoramaGuide(
    frameCount: Int, busy: Boolean,
    onFinish: () -> Unit, onCancel: () -> Unit
) {
    Box(
        Modifier.align(Alignment.Center).fillMaxWidth(.84f)
            .heightIn(min = 64.dp)
            .border(1.dp, Ice.copy(alpha = .94f), RoundedCornerShape(5.dp))
            .background(Color.Black.copy(alpha = .14f), RoundedCornerShape(5.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.ArrowForward, contentDescription = null,
                tint = Ice, modifier = Modifier.size(15.dp))
            Text("Pomaknite uređaj udesno", color = Ice, fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Icon(Icons.Default.ArrowForward, contentDescription = null,
                tint = Ice, modifier = Modifier.size(15.dp))
        }
    }
    Row(
        Modifier.align(Alignment.Center).offset(y = 79.dp)
            .fillMaxWidth().padding(start = 22.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LinearProgressIndicator(
            progress = { frameCount.coerceIn(0, PanoramaComposer.MAX_FRAMES) /
                PanoramaComposer.MAX_FRAMES.toFloat() },
            modifier = Modifier.weight(1f).height(5.dp),
            color = Ice, trackColor = Ice.copy(alpha = .42f)
        )
        Icon(Icons.Default.ArrowForward, contentDescription = "Smjer panorame: udesno",
            tint = Ice, modifier = Modifier.size(42.dp))
    }
    if (frameCount > 0) {
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 66.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedButton(onClick = onCancel, enabled = !busy) {
                Text("ODUSTANI", color = Ice, fontSize = 11.sp)
            }
            Button(onClick = onFinish,
                enabled = frameCount >= PanoramaComposer.MIN_FRAMES && !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Cyan)) {
                Text("ZAVRŠI", color = Ink, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun BoxScope.DualPreview(
    step: Int, concurrent: Boolean, pipLeft: Boolean, onView: (PreviewView) -> Unit
) {
    Box(
        Modifier.align(if (pipLeft) Alignment.TopStart else Alignment.TopEnd)
            .padding(top = 62.dp, start = 17.dp, end = 17.dp)
            .fillMaxWidth(.32f).aspectRatio(.77f)
            .border(2.dp, Ice, RoundedCornerShape(13.dp))
            .background(Ink, RoundedCornerShape(13.dp))
    ) {
        if (step == 0) {
            AndroidView(
                factory = { ctx -> PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    onView(this)
                } },
                modifier = Modifier.fillMaxSize(),
                update = { onView(it) }
            )
        }
        if (!concurrent) {
            Box(Modifier.fillMaxSize()
                .background(Ink, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center) {
                Text(if (step == 0) "Prednja kamera\nnakon 1. snimke"
                    else "Snimi prednjom\nkamerom",
                    color = Ice, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
    }
    // A real concurrent preview speaks for itself. On devices without support,
    // keep the fallback's step number so users know which frame to capture.
    if (!concurrent) {
        Text(
            if (step == 0) "1/2 · Stražnja kamera" else "2/2 · Prednja kamera",
            color = Ice, fontSize = 12.sp,
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 76.dp)
                .background(Color.Black.copy(alpha = .77f), RoundedCornerShape(8.dp))
                .padding(7.dp)
        )
    }
}
