package com.fatisdahere.app.ui.dosen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * @param peserta peserta yang sudah terdaftar di matkul ini
 * @param daftarMahasiswa semua mahasiswa yang bisa ditambahkan (cari berdasarkan NIM / nama)
 */
@Composable
fun KelolaPeserta(
    username: String,
    matkul: MataKuliah,
    peserta: List<Peserta>,
    daftarMahasiswa: List<Peserta>,
    onTambah: (Peserta) -> Unit,
    onHapus: (Peserta) -> Unit,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
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
                SectionCard("Kelola Peserta") {
                    Text(matkul.judul, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = UnsNavy)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppTextField(
                            value = query,
                            onValueChange = {
                                query = it
                                pesan = null
                            },
                            placeholder = "Cari NIM atau nama",
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { tambahPeserta() },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = UnsBlue)
                        ) {
                            Text("Tambah", fontWeight = FontWeight.Bold)
                        }
                    }
                    pesan?.let { (teks, error) ->
                        if (error) ErrorText(teks) else HelperText(teks, color = StatusSuccess)
                    }
                    Text(
                        "Peserta terdaftar: ${peserta.size} mahasiswa",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }
            if (tampil.isEmpty()) item {
                HelperText(if (q.isEmpty()) "Belum ada peserta." else "Tidak ada peserta yang cocok.")
            }
            items(tampil) { p -> KartuPeserta(p) { hapusTarget = p } }
        }
    }

    hapusTarget?.let { p ->
        ConfirmDialog(
            title = "Hapus peserta?",
            text = "${p.nama} (${p.nim}) akan dihapus dari mata kuliah ini.",
            confirmText = "Hapus",
            onConfirm = {
                onHapus(p)
                hapusTarget = null
            },
            onDismiss = { hapusTarget = null }
        )
    }
}

@Composable
private fun KartuPeserta(p: Peserta, onHapus: () -> Unit) {
    ItemCard {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ItemTitle(p.nama)
            ItemSubtitle(p.nim)
        }
        Spacer(Modifier.width(12.dp))
        OutlinedButton(
            onClick = onHapus,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, StatusDanger),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDanger)
        ) {
            Text("Hapus", fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun KelolaPesertaPreview() {
    FatisdaTheme {
        KelolaPeserta(
            username = "Dosen",
            matkul = MataKuliah("WMC-A", "Wireless & Mobile Computing", "LAB 2 UPT", 3),
            peserta = listOf(
                Peserta("Andi Pratama", "M0523001"),
                Peserta("Budi Santoso", "M0523002"),
                Peserta("Citra Lestari", "M0523003")
            ),
            daftarMahasiswa = emptyList(),
            onTambah = {}, onHapus = {}, onBack = {}, onProfileClick = {}
        )
    }
}
