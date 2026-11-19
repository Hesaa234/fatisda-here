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
        topBar = {
            HeaderGradien(
                judul = "Halo, ${username.trim().substringBefore(' ').ifBlank { username }}",
                subjudul = "Dosen · FATISDA Here",
                userName = username,
                onUserClick = onProfileClick
            ) {
                StatistikHeader(
                    listOf(
                        "${daftarMatkul.size}" to "Mata Kuliah",
                        "${daftarMatkul.sumOf { it.jumlahPeserta }}" to "Peserta",
                        (if (sesiAktif != null) "1" else "0") to "Sesi Aktif"
                    )
                )
            }
        },
        containerColor = PageBackground
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sesiAktif != null) item {
                KartuSesiAktif(
                    judul = sesiAktif.judul,
                    keterangan = "Sesi sedang berlangsung",
                    tombol = "Pantau Sesi",
                    onClick = onPantauSesi,
                    baris = listOf(
                        "${sesiAktif.waktu} · ${sesiAktif.ruang}",
                        "Hadir ${sesiAktif.hadir} dari ${sesiAktif.totalMahasiswa} mahasiswa"
                    )
                )
            }
            item { SectionTitle("Mata Kuliah Saya") }
            item { SecondaryButton("+ Tambah Mata Kuliah", onTambahMatkul) }
            if (daftarMatkul.isEmpty()) item { KartuKosong("Belum ada mata kuliah.") }
            items(daftarMatkul) { matkul -> MatkulCard(matkul) { onMatkulClick(matkul) } }
        }
    }
}

@Composable
private fun MatkulCard(matkul: MataKuliah, onClick: () -> Unit) {
    ItemCard(onClick = onClick, inisial = matkul.kode) {
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
