package com.brendigo.lensigo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Shared design tokens measured against the Brendigo settings references.
private val Panel = Color(0xFF182F43)
private val Cyan = Color(0xFF00D4FF)
private val White = Color(0xFFF7FCFF)
private val Muted = Color(0xFF9CB4C6)
private val Border = Color(0xFF2D536A)

@Composable
private fun SettingsHeader(title: String, onBack: () -> Unit, onMenu: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 65.dp)
        .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Natrag", tint = White) }
        Text(title, color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        IconButton(onClick = onMenu) { Icon(Icons.Default.Settings, "Postavke", tint = White) }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title.uppercase(), color = Cyan, fontSize = 12.sp,
        fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 13.dp, bottom = 9.dp))
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.fillMaxWidth()
        .background(Panel, shape)
        .border(1.dp, Border, shape)
        .padding(horizontal = 9.dp), content = content)
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(thickness = 1.dp, color = Border,
        modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    value: String? = null,
    checked: Boolean? = null,
    onClick: (() -> Unit)? = null,
    onToggle: ((Boolean) -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .then(when {
                checked != null && onToggle != null ->
                    Modifier.toggleable(value = checked, role = Role.Switch,
                        onValueChange = onToggle)
                onClick != null ->
                    Modifier.clickable(role = Role.Button,
                        onClickLabel = label, onClick = onClick)
                else -> Modifier
            })
            .padding(horizontal = 7.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null,
            modifier = Modifier.size(22.dp), tint = if (checked == true) Cyan else Muted)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = White, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
            if (subtitle.isNotBlank()) Text(subtitle,
                color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Spacer(Modifier.width(8.dp))
        if (checked != null) {
            // Informational fixed states are visible but not falsely editable.
            // The parent row is the only action for interactive switches.
            Switch(checked = checked, onCheckedChange = null,
                enabled = onToggle != null,
                modifier = Modifier.heightIn(min = 40.dp),
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Color(0xFF0A99C1),
                    disabledCheckedTrackColor = Color(0xFF0A99C1),
                    disabledCheckedThumbColor = White,
                    disabledUncheckedTrackColor = Color(0xFF395568),
                    disabledUncheckedThumbColor = White))
        } else if (value != null) {
            Text(value, color = Muted, fontSize = 11.sp, textAlign = TextAlign.End,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        } else if (onClick != null) {
            Icon(Icons.Default.ChevronRight, null, tint = Muted,
                modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
internal fun LensigoSettingsScreen(
    grid: Boolean, timerSeconds: Int, shutterSound: Boolean,
    lowMemory: Boolean, noise: Boolean, sharpen: Boolean,
    preferSD: Boolean, jpegQuality: Int,
    hasMicPermission: Boolean,
    onGrid: (Boolean) -> Unit,
    onTimer: (Int) -> Unit,
    onSound: (Boolean) -> Unit,
    onLowMemory: (Boolean) -> Unit,
    onNoise: (Boolean) -> Unit,
    onSharpen: (Boolean) -> Unit,
    onSD: (Boolean) -> Unit,
    onCameraSettings: () -> Unit,
    onMicrophone: () -> Unit,
    onPrivacy: () -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit
) {
    var timerDialog by remember { mutableStateOf(false) }
    if (timerDialog) AlertDialog(
        onDismissRequest = { timerDialog = false },
        title = { Text("Samookidač") },
        text = {
            Column {
                listOf(0, 3, 5, 10).forEach { value ->
                    TextButton(onClick = { onTimer(value); timerDialog = false },
                        modifier = Modifier.fillMaxWidth()) {
                        Text(if (value == 0) "Isključeno" else "$value sekundi")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { timerDialog = false }) { Text("Zatvori") } }
    )
    Column(Modifier.fillMaxSize().background(LensigoReferenceBackground)) {
        SettingsHeader("Postavke", onBack, onCameraSettings)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.widthIn(max = 600.dp)) {
                Spacer(Modifier.height(22.dp))
                Text("Postavke", color = White, fontSize = 29.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("Prilagodi LENSIGO svojem načinu snimanja.",
                    color = Muted, fontSize = 13.sp)
                SettingsSection("Kamera") {
                    SettingsItem(Icons.Default.CameraAlt, "Kvaliteta fotografije",
                        "JPEG fotografije", "$jpegQuality %", onClick = onCameraSettings)
                    SettingsDivider()
                    SettingsItem(Icons.Default.Videocam, "Kvaliteta videozapisa",
                        "Najviša podržana razlučivost (do 4K)",
                        if (preferSD) "SD" else "Najbolja",
                        onClick = { onSD(!preferSD) })
                    SettingsDivider()
                    SettingsItem(Icons.Default.GridOn, "Mreža kadra",
                        "Pomoć pri kompoziciji", checked = grid, onToggle = onGrid)
                    SettingsDivider()
                    SettingsItem(Icons.Default.Timer, "Samookidač", "Odgoda okidača",
                        if (timerSeconds == 0) "Isključeno" else "$timerSeconds s",
                        onClick = { timerDialog = true })
                    SettingsDivider()
                    SettingsItem(Icons.Default.VolumeUp, "Zvuk okidača", "Zvuk pri fotografiranju",
                        checked = shutterSound, onToggle = onSound)
                }
                SettingsSection("Privatnost i pohrana") {
                    SettingsItem(Icons.Default.Folder, "Mjesto spremanja",
                        "Pictures/LENSIGO · Movies/LENSIGO", "Uređaj")
                    SettingsDivider()
                    SettingsItem(Icons.Default.VerifiedUser, "Lokacija u EXIF-u",
                        "Ne prikuplja se niti dodaje fotografijama",
                        checked = false, onClick = onPrivacy)
                    SettingsDivider()
                    SettingsItem(Icons.Default.CloudOff, "Lokalna obrada",
                        "Izvanmrežni rad, bez poslužitelja",
                        checked = true)
                }
                SettingsSection("Aplikacija") {
                    SettingsItem(Icons.Default.Language, "Jezik sučelja",
                        "Trenutačni jezik", "Hrvatski")
                    SettingsDivider()
                    SettingsItem(Icons.Default.NightsStay, "Izgled",
                        "Tamna tema", "Tamna")
                    SettingsDivider()
                    SettingsItem(Icons.Default.Info, "O aplikaciji",
                        "LENSIGO by Brendigo", onClick = onAbout)
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
internal fun LensigoCameraSettingsScreen(
    jpegQuality: Int,
    grid: Boolean,
    photoAspectWide: Boolean,
    preferSD: Boolean,
    prefer60Fps: Boolean,
    fps60Supported: Boolean,
    noise: Boolean,
    sharpen: Boolean,
    lowMemory: Boolean,
    hasMicrophone: Boolean,
    recordAudio: Boolean,
    nightProcessing: Boolean,
    rawEnabled: Boolean,
    rawSupported: Boolean,
    onQuality: (Int) -> Unit,
    onQualityFinished: () -> Unit,
    onPhotoAspect: (Boolean) -> Unit,
    onGrid: (Boolean) -> Unit,
    onSD: (Boolean) -> Unit,
    on60Fps: (Boolean) -> Unit,
    onNoise: (Boolean) -> Unit,
    onSharpen: (Boolean) -> Unit,
    onLowMemory: (Boolean) -> Unit,
    onMicrophone: () -> Unit,
    onRecordAudio: (Boolean) -> Unit,
    onNightProcessing: (Boolean) -> Unit,
    onRawEnabled: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var advanced by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(LensigoReferenceBackground)) {
        // The right-hand gear opens genuinely functional extra controls.
        // The default content follows the original 17_camera-settings.png.
        SettingsHeader("Postavke kamere", onBack, { advanced = !advanced })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.widthIn(max = 600.dp)) {
                Spacer(Modifier.height(20.dp))
                Text(if (advanced) "Napredne postavke" else "Snimanje",
                    color = White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (advanced) "Detaljne postavke lokalne obrade fotografija."
                    else "Napredne postavke fotografija i videa.",
                    color = Muted, fontSize = 13.sp
                )
                if (!advanced) {
                    // 17_camera-settings reference: exactly five photo rows.
                    SettingsSection("Fotografija") {
                        SettingsItem(Icons.Default.CameraAlt, "Razlučivost",
                            "Najviša podržana za odabranu kameru", "Visoka")
                        SettingsDivider()
                        SettingsItem(Icons.Default.CropOriginal, "Format fotografije",
                            if (rawSupported) "RAW + JPEG samo u načinu PRO"
                            else "RAW provjera dostupna nakon otvaranja PRO kamere",
                            if (rawEnabled && rawSupported) "RAW + JPEG" else "JPEG",
                            onClick = if (rawSupported) {
                                { onRawEnabled(!rawEnabled) }
                            } else null)
                        SettingsDivider()
                        SettingsItem(Icons.Default.Tune, "HDR obrada",
                            "Ovisi o obradi uređaja", "Uređaj")
                        SettingsDivider()
                        SettingsItem(Icons.Default.AspectRatio, "Omjer slike",
                            "Standardni ili široki kadar",
                            if (photoAspectWide) "16:9" else "4:3",
                            onClick = { onPhotoAspect(!photoAspectWide) })
                        SettingsDivider()
                        SettingsItem(Icons.Default.NightsStay, "Noćna obrada",
                            "U načinu NOĆ, ako je dostupna",
                            checked = nightProcessing, onToggle = onNightProcessing)
                    }
                    // Reference has four video rows. FPS and stabilization are
                    // informational: CameraX cannot promise 4K/60 or EIS here.
                    SettingsSection("Video") {
                        SettingsItem(Icons.Default.Videocam, "Razlučivost",
                            "SD ili najviša podržana (do 4K)",
                            if (preferSD) "SD" else "Najbolja",
                            onClick = { onSD(!preferSD) })
                        SettingsDivider()
                        SettingsItem(Icons.Default.Timer, "Broj sličica",
                            if (fps60Supported) "60 FPS na kompatibilnom uređaju"
                            else "Za ovu kameru 60 FPS nije potvrđeno",
                            if (prefer60Fps && fps60Supported) "60 FPS (cilj)" else "Auto",
                            onClick = if (fps60Supported) {
                                { on60Fps(!prefer60Fps) }
                            } else null)
                        SettingsDivider()
                        SettingsItem(Icons.Default.Tune, "Stabilizacija",
                            "Ovisi o podršci uređaja", "Uređaj")
                        SettingsDivider()
                        SettingsItem(Icons.Default.Mic, "Snimanje zvuka",
                            if (hasMicrophone) "Mikrofon" else "Potrebna je dozvola za mikrofon",
                            checked = hasMicrophone && recordAudio,
                            onToggle = { enabled ->
                                if (enabled && !hasMicrophone) {
                                    onRecordAudio(true)
                                    onMicrophone()
                                } else onRecordAudio(enabled)
                            })
                    }
                } else {
                    // Gear-only advanced controls preserve the source's
                    // nine-row default layout without removing functionality.
                    SettingsSection("JPEG i lokalna obrada") {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CameraAlt, null, tint = Muted)
                                Spacer(Modifier.width(14.dp))
                                Text("JPEG kvaliteta", color = White,
                                    fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text("$jpegQuality %", color = Cyan, fontSize = 12.sp)
                            }
                            Slider(value = jpegQuality.toFloat(), valueRange = 70f..100f,
                                onValueChange = { onQuality(it.toInt()) },
                                onValueChangeFinished = onQualityFinished)
                        }
                        SettingsDivider()
                        SettingsItem(Icons.Default.GridOn, "Mreža kadra",
                            "Pravilo trećina", checked = grid, onToggle = onGrid)
                        SettingsDivider()
                        SettingsItem(Icons.Default.Memory, "Štedljiva obrada",
                            "Manja potrošnja memorije", checked = lowMemory,
                            onToggle = onLowMemory)
                        SettingsDivider()
                        SettingsItem(Icons.Default.BlurOn, "Smanjenje šuma",
                            "Blago zaglađivanje slike", checked = noise, onToggle = onNoise)
                        SettingsDivider()
                        SettingsItem(Icons.Default.AutoFixHigh, "Izoštravanje",
                            "Naglašavanje rubova", checked = sharpen, onToggle = onSharpen)
                    }
                    TextButton(onClick = { advanced = false },
                        modifier = Modifier.fillMaxWidth()) {
                        Text("Natrag na postavke snimanja", color = Cyan)
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}
