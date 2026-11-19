package com.fatisdahere.app.ui.mahasiswa

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.theme.*

@Composable
fun ResultScreen(
    userName: String,
    session: SessionUi,
    selectedType: AttendanceStatus,
    state: ResultState,
    onRetryClick: () -> Unit,
    onDoneClick: () -> Unit,
    message: String? = null,
    bisaCobaLagi: Boolean = true,
    onBack: (() -> Unit)? = null,
    onProfileClick: () -> Unit = {}
) {
    val berhasil = state == ResultState.Verified
    val warna = if (berhasil) StatusSuccess else StatusDanger
    var tampil by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { tampil = true }
    val skalaIkon by animateFloatAsState(
        if (tampil) 1f else 0.3f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "ikonHasil"
    )

    Scaffold(
        topBar = {
            HeaderGradien(
                judul = "Hasil Absensi",
                subjudul = session.title,
                onBack = onBack,
                userName = userName,
                onUserClick = onProfileClick
            )
        },
        containerColor = PageBackground
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            SectionCard {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LingkaranIkon(96.dp, warna, Modifier.graphicsLayer { scaleX = skalaIkon; scaleY = skalaIkon }) {
                        Icon(
                            if (berhasil) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = warna,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    Text(
                        if (berhasil) "Absensi Berhasil!" else "Absensi Gagal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = warna
                    )
                    Text(session.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = UnsNavy, textAlign = TextAlign.Center)
                    StatusPill(selectedType.label, selectedType.color)
                    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoRow("Ruang", session.room)
                        InfoRow("Waktu", session.time)
                    }
                    if (message != null) {
                        Text(message, fontSize = 14.sp, color = TextPrimary, textAlign = TextAlign.Center)
                    }
                }
            }
            PrimaryButton("Selesai", onDoneClick)
            if (!berhasil && bisaCobaLagi) SecondaryButton("Coba Lagi", onRetryClick)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultScreenPreview() {
    FatisdaTheme {
        ResultScreen("Andi", sampleSession, AttendanceStatus.Hadir, ResultState.Verified, {}, {})
    }
}
