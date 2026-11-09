package com.brendigo.lensigo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF0D2133)
private val Deep = Color(0xFF07131F)
private val Accent = Color(0xFF00CFF7)
private val Muted = Color(0xFF9CB3C4)
private val White = Color(0xFFF8FBFF)

internal enum class PermissionNextStep { CAMERA, STORAGE, CONTINUE }

internal fun nextPermissionStep(
    cameraGranted: Boolean, storageGranted: Boolean, storageRequired: Boolean
): PermissionNextStep = when {
    !cameraGranted -> PermissionNextStep.CAMERA
    storageRequired && !storageGranted -> PermissionNextStep.STORAGE
    else -> PermissionNextStep.CONTINUE
}

@Composable
fun LensigoSplash() {
    BoxWithConstraints(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF0E2134), Color(0xFF0D2032), Color(0xFF08111E))
            )
        )
    ) {
        SplashWaves()
        val fontScale = LocalDensity.current.fontScale
        val compact = maxHeight < 600.dp || maxWidth < 350.dp || fontScale >= 1.4f
        if (compact) {
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                SplashIdentity(compact = true)
                SplashFooter(compact = true)
            }
        } else {
            // Reference 01: aperture begins 24.5% down the full-height screen.
            SplashIdentity(
                compact = false,
                modifier = Modifier.align(Alignment.TopCenter)
                    .offset(y = maxHeight * .245f)
            )
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = maxHeight * .115f).size(22.dp),
                color = Accent, trackColor = Color(0xFF10597C),
                strokeWidth = 2.5.dp
            )
            Text(
                "BRENDIGO.COM",
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = maxHeight * .054f),
                color = Muted, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = .2.sp
            )
        }
    }
}

@Composable
private fun SplashWaves() {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // Four broad lens arcs, rather than the circles of a generic loader.
        listOf(
            .814f to Color(0xFF12547D),
            .847f to Color(0xFF00D4FF),
            .871f to Color(0xFF14486D),
            .899f to Color(0xFF00D4FF)
        ).forEach { (edge, color) ->
            val curve = Path().apply {
                moveTo(-w * .05f, h * (edge + .008f))
                quadraticBezierTo(w * .5f, h * (edge - .078f),
                    w * 1.05f, h * (edge + .008f))
            }
            drawPath(curve, color = color, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@Composable
private fun SplashIdentity(compact: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LensigoBrandMark(Modifier.size(100.dp))
        Spacer(Modifier.height(if (compact) 8.dp else 10.dp))
        Text("LENSIGO", fontWeight = FontWeight.Bold,
            fontSize = if (compact) 34.sp else 38.sp,
            color = White, letterSpacing = .7.sp)
        Spacer(Modifier.height(5.dp))
        Text("PRO CAMERA", fontWeight = FontWeight.Bold,
            fontSize = 15.sp, color = Accent, letterSpacing = 3.sp)
        Spacer(Modifier.height(5.dp))
        Text("BY BRENDIGO", fontSize = 10.sp, color = Muted,
            fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(if (compact) 26.dp else 42.dp))
        Text("Svaki kadar. Tvoja priča.", color = White, fontSize = 17.sp,
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun SplashFooter(compact: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            Modifier.size(if (compact) 22.dp else 25.dp),
            color = Accent, trackColor = Color(0xFF10597C),
            strokeWidth = 2.dp
        )
        Spacer(Modifier.height(if (compact) 14.dp else 25.dp))
        Text("BRENDIGO.COM", fontSize = 10.sp, color = Muted,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OnboardingHeader(title: String, onBack: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(65.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Natrag", tint = White)
        }
        Text(title, fontSize = 18.sp, color = White,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f))
        IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Settings, contentDescription = "Postavke aplikacije", tint = White)
        }
    }
}

@Composable
private fun OnboardingDots() {
    Row(
        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).background(Accent, CircleShape))
        Spacer(Modifier.width(14.dp))
        repeat(2) { index ->
            Box(Modifier.size(7.dp).border(1.dp, Accent, CircleShape))
            if (index == 0) Spacer(Modifier.width(14.dp))
        }
    }
}

