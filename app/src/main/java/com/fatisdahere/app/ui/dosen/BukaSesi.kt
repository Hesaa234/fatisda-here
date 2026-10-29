package com.fatisdahere.app.ui.dosen

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.theme.*
import java.util.Calendar

@Composable
fun BukaSesi(
    username: String,
    daftarMatkul: List<MataKuliah>,
    matkulAwal: MataKuliah?,
    onBukaAbsensi: (matkul: MataKuliah, jamMulai: String, jamSelesai: String) -> Unit,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Default: mulai sekarang, selesai 2 jam lagi (paling lambat 23:59)
    val (awalMulai, awalSelesai) = remember { jamDefault() }
    var terpilih by remember { mutableStateOf(matkulAwal) }
    var jamMulai by remember { mutableStateOf(awalMulai) }
    var jamSelesai by remember { mutableStateOf(awalSelesai) }
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
            SectionCard("Buka Sesi Absen") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Mata Kuliah")
                    SelectField(
                        value = terpilih?.judul,
                        placeholder = "Pilih mata kuliah",
                        options = daftarMatkul.map { it.judul },
                        onSelect = { terpilih = daftarMatkul[it] }
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Ruang (otomatis dari matkul)")
                    ClickableField(terpilih?.ruang, "-", onClick = {}, enabled = false)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FieldLabel("Jam Mulai")
                        ClickableField(
                            jamMulai, "", onClick = { pilihJam(jamMulai) { jamMulai = it } },
                            trailingIcon = Icons.Filled.Schedule
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FieldLabel("Jam Selesai")
                        ClickableField(
                            jamSelesai, "", onClick = { pilihJam(jamSelesai) { jamSelesai = it } },
                            trailingIcon = Icons.Filled.Schedule
                        )
                    }
                }
                if (!jamValid) ErrorText("Jam selesai harus setelah jam mulai.")
                HelperText(
                    "Mahasiswa hanya bisa absen di antara jam mulai dan jam selesai. " +
                        "Yang belum absen saat sesi berakhir tercatat Alpa."
                )
                PrimaryButton(
                    "Buka Absensi",
                    onClick = { terpilih?.let { onBukaAbsensi(it, jamMulai, jamSelesai) } },
                    enabled = bisaBuka
                )
            }
        }
    }
}

private fun jamDefault(): Pair<String, String> {
    val now = Calendar.getInstance()
    val jam = now.get(Calendar.HOUR_OF_DAY)
    val menit = now.get(Calendar.MINUTE)
    val mulai = "%02d:%02d".format(jam, menit)
    val selesaiMenit = minOf(jam * 60 + menit + 120, 23 * 60 + 59)
    val selesai = "%02d:%02d".format(selesaiMenit / 60, selesaiMenit % 60)
    return mulai to selesai
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun BukaSesiPreview() {
    val matkul = listOf(
        MataKuliah("WMC-A", "Wireless & Mobile Computing", "LAB 2 UPT", 25),
        MataKuliah("JARKOM-A", "Jaringan Komputer", "B4.05", 30)
    )
    FatisdaTheme {
        BukaSesi("Dosen", matkul, matkul.first(), { _, _, _ -> }, {}, {})
    }
}
