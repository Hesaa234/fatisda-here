package com.fatisdahere.app.ui.dosen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
private val Disabled = Color(0xFFD9D9D9)

data class Pertemuan(
    val nomor: Int,
    val hadir: Int,
    val total: Int,
    val tanggal: String,
    val waktu: String
)

@Composable
fun DetailMatkul(
    username: String = "Username",
    matkul: MataKuliah,
    riwayat: List<Pertemuan> = emptyList(),
    sesiBerlangsung: Boolean = false,
    onBukaSesi: () -> Unit = {},
    onKelolaPeserta: () -> Unit = {},
    onEditMatkul: () -> Unit = {},
    onRekap: () -> Unit = {}
) {
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
            // ===== Info matkul + aksi =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SectionBg)
                    .padding(16.dp)
            ) {
                Text(
                    text = "(${matkul.kode}) - ${matkul.nama}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                BarisInfo("Ruang", matkul.ruang)
                BarisInfo("Peserta", "${matkul.jumlahPeserta} mahasiswa")

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onBukaSesi,
                    enabled = !sesiBerlangsung,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UnsBlue,
                        contentColor = Color.White,
                        disabledContainerColor = Disabled,
                        disabledContentColor = Color.Black
                    ),
                    border = BorderStroke(2.dp, CardBorder)
                ) {
                    Text(
                        text = if (sesiBerlangsung) "Sesi Sedang Berlangsung" else "Buka Sesi Absen",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TombolOutline("Kelola Peserta", Modifier.weight(1f), onKelolaPeserta)
                    TombolOutline("Edit Matkul", Modifier.weight(1f), onEditMatkul)
                }
                Spacer(Modifier.height(10.dp))
                TombolOutline("Rekap Kehadiran", Modifier.fillMaxWidth(), onRekap)
            }

            // ===== Riwayat pertemuan =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SectionBg)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Riwayat Pertemuan", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (riwayat.isEmpty()) {
                    Text("Belum ada pertemuan.", fontSize = 14.sp)
                }
                riwayat.forEach { KartuPertemuan(it) }
            }
        }
    }
}

@Composable
private fun BarisInfo(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, fontSize = 14.sp, modifier = Modifier.width(64.dp))
        Text(": $value", fontSize = 14.sp)
    }
}

@Composable
private fun TombolOutline(teks: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(2.dp, CardBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        )
    ) {
        Text(teks, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun KartuPertemuan(p: Pertemuan) {
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
            Text("Pertemuan ${p.nomor}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildAnnotatedString {
                    append("Hadir: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("${p.hadir}") }
                    append(" dari ${p.total}")
                },
                fontSize = 14.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(p.tanggal, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(p.waktu, fontSize = 13.sp)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
fun DetailMatkulPreview() {
    DetailMatkul(
        matkul = MataKuliah("IF301", "Pemrograman Mobile", "Lab 3", 25),
        riwayat = listOf(
            Pertemuan(8, 23, 25, "16 Okt 2026", "13:00 - 15:00"),
            Pertemuan(7, 22, 25, "9 Okt 2026", "13:00 - 15:00"),
            Pertemuan(6, 25, 25, "2 Okt 2026", "13:00 - 15:00")
        )
    )
}