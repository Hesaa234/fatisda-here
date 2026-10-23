package com.fatisdahere.app.ui.dosen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SectionBg = Color(0xFFEBEBEB)
private val LiveRed = Color(0xFFFF3B30)
private val CardBorder = Color(0xFF1C1C1C)
private val LiveHeaderBg = Color(0xFFD9D9D9)

/** jamAbsen == null berarti "Belum Absen" */
data class KehadiranMahasiswa(
    val nama: String,
    val nim: String,
    val jamAbsen: String? = null
)

@Composable
fun PantauSesi(
    username: String = "Username",
    sesi: SesiAktif,
    daftarKehadiran: List<KehadiranMahasiswa>,
    onTutupSesi: () -> Unit = {}
) {
    var tampilDialog by remember { mutableStateOf(false) }
    val hadir = daftarKehadiran.count { it.jamAbsen != null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        HeaderBeranda(username)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ===== Pantau Sesi =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SectionBg)
                    .padding(16.dp)
            ) {
                Text("Pantau Sesi", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, CardBorder, RoundedCornerShape(16.dp))
                        .background(Color.White)
                ) {
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
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(CardBorder)
                    )

                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "(${sesi.kodeMatkul}) - ${sesi.namaMatkul}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(10.dp))
                        InfoBaris("Waktu", sesi.waktu)
                        InfoBaris("Ruang", sesi.ruang)

                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("Hadir: ", fontSize = 15.sp, modifier = Modifier.padding(bottom = 6.dp))
                            Text(
                                text = "$hadir",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = UnsBlue
                            )
                            Text(
                                text = " / ${daftarKehadiran.size}",
                                fontSize = 20.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { tampilDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(2.dp, CardBorder)
                        ) {
                            Text(
                                "Tutup Sesi Lebih Awal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            // ===== Daftar Kehadiran =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SectionBg)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Daftar Kehadiran", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                daftarKehadiran.forEach { KartuMahasiswa(it) }
            }
        }
    }

    if (tampilDialog) {
        AlertDialog(
            onDismissRequest = { tampilDialog = false },
            title = { Text("Tutup sesi lebih awal?") },
            text = { Text("Mahasiswa yang belum absen tidak akan bisa absen lagi setelah sesi ditutup.") },
            confirmButton = {
                TextButton(onClick = {
                    tampilDialog = false
                    onTutupSesi()
                }) { Text("Tutup Sesi", color = LiveRed) }
            },
            dismissButton = {
                TextButton(onClick = { tampilDialog = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun InfoBaris(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, fontSize = 14.sp, modifier = Modifier.width(56.dp))
        Text(": $value", fontSize = 14.sp)
    }
}

@Composable
private fun KartuMahasiswa(m: KehadiranMahasiswa) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(m.nama, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(m.nim, fontSize = 13.sp)
        }
        Spacer(Modifier.width(8.dp))
        if (m.jamAbsen != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text("Hadir", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = UnsBlue)
                Spacer(Modifier.height(4.dp))
                Text(m.jamAbsen, fontSize = 13.sp, textAlign = TextAlign.End)
            }
        } else {
            Text("Belum Absen", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
fun PantauSesiPreview() {
    PantauSesi(
        sesi = SesiAktif("IF301", "Pemrograman Mobile", "08:00 - 10:30", "Lab Komputer 3", 0, 0),
        daftarKehadiran = listOf(
            KehadiranMahasiswa("Andi Pratama", "M0723001", "08:02"),
            KehadiranMahasiswa("Budi Santoso", "M0723002", "08:05"),
            KehadiranMahasiswa("Citra Lestari", "M0723003", null)
        )
    )
}