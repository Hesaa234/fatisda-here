package com.fatisdahere.app.ui.dosen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Warna (UnsBlue = RGB 0, 114, 188)
val UnsBlue = Color(0xFF0072BC)
private val SectionBg = Color(0xFFEBEBEB)
private val LiveRed = Color(0xFFFF3B30)
private val CardBorder = Color(0xFF1C1C1C)
private val LiveHeaderBg = Color(0xFFD9D9D9)

data class SesiAktif(
    val kodeMatkul: String,
    val namaMatkul: String,
    val waktu: String,
    val ruang: String,
    val hadir: Int,
    val totalMahasiswa: Int
)

data class MataKuliah(
    val kode: String,
    val nama: String,
    val ruang: String,
    val jumlahPeserta: Int
)

@Composable
fun BerandaDosen(
    username: String = "Username",
    sesiAktif: SesiAktif? = null,
    daftarMatkul: List<MataKuliah> = emptyList(),
    onPantauSesi: () -> Unit = {},
    onTambahMatkul: () -> Unit = {},
    onMatkulClick: (MataKuliah) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        HeaderBeranda(username)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp, vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sesiAktif != null) {
                item { SesiKelasAktifSection(sesiAktif, onPantauSesi) }
            }
            item {
                MataKuliahSayaSection(daftarMatkul, onTambahMatkul, onMatkulClick)
            }
        }
    }
}

@Composable
fun HeaderBeranda(username: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(UnsBlue)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "FATISDA Here",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, Color.White, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(text = username, color = Color.White, fontSize = 14.sp)
            }
        }
        Garis()
    }
}

@Composable
private fun SesiKelasAktifSection(sesi: SesiAktif, onPantauSesi: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SectionBg)
            .padding(16.dp)
    ) {
        Text("Sesi Kelas Aktif", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, CardBorder, RoundedCornerShape(16.dp))
                .background(Color.White)
        ) {
            // Header "Sedang Berlangsung"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LiveHeaderBg)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(LiveRed)
                )
                Spacer(Modifier.width(8.dp))
                Text("Sedang Berlangsung", fontSize = 15.sp)
            }
            Garis()

            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "(${sesi.kodeMatkul}) - ${sesi.namaMatkul}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                InfoRow("Waktu", sesi.waktu)
                InfoRow("Ruang", sesi.ruang)
                InfoRow(
                    label = "Hadir",
                    value = "${sesi.hadir} dari ${sesi.totalMahasiswa} mahasiswa",
                    boldValue = true
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onPantauSesi,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UnsBlue,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(2.dp, CardBorder)
                ) {
                    Text("Pantau Sesi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, boldValue: Boolean = false) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, fontSize = 14.sp, modifier = Modifier.width(56.dp))
        Text(": ", fontSize = 14.sp)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (boldValue) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun MataKuliahSayaSection(
    daftar: List<MataKuliah>,
    onTambah: () -> Unit,
    onClick: (MataKuliah) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SectionBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Mata Kuliah Saya", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        OutlinedButton(
            onClick = onTambah,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, CardBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = UnsBlue
            )
        ) {
            Text("+ Tambah Mata Kuliah", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        daftar.forEach { matkul ->
            MatkulCard(matkul) { onClick(matkul) }
        }
    }
}

@Composable
private fun MatkulCard(matkul: MataKuliah, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "(${matkul.kode}) - ${matkul.nama}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text("${matkul.jumlahPeserta} peserta", fontSize = 13.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "(${matkul.ruang})",
            fontSize = 13.sp,
            textAlign = TextAlign.End
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
fun BerandaDosenPreview() {
    BerandaDosen(
        sesiAktif = SesiAktif(
            kodeMatkul = "IF1234",
            namaMatkul = "Pemrograman Mobile",
            waktu = "08:00 - 10:30",
            ruang = "Ruang 3.1",
            hadir = 18,
            totalMahasiswa = 25
        ),
        daftarMatkul = listOf(
            MataKuliah("IF1234", "Pemrograman Mobile", "Ruang 3.1", 25),
            MataKuliah("IF2345", "Basis Data", "Ruang 2.4", 30),
            MataKuliah("IF3456", "Rekayasa Perangkat Lunak", "Lab 1", 28)
        )
    )
}

// Pengganti HorizontalDivider (kompatibel dengan semua versi Material3)
@Composable
private fun Garis() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(CardBorder)
    )
}