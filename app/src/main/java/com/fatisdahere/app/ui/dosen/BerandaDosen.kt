package com.fatisdahere.app.ui.dosen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun BerandaDosen(
    username: String,
    sesiAktif: SesiAktif?,
    daftarMatkul: List<MataKuliah>,
    onPantauSesi: () -> Unit,
    onTambahMatkul: () -> Unit,
    onMatkulClick: (MataKuliah) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier,
        topBar = { FatisdaTopBar(username, onProfileClick) },
        containerColor = PageBackground
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (sesiAktif != null) item {
                SectionCard("Sesi Kelas Aktif") {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PageBackground)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LiveBadge()
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(sesiAktif.judul, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = UnsNavy)
                                Spacer(Modifier.height(4.dp))
                                InfoRow("Waktu", sesiAktif.waktu)
                                InfoRow("Ruang", sesiAktif.ruang)
                                InfoRow("Hadir", "${sesiAktif.hadir} dari ${sesiAktif.totalMahasiswa} mahasiswa")
                            }
                            PrimaryButton("Pantau Sesi", onPantauSesi)
                        }
                    }
                }
            }
            item { SectionTitle("Mata Kuliah Saya") }
            item { SecondaryButton("+ Tambah Mata Kuliah", onTambahMatkul) }
            if (daftarMatkul.isEmpty()) item { HelperText("Belum ada mata kuliah.") }
            items(daftarMatkul) { matkul -> MatkulCard(matkul) { onMatkulClick(matkul) } }
        }
    }
}

@Composable
private fun MatkulCard(matkul: MataKuliah, onClick: () -> Unit) {
    ItemCard(onClick = onClick) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ItemTitle(matkul.judul)
            ItemSubtitle("${matkul.jumlahPeserta} peserta")
        }
        Spacer(Modifier.width(12.dp))
        Text(
            matkul.ruang,
            fontSize = 13.sp,
            color = UnsBlue,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun BerandaDosenPreview() {
    FatisdaTheme {
        BerandaDosen(
            username = "Dosen",
            sesiAktif = SesiAktif("WMC-A", "Wireless & Mobile Computing", "13:00 - 15:00", "LAB 2 UPT", 18, 25),
            daftarMatkul = listOf(
                MataKuliah("WMC-A", "Wireless & Mobile Computing", "LAB 2 UPT", 25),
                MataKuliah("JARKOM-A", "Jaringan Komputer", "B4.05", 30)
            ),
            onPantauSesi = {},
            onTambahMatkul = {},
            onMatkulClick = {},
            onProfileClick = {}
        )
    }
}
