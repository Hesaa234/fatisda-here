package com.fatisdahere.app.ui.dosen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SectionBg = Color(0xFFEBEBEB)
private val CardBorder = Color(0xFF1C1C1C)
private val Placeholder = Color(0xFF808080)
private val ErrorRed = Color(0xFFD32F2F)

data class Peserta(
    val nama: String,
    val nim: String
)

/**
 * @param peserta peserta yang sudah terdaftar di matkul ini
 * @param daftarMahasiswa semua mahasiswa yang bisa ditambahkan (cari berdasarkan NIM / nama)
 */
@Composable
fun KelolaPeserta(
    username: String = "Username",
    matkul: MataKuliah,
    peserta: List<Peserta>,
    daftarMahasiswa: List<Peserta> = emptyList(),
    onTambah: (Peserta) -> Unit = {},
    onHapus: (Peserta) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var pesan by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // teks, isError
    var hapusTarget by remember { mutableStateOf<Peserta?>(null) }

    val q = query.trim()
    val tampil = peserta.filter {
        q.isEmpty() || it.nama.contains(q, ignoreCase = true) || it.nim.contains(q, ignoreCase = true)
    }

    fun tambahPeserta() {
        val cocok = daftarMahasiswa.filter {
            it.nim.equals(q, ignoreCase = true) || it.nama.contains(q, ignoreCase = true)
        }
        val tepat = cocok.firstOrNull { it.nim.equals(q, ignoreCase = true) }
        val target = tepat ?: cocok.singleOrNull()
        pesan = when {
            q.isEmpty() -> "Ketik NIM atau nama mahasiswa dulu." to true
            cocok.isEmpty() -> "Mahasiswa tidak ditemukan." to true
            target == null -> "Ditemukan ${cocok.size} mahasiswa, ketik NIM lengkap." to true
            peserta.any { it.nim == target.nim } -> "${target.nama} sudah terdaftar." to true
            else -> {
                onTambah(target)
                query = ""
                "${target.nama} ditambahkan." to false
            }
        }
    }

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
            // ===== Cari / tambah =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SectionBg)
                    .padding(16.dp)
            ) {
                Text("Kelola Peserta", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Text(
                    "(${matkul.kode}) - ${matkul.nama}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KolomCari(
                        nilai = query,
                        onChange = {
                            query = it
                            pesan = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { tambahPeserta() },
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UnsBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Tambah", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                pesan?.let { (teks, error) ->
                    Spacer(Modifier.height(8.dp))
                    Text(teks, fontSize = 13.sp, color = if (error) ErrorRed else UnsBlue)
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "Peserta terdaftar: ${peserta.size} mahasiswa",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // ===== Daftar peserta =====
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SectionBg)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (tampil.isEmpty()) {
                    Text(
                        if (q.isEmpty()) "Belum ada peserta." else "Tidak ada peserta yang cocok.",
                        fontSize = 14.sp
                    )
                }
                tampil.forEach { p ->
                    KartuPeserta(p) { hapusTarget = p }
                }
            }
        }
    }

    hapusTarget?.let { p ->
        AlertDialog(
            onDismissRequest = { hapusTarget = null },
            title = { Text("Hapus peserta?") },
            text = { Text("${p.nama} (${p.nim}) akan dihapus dari mata kuliah ini.") },
            confirmButton = {
                TextButton(onClick = {
                    onHapus(p)
                    hapusTarget = null
                }) { Text("Hapus", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { hapusTarget = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun KolomCari(nilai: String, onChange: (String) -> Unit, modifier: Modifier) {
    BasicTextField(
        value = nilai,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
        cursorBrush = SolidColor(UnsBlue),
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(2.dp, CardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (nilai.isEmpty()) {
                    Text("Cari NIM atau nama", fontSize = 16.sp, color = Placeholder)
                }
                inner()
            }
        }
    )
}

@Composable
private fun KartuPeserta(p: Peserta, onHapus: () -> Unit) {
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
            Text(p.nama, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(p.nim, fontSize = 13.sp)
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = onHapus,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, CardBorder),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            Text("Hapus", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
fun KelolaPesertaPreview() {
    KelolaPeserta(
        matkul = MataKuliah("IF301", "Pemrograman Mobile", "Lab 3", 3),
        peserta = listOf(
            Peserta("Andi Pratama", "L200220001"),
            Peserta("Budi Santoso", "L200220002"),
            Peserta("Citra Lestari", "L200220003")
        )
    )
}