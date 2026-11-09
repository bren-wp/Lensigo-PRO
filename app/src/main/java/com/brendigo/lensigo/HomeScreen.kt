package com.brendigo.lensigo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The 04_home reference has: brand header, large visual story banner,
 * two-column camera modes and one anchored gallery action.
 * All modes are real navigation actions, never dummy/demo surfaces.
 */
private val Midnight = Color(0xFF0E2336)
private val CardColor = Color(0xFF1B394F)
private val Cyan = Color(0xFF00D4FF)
private val White = Color(0xFFF7FCFF)
private val Muted = Color(0xFF9FB8CA)
private val Outline = Color(0xFF2D6884)

private data class ModeTile(
    val mode: CameraMode,
    val label: String,
    val hint: String,
    val icon: ImageVector
)

private val tiles = listOf(
    ModeTile(CameraMode.PHOTO, "Foto", "Fotografiranje", Icons.Outlined.CameraAlt),
    ModeTile(CameraMode.VIDEO, "Video", "Videosnimanje", Icons.Outlined.Videocam),
    ModeTile(CameraMode.PRO, "Pro način", "Ručne kontrole", Icons.Outlined.Tune),
    ModeTile(CameraMode.NIGHT, "Noćni način", "Noćno snimanje", Icons.Outlined.NightsStay),
    ModeTile(CameraMode.PORTRAIT, "Portret", "Portretni način", Icons.Outlined.PersonOutline),
    ModeTile(CameraMode.PANORAMA, "Panorama", "Panoramsko snimanje", Icons.Outlined.Panorama),
    ModeTile(CameraMode.DUAL, "Dual kamera", "Dvije kamere", Icons.Outlined.ViewWeek),
    ModeTile(CameraMode.FILTERS, "Filteri", "Fotografski filteri", Icons.Outlined.AutoAwesome)
)

/**
 * The actual user-supplied sunset photograph from assets/photo.jpg, cropped
 * locally and packaged as a tiny WebP. No remote image loading or network use.
 */
@Composable
private fun BrandBanner(tablet: Boolean) {
    val fontScale = LocalDensity.current.fontScale
    val bannerHeight = HomeLayoutRules.bannerHeightDp(tablet, fontScale).dp
    val corner = RoundedCornerShape(if (tablet) 21.dp else 15.dp)
    Box(
        Modifier.fillMaxWidth().height(bannerHeight)
            .clip(corner)
            .border(1.dp, Outline, corner)
    ) {
        Image(
            painter = painterResource(R.drawable.lensigo_home_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(Color(0xBE061321), Color(0x44061220), Color.Transparent)
                )
            )
        )
        Column(
            Modifier.fillMaxSize().padding(horizontal = 18.dp,
                vertical = if (tablet) 24.dp else 17.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("OTKRIJ NOVU PERSPEKTIVU", color = Cyan,
                fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text("Svaki kadar.\nTvoja priča.", color = White,
                fontSize = 25.sp, lineHeight = 27.sp,
                fontWeight = FontWeight.Bold)
            Text("Profesionalna kamera u tvom džepu.",
                color = White.copy(alpha = .87f), fontSize = 12.sp)
        }
    }
}

@Composable
internal fun LensigoHomeScreen(
    onMode: (CameraMode) -> Unit,
    onGallery: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(
        listOf(Midnight, Color(0xFF0B1C2B), Color(0xFF07111E))
    ))) {
        val tablet = maxWidth >= 600.dp
        val fontScale = LocalDensity.current.fontScale
        val columns = HomeLayoutRules.columns(maxWidth.value, fontScale)
        val tileHeight = HomeLayoutRules.tileHeightDp(tablet, fontScale).dp
        val gridGap = if (tablet) 11.dp else 7.dp
        val horizontalPad = if (tablet) 24.dp else 17.dp
        val maxContentWidth = if (tablet) 950.dp else 540.dp
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier.widthIn(max = maxContentWidth).fillMaxWidth()
                    .heightIn(min = 77.dp).padding(horizontal = horizontalPad),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(48.dp).clickable(
                        onClickLabel = "O aplikaciji LENSIGO",
                        role = Role.Button, onClick = onAbout
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    LensigoBrandMark(Modifier.size(if (tablet) 44.dp else 37.dp))
                }
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text("LENSIGO", color = White,
                        fontSize = if (fontScale >= 1.6f) 22.sp else 27.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = .7.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("PRO CAMERA · BY BRENDIGO", color = Cyan,
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, "Postavke",
                        modifier = Modifier.size(25.dp), tint = White)
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.weight(1f).widthIn(max = maxContentWidth).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gridGap),
                verticalArrangement = Arrangement.spacedBy(if (tablet) 11.dp else 10.dp),
                contentPadding = PaddingValues(
                    start = horizontalPad, end = horizontalPad, top = 20.dp, bottom = 12.dp
                )
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    BrandBanner(tablet)
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Načini snimanja", color = White,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        modifier = Modifier.padding(top = 13.dp, bottom = 4.dp))
                }
                items(tiles, key = { it.mode.name }) { tile ->
                    Column(
                        Modifier.fillMaxWidth().heightIn(min = tileHeight)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF1B394F), Color(0xFF143048))),
                                RoundedCornerShape(if (tablet) 17.dp else 12.dp)
                            )
                            .border(1.dp, Outline, RoundedCornerShape(if (tablet) 17.dp else 12.dp))
                            .clickable(onClickLabel = tile.hint, role = Role.Button) {
                                onMode(tile.mode)
                            }.padding(if (tablet) 17.dp else 12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(tile.icon, contentDescription = null, tint = Cyan,
                            modifier = Modifier.size(if (tablet) 28.dp else 22.dp))
                        Spacer(Modifier.height(if (tablet) 18.dp else 11.dp))
                        Text(tile.label, color = White, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Box(Modifier.widthIn(max = maxContentWidth).fillMaxWidth()
                .padding(horizontal = horizontalPad, vertical = 15.dp)
                .heightIn(min = if (tablet) 57.dp else 40.dp)
                .clip(RoundedCornerShape(15.dp))
                .border(1.dp, Cyan.copy(alpha = .8f), RoundedCornerShape(15.dp))
                .background(Color(0xFF0B4868))
                .clickable(onClickLabel = "Otvori galeriju snimki", role = Role.Button,
                    onClick = onGallery), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhotoLibrary, null, tint = White,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("Otvori galeriju", color = White,
                        fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
