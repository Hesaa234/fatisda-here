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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SectionBg = Color(0xFFEBEBEB)
private val CardBorder = Color(0xFF1C1C1C)
private val Placeholder = Color(0xFF808080)
private val Disabled = Color(0xFFD9D9D9)

@Composable
fun TambahMatkul(
    username: String = "Username",
    judul: String = "Tambah Mata Kuliah",
    // Isi awal form (dipakai saat Edit)
    kodeAwal: String = "",
    namaAwal: String = "",
    ruanganAwal: String? = null,
    // Kode yang sudah dipakai mata kuliah lain (tidak boleh kembar)
    kodeTerpakai: List<String> = emptyList(),
    // Hanya ruangan yang sudah punya data fingerprint
    daftarRuangan: List<String> = emptyList(),
    onSimpan: (kode: String, nama: String, ruangan: String) -> Unit = { _, _, _ -> }
) {
    var kode by remember { mutableStateOf(kodeAwal) }
    var nama by remember { mutableStateOf(namaAwal) }
    var ruangan by remember { mutableStateOf<String?>(ruanganAwal) }
    var dropdownTerbuka by remember { mutableStateOf(false) }

    val kodeDipakai = kodeTerpakai.any { it.equals(kode.trim(), ignoreCase = true) }
    val bisaSimpan = kode.isNotBlank() && nama.isNotBlank() && ruangan != null && !kodeDipakai

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
                Text(judul, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                Label("Kode Mata Kuliah")
                KolomIsian(kode, { kode = it }, "contoh: WMC-A")
                if (kodeDipakai) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Kode mata kuliah sudah dipakai.",
                        fontSize = 13.sp,
                        color = Color(0xFFD32F2F)
                    )
                }

                Spacer(Modifier.height(14.dp))
                Label("Nama Mata Kuliah")
                KolomIsian(nama, { nama = it }, "contoh: Wireless & Mobile Computing")

                Spacer(Modifier.height(14.dp))
                Label("Ruangan")
                PilihRuangan(
                    terpilih = ruangan,
                    terbuka = dropdownTerbuka,
                    daftar = daftarRuangan,
                    onToggle = { dropdownTerbuka = !dropdownTerbuka },
                    onPilih = {
                        ruangan = it
                        dropdownTerbuka = false
                    }
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    "Hanya ruangan yang sudah memiliki data fingerprint yang bisa dipilih.",
                    fontSize = 13.sp,
                    color = Color(0xFF404040)
                )

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onSimpan(kode.trim(), nama.trim(), ruangan!!) },
                    enabled = bisaSimpan,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UnsBlue,
                        contentColor = Color.White,
                        disabledContainerColor = Disabled,
                        disabledContentColor = Color.Black
                    )
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
private fun KolomIsian(
    nilai: String,
    onChange: (String) -> Unit,
    placeholder: String
) {
    BasicTextField(
        value = nilai,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
        cursorBrush = SolidColor(UnsBlue),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(2.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                if (nilai.isEmpty()) {
                    Text(placeholder, fontSize = 16.sp, color = Placeholder)
                }
                inner()
            }
        }
    )
}

@Composable
private fun PilihRuangan(
    terpilih: String?,
    terbuka: Boolean,
    daftar: List<String>,
    onToggle: () -> Unit,
    onPilih: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(2.dp, CardBorder, RoundedCornerShape(16.dp))
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = terpilih ?: "Pilih ruangan",
                fontSize = 16.sp,
                color = if (terpilih == null) Color.Black else UnsBlue,
                fontWeight = if (terpilih == null) FontWeight.Normal else FontWeight.Bold
            )
            Text(if (terpilih == null && !terbuka) "▼" else if (terbuka) "▲" else "▼", fontSize = 14.sp)
        }

        if (terbuka) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(2.dp, CardBorder, RoundedCornerShape(16.dp))
            ) {
                if (daftar.isEmpty()) {
                    Text(
                        "Belum ada ruangan dengan data fingerprint",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp,
                        color = Placeholder
                    )
                }
                daftar.forEachIndexed { i, ruang ->
                    if (i > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFD9D9D9))
                        )
                    }
                    Text(
                        text = ruang,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPilih(ruang) }
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
fun TambahMatkulPreview() {
    TambahMatkul(daftarRuangan = listOf("LAB 2 UPT", "LAB UPT LT 3", "B4.05"))
}