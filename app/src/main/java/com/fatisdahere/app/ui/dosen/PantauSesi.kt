package com.fatisdahere.app.ui.dosen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.theme.*

@Composable
fun PantauSesi(
    username: String,
    sesi: SesiAktif,
    daftarKehadiran: List<KehadiranMahasiswa>,
    onTutupSesi: () -> Unit,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tampilDialog by remember { mutableStateOf(false) }
    val hadir = daftarKehadiran.count { it.status == AttendanceStatus.Hadir }

    Scaffold(
        modifier,
        topBar = { FatisdaTopBar(username, onProfileClick, onBack) },
        containerColor = PageBackground
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionCard("Pantau Sesi") {
                    LiveBadge()
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(sesi.judul, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = UnsNavy)
                        Spacer(Modifier.height(4.dp))
                        InfoRow("Waktu", sesi.waktu)
                        InfoRow("Ruang", sesi.ruang)
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Hadir ", fontSize = 15.sp, color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
                        Text("$hadir", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = UnsBlue)
                        Text(
                            " / ${daftarKehadiran.size}",
                            fontSize = 20.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    SecondaryButton("Tutup Sesi Lebih Awal", { tampilDialog = true }, color = StatusDanger)
                }
            }
            item { SectionTitle("Daftar Kehadiran") }
            if (daftarKehadiran.isEmpty()) item { HelperText("Belum ada peserta.") }
            items(daftarKehadiran) { KartuMahasiswa(it) }
        }
    }

    if (tampilDialog) {
        ConfirmDialog(
            title = "Tutup sesi lebih awal?",
            text = "Mahasiswa yang belum absen tidak akan bisa absen lagi setelah sesi ditutup.",
            confirmText = "Tutup Sesi",
            onConfirm = {
                tampilDialog = false
                onTutupSesi()
            },
            onDismiss = { tampilDialog = false }
        )
    }
}

@Composable
private fun KartuMahasiswa(m: KehadiranMahasiswa) {
    ItemCard {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ItemTitle(m.nama)
            ItemSubtitle(m.nim)
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StatusPill(m.status.label, m.status.color)
            if (m.jam != null) ItemSubtitle(m.jam)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun PantauSesiPreview() {
    FatisdaTheme {
        PantauSesi(
            username = "Dosen",
            sesi = SesiAktif("WMC-A", "Wireless & Mobile Computing", "13:00 - 15:00", "LAB 2 UPT", 2, 3),
            daftarKehadiran = listOf(
                KehadiranMahasiswa("Andi Pratama", "M0523001", AttendanceStatus.Hadir, "13:02"),
                KehadiranMahasiswa("Budi Santoso", "M0523002", AttendanceStatus.Izin, "13:05"),
                KehadiranMahasiswa("Citra Lestari", "M0523003")
            ),
            onTutupSesi = {}, onBack = {}, onProfileClick = {}
        )
    }
}
