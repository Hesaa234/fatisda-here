package com.fatisdahere.app.ui.mahasiswa

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.theme.*

/** @param message penjelasan tambahan, misalnya alasan absen ditolak */
@Composable
fun ResultScreen(
    userName: String,
    session: SessionUi,
    selectedType: AttendanceStatus,
    state: ResultState,
    onRetryClick: () -> Unit,
    onDoneClick: () -> Unit,
    message: String? = null
) {
    val berhasil = state == ResultState.Verified
    val warna = if (berhasil) StatusSuccess else StatusDanger

    Scaffold(
        topBar = { FatisdaTopBar(userName) },
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (berhasil) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = warna,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        if (berhasil) "Absensi Berhasil!" else "Absensi Gagal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = warna
                    )
                    Text(session.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = UnsNavy, textAlign = TextAlign.Center)
                    StatusPill(selectedType.label, selectedType.color)
                    InfoRow("Ruang", session.room)
                    InfoRow("Waktu", session.time)
                    if (message != null) {
                        Text(message, fontSize = 14.sp, color = TextPrimary, textAlign = TextAlign.Center)
                    }
                }
            }
            PrimaryButton("Selesai", onDoneClick)
            if (!berhasil) SecondaryButton("Coba Lagi", onRetryClick)
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
