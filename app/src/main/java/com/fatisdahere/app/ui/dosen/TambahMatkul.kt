package com.fatisdahere.app.ui.dosen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.theme.*

/** Dipakai untuk Tambah Mata Kuliah dan Edit Mata Kuliah. */
@Composable
fun TambahMatkul(
    username: String,
    daftarRuangan: List<String>,
    kodeTerpakai: List<String>,
    onSimpan: (kode: String, nama: String, ruangan: String) -> Unit,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    judul: String = "Tambah Mata Kuliah",
    kodeAwal: String = "",
    namaAwal: String = "",
    ruanganAwal: String? = null
) {
    var kode by remember { mutableStateOf(kodeAwal) }
    var nama by remember { mutableStateOf(namaAwal) }
    var ruangan by remember { mutableStateOf(ruanganAwal) }

    val kodeDipakai = kodeTerpakai.any { it.equals(kode.trim(), ignoreCase = true) }
    val bisaSimpan = kode.isNotBlank() && nama.isNotBlank() && ruangan != null && !kodeDipakai

    Scaffold(
        modifier,
        topBar = { FatisdaTopBar(username, onProfileClick, onBack) },
        containerColor = PageBackground
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionCard(judul) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Kode Mata Kuliah")
                    AppTextField(kode, { kode = it }, "contoh: WMC-A")
                    if (kodeDipakai) ErrorText("Kode mata kuliah sudah dipakai.")
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Nama Mata Kuliah")
                    AppTextField(nama, { nama = it }, "contoh: Wireless & Mobile Computing")
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Ruangan")
                    SelectField(
                        value = ruangan,
                        placeholder = "Pilih ruangan",
                        options = daftarRuangan,
                        onSelect = { ruangan = daftarRuangan[it] },
                        emptyText = "Belum ada ruangan dengan data fingerprint"
                    )
                }
                HelperText("Hanya ruangan yang sudah memiliki data fingerprint yang bisa dipilih.")
                PrimaryButton(
                    "Simpan",
                    onClick = { ruangan?.let { onSimpan(kode.trim(), nama.trim(), it) } },
                    enabled = bisaSimpan
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun TambahMatkulPreview() {
    FatisdaTheme {
        TambahMatkul(
            username = "Dosen",
            daftarRuangan = listOf("LAB 2 UPT", "LAB UPT LT 3", "B4.05"),
            kodeTerpakai = emptyList(),
            onSimpan = { _, _, _ -> }, onBack = {}, onProfileClick = {}
        )
    }
}