@Composable
fun LensigoWelcome(onBack: () -> Unit, onSettings: () -> Unit, onContinue: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(
        listOf(Night, Color(0xFF0A1B2C), Color(0xFF08111E))
    ))) {
        val compact = maxHeight < 680.dp || maxWidth < 350.dp ||
            LocalDensity.current.fontScale >= 1.35f
        Column(
            Modifier.fillMaxSize()
                .then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(18.dp)
        ) {
            OnboardingHeader("DOBRO DOŠLI", onBack, onSettings)
            Box(
                Modifier.fillMaxWidth()
                    .then(if (compact) Modifier.height(200.dp) else Modifier.weight(1f))
                    .border(1.dp, Color(0xFF2A779B), RoundedCornerShape(24.dp))
                    .background(Color(0xFF0C2A40), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                LensigoBrandMark(Modifier.size(if (compact) 90.dp else 112.dp))
            }
            Spacer(Modifier.height(if (compact) 16.dp else 24.dp))
            Text("LENSIGO  ·  PRO CAMERA", fontSize = 13.sp, color = Accent)
            Spacer(Modifier.height(if (compact) 14.dp else 22.dp))
            Text("Stvori nešto\nizvanredno.", fontSize = 31.sp,
                lineHeight = 36.sp, fontWeight = FontWeight.Bold, color = White)
            Spacer(Modifier.height(if (compact) 12.dp else 20.dp))
            Text("Profesionalna kamera za svaki trenutak.", fontSize = 15.sp, color = Muted)
            Spacer(Modifier.height(if (compact) 20.dp else 32.dp))
            OnboardingDots()
            Spacer(Modifier.height(if (compact) 18.dp else 30.dp))
            Button(onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0984AD))) {
                Text("Započni", fontWeight = FontWeight.Bold, color = White)
            }
        }
    }
}

@Composable
fun LensigoPermissions(
    cameraGranted: Boolean,
    storageGranted: Boolean,
    storageRequired: Boolean,
    micGranted: Boolean,
    onCamera: () -> Unit,
    onStorage: () -> Unit,
    onMic: () -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onContinue: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(
        listOf(Night, Color(0xFF0A1B2C), Color(0xFF08111E))
    ))) {
        val compact = maxHeight < 710.dp || maxWidth < 350.dp ||
            LocalDensity.current.fontScale >= 1.35f
        Column(
            Modifier.fillMaxSize()
                .then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OnboardingHeader("Dopuštenja", onBack, onSettings)
            Spacer(Modifier.height(2.dp))
            Text("Spremno za snimanje?", fontSize = 27.sp,
                fontWeight = FontWeight.Bold, color = White)
            Text("Odaberi potrebna dopuštenja.", fontSize = 14.sp, color = Muted)
            Spacer(Modifier.height(12.dp))
            PermissionRow("Kamera", "Fotografiranje i prikaz uživo", cameraGranted, onCamera,
                icon = { Icon(Icons.Default.CameraAlt, null, tint = Accent) })
            PermissionRow("Mikrofon", "Zvuk za videozapise (neobvezno)",
                micGranted, onMic, icon = { Icon(Icons.Default.Mic, null, tint = Accent) })
            PermissionRow("Medijske datoteke",
                if (storageRequired) "Spremanje vlastitih snimki na Androidu 7–9"
                else "Spremanje vlastitih snimki bez dodatne dozvole",
                storageGranted, onStorage, notRequired = !storageRequired,
                icon = { Icon(Icons.Default.Folder, null, tint = Accent) })
            Row(
                Modifier.fillMaxWidth().heightIn(min = 72.dp)
                    .background(Color(0xFF102E45), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF25516B), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = Accent)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Lokacija (neobvezno)", color = White, fontWeight = FontWeight.Bold)
                    Text("Ne prikuplja se niti dodaje fotografijama.",
                        color = Muted, fontSize = 11.sp)
                }
            }
            Spacer(if (compact) Modifier.height(8.dp) else Modifier.weight(1f))
            val nextStep = nextPermissionStep(
                cameraGranted, storageGranted, storageRequired
            )
            Button(onClick = {
                when (nextStep) {
                    PermissionNextStep.CAMERA -> onCamera()
                    PermissionNextStep.STORAGE -> onStorage()
                    PermissionNextStep.CONTINUE -> onContinue()
                }
            },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0984AD))) {
                Text(when (nextStep) {
                    PermissionNextStep.CAMERA -> "Dopusti pristup kameri"
                    PermissionNextStep.STORAGE -> "Dopusti spremanje fotografija"
                    PermissionNextStep.CONTINUE -> "Nastavi na kameru"
                }, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String, detail: String, granted: Boolean,
    onClick: () -> Unit, notRequired: Boolean = false,
    icon: @Composable () -> Unit
) {
    // The original reference uses a full-width permission card, not a text
    // action inside every row. The entire card is the touch target, with an
    // accessible grant state and genuine Android permission request.
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .background(Color(0xFF102E45), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF25516B), RoundedCornerShape(12.dp))
            .clickable(
                enabled = !granted && !notRequired,
                role = Role.Button,
                onClickLabel = "Dopusti pristup: $title",
                onClick = onClick
            )
            .semantics {
                stateDescription = when {
                    notRequired -> "Dodatno dopuštenje nije potrebno"
                    granted -> "Dopušteno"
                    else -> "Nije dopušteno"
                }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = White, fontWeight = FontWeight.Bold)
            Text(detail, color = Muted, fontSize = 11.sp)
        }
    }
}
