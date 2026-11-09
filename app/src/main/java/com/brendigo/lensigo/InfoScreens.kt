package com.brendigo.lensigo

import android.content.Intent
import android.net.Uri
import android.widget.Toast

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Matches the About brand reference; Privacy is not a separate screen.
 * Fixed maximum content width keeps phone and tablet typography readable;
 * vertical scrolling and dynamic row heights avoid clipping with large fonts.
 */
private val Surface = Color(0xFF152E43)
private val Cyan = Color(0xFF00C9F7)
private val White = Color(0xFFF6FBFF)
private val Secondary = Color(0xFF9DB6C9)
private val Line = Color(0xFF28485D)

@Composable
private fun InfoHeader(title: String, onBack: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Natrag", tint = White) }
        Text(title, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
            fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = White)
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, "Postavke", tint = White)
        }
    }
}

@Composable
private fun InfoLine(icon: ImageVector, title: String, caption: String,
                     detail: String? = null, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(
            if (onClick != null) Modifier.clickable(
                role = Role.Button, onClickLabel = "Otvori $title", onClick = onClick
            ) else Modifier
        ).heightIn(min = 50.dp).padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Secondary,
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 14.sp, color = White, fontWeight = FontWeight.SemiBold)
            if (caption.isNotEmpty()) Text(caption, fontSize = 11.sp, color = Secondary,
                lineHeight = 15.sp)
        }
        if (detail != null) {
            Spacer(Modifier.width(8.dp))
            Text(detail, fontSize = 11.sp, color = Secondary, textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun InfoGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()
        .background(Surface, RoundedCornerShape(14.dp))
        .border(1.dp, Line, RoundedCornerShape(14.dp)), content = content)
}

@Composable
private fun ThinDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 13.dp),
        thickness = 1.dp, color = Line)
}

@Composable
fun LensigoAboutScreen(
    version: String,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onLicences: () -> Unit,
    onWebsite: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(LensigoReferenceBackground)) {
        InfoHeader("O aplikaciji", onBack, onSettings)
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.widthIn(max = 540.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(if (
                    LocalConfiguration.current.screenWidthDp >= 600
                ) 60.dp else 38.dp))
                LensigoBrandMark(modifier = Modifier.size(
                    if (LocalConfiguration.current.screenWidthDp >= 600) 92.dp else 72.dp
                ))
                Spacer(Modifier.height(16.dp))
                Text("LENSIGO", color = White, fontSize = 25.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text("P R O   C A M E R A", color = Cyan,
                    fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(7.dp))
                Text("B Y   B R E N D I G O", color = Secondary,
                    fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Spacer(Modifier.height(26.dp))
                Text("Verzija $version", color = Secondary,
                    fontSize = 12.sp)
                Spacer(Modifier.height(44.dp))
                InfoGroup {
                    InfoLine(Icons.Default.Info, "Naziv aplikacije",
                        "", detail = "LENSIGO")
                    ThinDivider()
                    InfoLine(Icons.Default.Public, "Web-stranica",
                        "", detail = "brendigo.com", onClick = onWebsite)
                    ThinDivider()
                    InfoLine(Icons.Default.VerifiedUser, "Verzija aplikacije",
                        "", detail = version)
                    ThinDivider()
                    InfoLine(Icons.Default.Description, "Licence i zahvale",
                        "", detail = "Pregledaj", onClick = onLicences)
                }
                Spacer(Modifier.height(65.dp))
                Text("Svaki kadar. Tvoja priča.", color = White, fontSize = 14.sp)
                Spacer(Modifier.height(18.dp))
                Text("© 2026 Brendigo",
                    color = Secondary, fontSize = 11.sp)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun LensigoLicencesScreen(onBack: () -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(LensigoReferenceBackground)) {
        InfoHeader("Licence i zahvale", onBack, onSettings)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("BRENDIGO · LENSIGO", color = Cyan, fontWeight = FontWeight.Bold)
            Text("© 2026 Brendigo. Izvorni projekt objavljen je pod MIT licencom.",
                color = White, fontSize = 14.sp)
            Text("AndroidX CameraX, Compose, Core, Activity, Lifecycle i druge AndroidX " +
                "biblioteke koriste vlastite otvorene licence, pretežno Apache 2.0. " +
                "Njihove autorske i licencne obveze ostaju na snazi.",
                color = Secondary, fontSize = 13.sp, lineHeight = 19.sp)
            Text("Izvorni kod, korijenska licenca i ovisnosti:",
                color = White, fontWeight = FontWeight.SemiBold)
            Text(
                "github.com/bren-wp/Lensigo-PRO",
                color = Cyan, fontSize = 13.sp,
                modifier = Modifier.clickable {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/bren-wp/Lensigo-PRO")))
                    }.onFailure {
                        Toast.makeText(context, "Preglednik nije dostupan",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            )
            Text("Slike i simbol LENSIGO nastali su za brend Brendigo. " +
                "Licenca aplikacije ne mijenja licence uključenih biblioteka.",
                color = Secondary, fontSize = 13.sp)
        }
    }
}
