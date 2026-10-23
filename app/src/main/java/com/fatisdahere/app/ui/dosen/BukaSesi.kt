package com.fatisdahere.app.ui.dosen

import android.app.TimePickerDialog
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SectionBg = Color(0xFFEBEBEB)
private val CardBorder = Color(0xFF1C1C1C)
private val Disabled = Color(0xFFD9D9D9)
private val ErrorRed = Color(0xFFD32F2F)

@Composable
fun BukaSesi(
    username: String = "Username",
    daftarMatkul: List<MataKuliah>,
    matkulAwal: MataKuliah? = null,
    onBukaAbsensi: (matkul: MataKuliah, jamMulai: String, jamSelesai: String) -> Unit = { _, _, _ -> }
) {
    var terpilih by remember { mutableStateOf(matkulAwal) }
    var dropdownTerbuka by remember { mutableStateOf(false) }
    var jamMulai by remember { mutableStateOf("13:00") }
    var jamSelesai by remember { mutableStateOf("15:00") }
    val context = LocalContext.current

    fun pilihJam(awal: String, onPilih: (String) -> Unit) {
        val bagian = awal.split(":")
        TimePickerDialog(
            context,
            { _, jam, menit -> onPilih("%02d:%02d".format(jam, menit)) },
            bagian[0].toInt(),
            bagian[1].toInt(),
            true
        ).show()
    }

    val jamValid = jamSelesai > jamMulai // format HH:mm, aman dibandingkan sebagai teks
    val bisaBuka = terpilih != null && jamValid

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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SectionBg)
                    .padding(16.dp)
            ) {
                Text("Buka Sesi Absen", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                // Mata kuliah
                Label("Mata Kuliah")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(2.dp, CardBorder, RoundedCornerShape(16.dp))
                        .clickable { dropdownTerbuka = !dropdownTerbuka }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = terpilih?.let { "(${it.kode}) - ${it.nama}" } ?: "Pilih mata kuliah",
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(if (dropdownTerbuka) "▲" else "▼", fontSize = 14.sp)
                }
                if (dropdownTerbuka) {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(2.dp, CardBorder, RoundedCornerShape(16.dp))
                    ) {
                        daftarMatkul.forEachIndexed { i, m ->
                            if (i > 0) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color(0xFFD9D9D9))
                                )
                            }
                            Text(
                                text = "(${m.kode}) - ${m.nama}",
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        terpilih = m
                                        dropdownTerbuka = false
                                    }
                                    .padding(16.dp)
                            )
                        }
                    }
                }

                // Ruang (otomatis)
                Spacer(Modifier.height(14.dp))
                Label("Ruang (otomatis dari matkul)")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Disabled)
                        .border(2.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(terpilih?.ruang ?: "(Nama Ruang)", fontSize = 16.sp)
                }

                // Jam
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Label("Jam Mulai")
                        KotakJam(jamMulai) { pilihJam(jamMulai) { jamMulai = it } }
                    }
                    Column(Modifier.weight(1f)) {
                        Label("Jam Selesai")
                        KotakJam(jamSelesai) { pilihJam(jamSelesai) { jamSelesai = it } }
                    }
                }

                if (!jamValid) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Jam selesai harus setelah jam mulai.",
                        fontSize = 13.sp,
                        color = ErrorRed
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "Mahasiswa hanya bisa absen di antara jam mulai dan jam selesai. " +
                            "Yang belum absen saat sesi berakhir tercatat Alpa.",
                    fontSize = 13.sp,
                    color = Color(0xFF404040)
                )

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { terpilih?.let { onBukaAbsensi(it, jamMulai, jamSelesai) } },
                    enabled = bisaBuka,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UnsBlue,
                        contentColor = Color.White,
                        disabledContainerColor = Disabled,
                        disabledContentColor = Color.Black
                    )
                ) {
                    Text("Buka Absensi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun Label(teks: String) {
    Text(teks, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun KotakJam(jam: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(jam, fontSize = 16.sp)
        Text("🕒", fontSize = 16.sp)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
fun BukaSesiPreview() {
    val matkul = listOf(
        MataKuliah("IF301", "Pemrograman Mobile", "Lab 3", 25),
        MataKuliah("IF302", "Kecerdasan Buatan", "R. 201", 30)
    )
    BukaSesi(daftarMatkul = matkul, matkulAwal = matkul.first())
}