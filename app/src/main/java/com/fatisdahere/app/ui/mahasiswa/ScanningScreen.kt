package com.fatisdahere.app.ui.mahasiswa

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.sensing.hasLocationPermission
import com.fatisdahere.app.sensing.wifiEnvironmentProblem
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.theme.*

/** Tahap proses absen yang sedang berjalan, diatur oleh MahasiswaApp. */
sealed interface ProsesAbsen {
    object Siap : ProsesAbsen
    data class Memindai(val selesai: Int, val total: Int) : ProsesAbsen
    object Memproses : ProsesAbsen
}

/**
 * Halaman absen. Keterangan Hadir diverifikasi lewat scan Wi-Fi + engine KNN (dijalankan MahasiswaApp);
 * Sakit/Izin langsung dikirim tanpa scan.
 *
 * Halaman ini hanya memastikan izin lokasi, Wi-Fi, dan lokasi HP siap sebelum memanggil [onKirim].
 *
 * @param proses tahap yang sedang berjalan, untuk menampilkan progres scan
 * @param onKirim dipanggil saat mahasiswa menekan tombol kirim, membawa keterangan yang dipilih
 */
@Composable
fun ScanningScreen(
    userName: String,
    session: SessionUi,
    wifiName: String,
    proses: ProsesAbsen,
    onKirim: (AttendanceStatus) -> Unit,
    onProfileClick: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedStatus by remember { mutableStateOf<AttendanceStatus?>(null) }
    var galat by remember { mutableStateOf<String?>(null) }
    val sibuk = proses != ProsesAbsen.Siap

    /** Hadir perlu Wi-Fi dan lokasi aktif; kalau belum, tampilkan pesan dan jangan kirim. */
    fun kirimJikaSiap(status: AttendanceStatus) {
        if (status == AttendanceStatus.Hadir) {
            galat = wifiEnvironmentProblem(context)
            if (galat != null) return
        }
        galat = null
        onKirim(status)
    }

    val izinLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { diberi ->
        if (diberi) {
            kirimJikaSiap(AttendanceStatus.Hadir)
        } else {
            galat = "Izin lokasi diperlukan untuk memindai Wi-Fi ruangan. Izin ini hanya dipakai saat absen."
        }
    }

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
                            enabled = !sibuk,
                            onClick = {
                                selectedStatus = status
                                galat = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (selectedStatus == AttendanceStatus.Hadir) {
                    HelperText("Pastikan kamu sudah di dalam ruangan ${session.room}, lalu diam di tempat selama scan.")
                }
                PrimaryButton(
                    if (selectedStatus == AttendanceStatus.Hadir || selectedStatus == null) "Verifikasi Kehadiran" else "Kirim Keterangan",
                    onClick = {
                        val status = selectedStatus ?: return@PrimaryButton
                        if (status == AttendanceStatus.Hadir && !hasLocationPermission(context)) {
                            izinLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        } else {
                            kirimJikaSiap(status)
                        }
                    },
                    enabled = selectedStatus != null && !sibuk
                )
                galat?.let { ErrorText(it) }
            }

            when (proses) {
                is ProsesAbsen.Memindai -> ProgresScan(wifiName, proses)
                ProsesAbsen.Memproses -> ProgresMemproses()
                ProsesAbsen.Siap -> Unit
            }
        }
    }
}

@Composable
private fun ProgresScan(wifiName: String, proses: ProsesAbsen.Memindai) {
    SectionCard {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Scanning Wi-Fi ($wifiName)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = UnsNavy,
                textAlign = TextAlign.Center
            )
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                CircularProgressIndicator(
                    progress = proses.selesai.toFloat() / proses.total,
                    modifier = Modifier.fillMaxSize(),
                    color = UnsBlue,
                    trackColor = PageBackground,
                    strokeWidth = 12.dp
                )
                Text(
                    "${proses.selesai} dari ${proses.total}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = UnsNavy
                )
            }
            Text(
                "Tetap diam di dalam ruangan. Proses ini bisa memakan waktu hingga 30 detik.",
                fontSize = 14.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Tanpa CircularProgressIndicator berputar: crash di proyek ini (lihat catatan di LoadingContent). */
@Composable
private fun ProgresMemproses() {
    SectionCard {
        Text(
            "Memproses absen...",
            Modifier.fillMaxWidth(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = UnsBlue,
            textAlign = TextAlign.Center
        )
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
    FatisdaTheme { ScanningScreen("Andi", sampleSession, "UNS SOLO", ProsesAbsen.Memindai(1, 3), {}) }
}
