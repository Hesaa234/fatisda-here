package com.fatisdahere.app.ui.dosen

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SectionBg = Color(0xFFEBEBEB)
private val CardBorder = Color(0xFF1C1C1C)

enum class StatusHadir(val label: String) {
    Hadir("Hadir"),
    Izin("Izin"),
    Sakit("Sakit"),
    Alpa("Alpa")
}

/** manual = true jika statusnya diubah dosen; false = hasil verifikasi otomatis */
data class CatatanKehadiran(
    val nama: String,
    val nim: String,
    val status: StatusHadir,
    val manual: Boolean = false
)

@Composable
fun RekapKehadiran(
    username: String = "Username",
    matkul: MataKuliah,
    riwayat: List<Pertemuan>,
    catatanUntuk: (Pertemuan) -> List<CatatanKehadiran>,
    onUbahStatus: (pertemuan: Pertemuan, nim: String, status: StatusHadir) -> Unit = { _, _, _ -> }
) {
    var dipilih by remember { mutableStateOf(riwayat.firstOrNull()) }
    var dropdownTerbuka by remember { mutableStateOf(false) }

    val pertemuan = dipilih
    val catatan = if (pertemuan != null) catatanUntuk(pertemuan) else emptyList()

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
            // ===== Pilih pertemuan + ringkasan =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SectionBg)
                    .padding(16.dp)
            ) {
                Text("Rekap Kehadiran", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Text(
                    "(${matkul.kode}) - ${matkul.nama}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Text("Pertemuan", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(2.dp, CardBorder, RoundedCornerShape(16.dp))
                        .clickable(enabled = riwayat.isNotEmpty()) {
                            dropdownTerbuka = !dropdownTerbuka
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = pertemuan?.let { "Pertemuan ${it.nomor} · ${it.tanggal}" }
                            ?: "Belum ada pertemuan",
                        fontSize = 16.sp
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
                        riwayat.forEachIndexed { i, p ->
                            if (i > 0) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color(0xFFD9D9D9))
                                )
                            }
                            Text(
                                text = "Pertemuan ${p.nomor} · ${p.tanggal}",
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        dipilih = p
                                        dropdownTerbuka = false
                                    }
                                    .padding(16.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusHadir.values().forEach { s ->
                        val jumlah = catatan.count { it.status == s }
                        Text(
                            text = buildAnnotatedString {
                                append("${s.label} ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$jumlah") }
                            },
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // ===== Daftar mahasiswa =====
            if (pertemuan != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SectionBg)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (catatan.isEmpty()) {
                        Text("Belum ada peserta.", fontSize = 14.sp)
                    }
                    catatan.forEach { c ->
                        KartuCatatan(c) { statusBaru -> onUbahStatus(pertemuan, c.nim, statusBaru) }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Status hasil verifikasi otomatis tertulis \"auto\"; " +
                                "perubahan oleh dosen tercatat sebagai \"manual\".",
                        fontSize = 13.sp,
                        color = Color(0xFF404040)
                    )
                }
            }
        }
    }
}

@Composable
private fun KartuCatatan(c: CatatanKehadiran, onUbah: (StatusHadir) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(c.nama, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "${c.nim} · ${if (c.manual) "manual" else "auto"}",
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        PilihStatus(c.status, onUbah)
    }
}

@Composable
private fun PilihStatus(status: StatusHadir, onPilih: (StatusHadir) -> Unit) {
    var terbuka by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .width(110.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(2.dp, CardBorder, RoundedCornerShape(14.dp))
                .clickable { terbuka = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(status.label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("▼", fontSize = 12.sp)
        }
        DropdownMenu(expanded = terbuka, onDismissRequest = { terbuka = false }) {
            StatusHadir.values().forEach { s ->
                DropdownMenuItem(
                    text = { Text(s.label) },
                    onClick = {
                        onPilih(s)
                        terbuka = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
fun RekapKehadiranPreview() {
    val p = Pertemuan(8, 2, 4, "16 Okt 2026", "13:00 - 15:00")
    RekapKehadiran(
        matkul = MataKuliah("IF301", "Pemrograman Mobile", "Lab 3", 4),
        riwayat = listOf(p),
        catatanUntuk = {
            listOf(
                CatatanKehadiran("Andi Pratama", "L200220001", StatusHadir.Hadir),
                CatatanKehadiran("Budi Santoso", "L200220002", StatusHadir.Hadir),
                CatatanKehadiran("Citra Lestari", "L200220003", StatusHadir.Izin, manual = true),
                CatatanKehadiran("Dewi Anggraini", "L200220004", StatusHadir.Alpa)
            )
        }
    )
}