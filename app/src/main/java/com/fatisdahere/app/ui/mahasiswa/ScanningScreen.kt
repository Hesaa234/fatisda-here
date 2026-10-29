package com.fatisdahere.app.ui.mahasiswa

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.theme.*

/**
 * Halaman absen. Proses scan Wi-Fi di sini MASIH SIMULASI (tombol "Klik untuk Simulasi Selesai").
 * Nanti diganti dengan WifiScanner + PositioningEngine (lihat ui/debug/DebugPositioningScreen.kt).
 *
 * @param onSelesai dipanggil setelah scan selesai, membawa status yang dipilih mahasiswa
 */
@Composable
fun ScanningScreen(
    userName: String,
    session: SessionUi,
    wifiName: String,
    onSelesai: (AttendanceStatus) -> Unit,
    onProfileClick: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var selectedStatus by remember { mutableStateOf<AttendanceStatus?>(null) }
    var isVerified by remember { mutableStateOf(false) }
    var isScanFinished by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { FatisdaTopBar(userName, onProfileClick, onBack) },
        containerColor = PageBackground
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("Absensi") {
                LiveBadge()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(session.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = UnsNavy)
                    Spacer(Modifier.height(4.dp))
                    InfoRow("Waktu", session.time)
                    InfoRow("Ruang", session.room)
                }
                Text(
                    "Keterangan: ${selectedStatus?.label ?: "Belum dipilih"}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(AttendanceStatus.Hadir, AttendanceStatus.Sakit, AttendanceStatus.Izin).forEach { status ->
                        PilihanKeterangan(
                            label = status.label,
                            selected = selectedStatus == status,
                            enabled = !isVerified,
                            onClick = { selectedStatus = status },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                PrimaryButton(
                    "Verifikasi Kehadiran",
                    onClick = { if (selectedStatus != null) isVerified = true },
                    enabled = selectedStatus != null && !isVerified
                )
            }

            if (isVerified) {
                SectionCard {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            if (isScanFinished) "Scanning Wi-Fi selesai" else "Scanning Wi-Fi ($wifiName)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = UnsNavy,
                            textAlign = TextAlign.Center
                        )
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                            CircularProgressIndicator(
                                progress = if (isScanFinished) 1.0f else 0.33f,
                                modifier = Modifier.fillMaxSize(),
                                color = UnsBlue,
                                trackColor = PageBackground,
                                strokeWidth = 12.dp
                            )
                            Text(
                                if (isScanFinished) "3 dari 3" else "1 dari 3",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = UnsNavy
                            )
                        }
                        if (isScanFinished) {
                            Text(
                                "Pemindaian selesai. Tekan tombol di bawah untuk mengirim absen.",
                                fontSize = 14.sp,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            PrimaryButton("Kirim Absen", { selectedStatus?.let(onSelesai) })
                        } else {
                            Text("Sedang memproses...", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = UnsBlue)
                            TextButton(onClick = { isScanFinished = true }) {
                                Text("(Klik untuk Simulasi Selesai)", fontSize = 12.sp, color = TextPrimary.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PilihanKeterangan(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) UnsBlue else SurfaceWhite,
        border = BorderStroke(1.dp, if (selected) UnsBlue else TextPrimary.copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) Color.White else TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanningScreenPreview() {
    FatisdaTheme { ScanningScreen("Andi", sampleSession, "UNS SOLO", {}) }
}
