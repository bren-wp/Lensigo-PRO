package com.brendigo.lensigo

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private val PanelNavy = Color(0xFF081724)
private val PanelCyan = Color(0xFF00D4FF)
private val PanelWhite = Color(0xFFF9FCFF)
private val PanelMuted = Color(0xFF93ADBE)
private enum class Setting { ISO, SHUTTER, EV, WB, FOCUS }

@Composable
fun ProControlOverlay(
    capabilities: ProCapabilities?,
    values: ProValues,
    onValues: (ProValues) -> Unit,
    exposure: Int,
    minExposure: Int,
    maxExposure: Int,
    onExposure: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf(Setting.EV) }
    val caps = capabilities ?: ProCapabilities()
    val availableShutters = ProCameraControl.shutterPresets.filter {
        it.first >= (caps.shutterNanos?.lower ?: 0L) &&
            it.first <= (caps.shutterNanos?.upper ?: Long.MAX_VALUE)
    }
    val wb = ProCameraControl.wbPresets.filter { caps.whiteBalanceModes.contains(it.first) }
    val shutterLabel = ProCameraControl.shutterPresets.minByOrNull {
        abs(it.first - values.shutterNanos)
    }?.second ?: "AUTO"
    val shutterDisplay = if (values.manualExposure && caps.manualSensor) shutterLabel else "AUTO"
    val isoDisplay = if (values.manualExposure && caps.manualSensor)
        values.iso.toString() else "AUTO"
    val focusDisplay = if (values.manualFocus && caps.focusDistance > 0f)
        String.format(java.util.Locale.US, "%.1f", values.focusDiopters) else "AUTO"
    val wbDisplay = wb.firstOrNull { it.first == values.whiteBalanceMode }?.second ?: "AUTO"
    // Every tick in the manual ruler represents the actual selected setting.
    val rulerPosition: Float = when (selected) {
        Setting.ISO -> caps.iso?.let { range ->
            (values.iso - range.lower).toFloat() /
                (range.upper - range.lower).coerceAtLeast(1).toFloat()
        } ?: .5f
        Setting.SHUTTER -> {
            if (availableShutters.size <= 1) .5f
            else {
                val i = availableShutters.indexOfFirst { it.first == values.shutterNanos }
                    .takeIf { it >= 0 } ?: availableShutters.indices.minByOrNull {
                        abs(availableShutters[it].first - values.shutterNanos)
                    } ?: 0
                i.toFloat() / availableShutters.lastIndex.toFloat()
            }
        }
        Setting.EV -> (exposure - minExposure).toFloat() /
            (maxExposure - minExposure).coerceAtLeast(1).toFloat()
        Setting.WB -> if (wb.size <= 1) .5f else
            wb.indexOfFirst { it.first == values.whiteBalanceMode }
                .coerceAtLeast(0).toFloat() / wb.lastIndex.toFloat()
        Setting.FOCUS -> if (caps.focusDistance > 0f)
            values.focusDiopters / caps.focusDistance else .5f
    }.coerceIn(0f, 1f)
    Column(modifier.background(Color.Black.copy(alpha = .86f), RoundedCornerShape(12.dp))
        .padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(
                "S" to shutterDisplay, "EV" to exposure.toString(), "WB" to wbDisplay
            ).forEach { (label, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PanelWhite)
                    Text(value, fontSize = 12.sp, color = PanelWhite)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            .background(PanelNavy, RoundedCornerShape(10.dp)).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(
                Triple(Setting.ISO, "ISO", isoDisplay),
                Triple(Setting.SHUTTER, "S", shutterDisplay),
                Triple(Setting.EV, "EV", exposure.toString()),
                Triple(Setting.WB, "WB", wbDisplay),
                Triple(Setting.FOCUS, "MF", focusDisplay)
            ).forEach { (setting, label, value) ->
                val supported = when(setting) {
                    Setting.ISO -> caps.manualSensor && caps.iso?.let { it.upper > it.lower } == true
                    Setting.SHUTTER -> caps.manualSensor && availableShutters.isNotEmpty()
                    Setting.EV -> maxExposure > minExposure
                    Setting.WB -> wb.size > 1
                    Setting.FOCUS -> caps.focusDistance > 0f
                }
                Column(
                    Modifier.weight(1f).heightIn(min = 48.dp)
                        .clickable(
                            enabled = supported, role = Role.Button,
                            onClickLabel = "Podesi $label"
                        ) {
                            selected = setting
                            if (setting == Setting.EV && values.manualExposure)
                                onValues(values.copy(manualExposure = false))
                        }, horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(label, fontSize = 11.sp, color = if (supported) PanelMuted else PanelMuted.copy(alpha = .4f))
                    Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = if (!supported) PanelMuted.copy(alpha = .45f)
                        else if (setting == selected) PanelCyan else PanelWhite,
                        textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
        when (selected) {
            Setting.ISO -> {
                val range = caps.iso
                if (caps.manualSensor && range != null && range.upper > range.lower) {
                    Slider(
                        value = values.iso.coerceIn(range.lower, range.upper).toFloat(),
                        onValueChange = {
                            onValues(values.copy(manualExposure = true,
                                iso = it.toInt().coerceIn(range.lower, range.upper)))
                        },
                        valueRange = range.lower.toFloat()..range.upper.toFloat(),
                        modifier = Modifier.padding(horizontal = 12.dp).heightIn(min = 48.dp))
                } else NoControl("Ručni ISO nije podržan")
            }
            Setting.SHUTTER -> {
                if (caps.manualSensor && availableShutters.size > 1) {
                    val selectedIndex = availableShutters.indexOfFirst { it.first == values.shutterNanos }
                        .takeIf { it >= 0 } ?: availableShutters.indices.minByOrNull {
                        abs(availableShutters[it].first - values.shutterNanos)
                    } ?: 0
                    Slider(value = selectedIndex.toFloat(),
                        onValueChange = {
                            val i = it.toInt().coerceIn(0, availableShutters.lastIndex)
                            onValues(values.copy(manualExposure = true,
                                shutterNanos = availableShutters[i].first))
                        }, valueRange = 0f..availableShutters.lastIndex.coerceAtLeast(1).toFloat(),
                        steps = (availableShutters.size - 2).coerceAtLeast(0),
                        modifier = Modifier.padding(horizontal = 12.dp).heightIn(min = 48.dp))
                } else if (caps.manualSensor && availableShutters.size == 1) {
                    TextButton(onClick = {
                        onValues(values.copy(
                            manualExposure = true,
                            shutterNanos = availableShutters.first().first
                        ))
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Primijeni " + availableShutters.first().second,
                            color = PanelCyan)
                    }
                } else NoControl("Brzina zatvarača nije podržana")
            }
            Setting.EV -> {
                if (maxExposure > minExposure) {
                    Slider(value = exposure.coerceIn(minExposure,maxExposure).toFloat(),
                        onValueChange = { onExposure(it.toInt().coerceIn(minExposure,maxExposure)) },
                        valueRange = minExposure.toFloat()..maxExposure.toFloat(),
                        steps = (maxExposure - minExposure - 1).coerceAtLeast(0),
                        modifier = Modifier.padding(horizontal = 12.dp).heightIn(min = 48.dp))
                } else NoControl("EV kompenzacija nije podržana")
            }
            Setting.WB -> {
                if (wb.size > 1) {
                    val index = wb.indexOfFirst { it.first == values.whiteBalanceMode }.coerceAtLeast(0)
                    Slider(value = index.toFloat(), onValueChange = {
                        onValues(values.copy(whiteBalanceMode =
                            wb[it.toInt().coerceIn(0,wb.lastIndex)].first))
                    }, valueRange = 0f..wb.lastIndex.toFloat(), steps = (wb.size-2).coerceAtLeast(0),
                        modifier = Modifier.padding(horizontal = 12.dp).heightIn(min = 48.dp))
                } else NoControl("Balans bijele nije podržan")
            }
            Setting.FOCUS -> {
                if (caps.focusDistance > 0f) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onValues(values.copy(manualFocus = !values.manualFocus)) }) {
                            Text(if (values.manualFocus) "MF" else "AF", color = PanelCyan)
                        }
                        Slider(
                            value = values.focusDiopters.coerceIn(0f,caps.focusDistance),
                            onValueChange = {
                                onValues(values.copy(manualFocus = true,
                                    focusDiopters = it))
                            }, valueRange = 0f..caps.focusDistance,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        )
                    }
                } else NoControl("Ručni fokus nije podržan")
            }
        }
        // Reference 07 shows an exposure-style tick ruler below manual controls.
        // Unlike a static mockup, its cyan marker is bound to the active setting.
        Canvas(
            Modifier.fillMaxWidth().height(23.dp)
                .padding(horizontal = 14.dp, vertical = 2.dp)
        ) {
            val total = 40
            val h = size.height
            for (index in 0..total) {
                val fraction = index / total.toFloat()
                val x = fraction * size.width
                val major = index % 5 == 0
                drawLine(
                    color = if (major) PanelWhite.copy(alpha = .9f)
                        else PanelMuted.copy(alpha = .55f),
                    start = Offset(x, if (major) 0f else h * .38f),
                    end = Offset(x, h),
                    strokeWidth = (if (major) 2.dp else 1.dp).toPx()
                )
            }
            drawCircle(
                color = PanelCyan, radius = 4.dp.toPx(),
                center = Offset(rulerPosition * size.width, h * .35f)
            )
        }
    }
}

@Composable
private fun NoControl(message: String) {
    Text(message, color = PanelMuted, fontSize = 11.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(9.dp))
}
