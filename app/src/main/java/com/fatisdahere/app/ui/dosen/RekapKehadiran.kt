package com.fatisdahere.app.ui.dosen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.theme.*

/** Status yang bisa dipilih dosen. Belum Absen tidak bisa dipilih. */
private val PilihanStatus = listOf(
    AttendanceStatus.Hadir, AttendanceStatus.Izin, AttendanceStatus.Sakit, AttendanceStatus.Alpa
)

/**
 * Dua tampilan:
 *  - Per Pertemuan: status tiap mahasiswa di satu pertemuan, bisa diubah dosen
 *  - Per Mahasiswa: jumlah Hadir/Izin/Sakit/Alpa dan % kehadiran tiap mahasiswa (pertemuan yang sudah selesai)
 *
 * @param riwayat pertemuan, terbaru di atas
 * @param catatanUntuk daftar status mahasiswa untuk satu pertemuan
 * @param rekapMahasiswa rekap per mahasiswa untuk tab "Per Mahasiswa"
 */
@Composable
fun RekapKehadiran(
    username: String,
    matkul: MataKuliah,
    riwayat: List<Pertemuan>,
    catatanUntuk: (Pertemuan) -> List<CatatanKehadiran>,
    onUbahStatus: (pertemuan: Pertemuan, uid: String, status: AttendanceStatus) -> Unit,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLihatKeterangan: (pertemuan: Pertemuan, catatan: CatatanKehadiran) -> Unit = { _, _ -> },
    rekapMahasiswa: List<RekapMahasiswa> = emptyList(),
    onHapusPertemuan: ((Pertemuan) -> Unit)? = null,
    buatCsv: (() -> String)? = null,
    namaFileCsv: String = "rekap-kehadiran.csv"
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var pesanEkspor by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // teks, isError
    // Pilih lokasi simpan (Download, Drive, dll) lewat pemilih file Android; tanpa izin penyimpanan
    val simpanCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null || buatCsv == null) return@rememberLauncherForActivityResult
        pesanEkspor = try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                // BOM supaya Excel membaca huruf dengan benar (UTF-8)
                out.write("\uFEFF".toByteArray(Charsets.UTF_8))
                out.write(buatCsv().toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("File tidak bisa dibuat.")
            "Rekap tersimpan. Buka dengan Excel atau Google Sheets." to false
        } catch (e: Exception) {
            "Gagal menyimpan: ${pesanGalat(e)}" to true
        }
    }
    var perMahasiswa by remember { mutableStateOf(false) }
    var hapusTarget by remember { mutableStateOf<Pertemuan?>(null) }
    // Disimpan sebagai sessionId supaya pilihan tetap sama setelah data dimuat ulang.
    // Default: pertemuan terbaru yang sudah dimulai (bukan yang masih terjadwal).
    var dipilihId by remember {
        mutableStateOf((riwayat.firstOrNull { !it.terjadwal } ?: riwayat.firstOrNull())?.sessionId)
    }
    val pertemuan = riwayat.firstOrNull { it.sessionId == dipilihId } ?: riwayat.firstOrNull()
    val catatan = if (pertemuan != null) catatanUntuk(pertemuan) else emptyList()
    val label = { p: Pertemuan -> "Pertemuan ${p.nomor} · ${p.tanggal}" + if (p.terjadwal) " (terjadwal)" else "" }

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
                SectionCard("Rekap Kehadiran") {
                    Text(matkul.judul, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = UnsNavy)
                    PilihanTab(
                        opsi = listOf("Per Pertemuan", "Per Mahasiswa"),
                        terpilih = if (perMahasiswa) 1 else 0,
                        onPilih = { perMahasiswa = it == 1 }
                    )
                }
            }
            if (perMahasiswa) {
                if (buatCsv != null) item {
                    SectionCard {
                        SecondaryButton("Ekspor Rekap ke Excel (CSV)", { simpanCsv.launch(namaFileCsv) })
                        HelperText("Berisi status tiap pertemuan (H/I/S/A), jumlah, dan % hadir setiap mahasiswa.")
                        pesanEkspor?.let { (teks, error) ->
                            if (error) ErrorText(teks) else HelperText(teks, color = StatusSuccess)
                        }
                    }
                }
                isiPerMahasiswa(rekapMahasiswa)
                return@LazyColumn
            }
            item {
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FieldLabel("Pertemuan")
                        SelectField(
                            value = pertemuan?.let(label),
                            placeholder = "Belum ada pertemuan",
                            options = riwayat.map(label),
                            onSelect = { dipilihId = riwayat[it].sessionId },
                            enabled = riwayat.isNotEmpty()
                        )
                    }
                    val ringkasan = PilihanStatus + listOfNotNull(
                        AttendanceStatus.BelumAbsen.takeIf { s -> catatan.any { it.status == s } }
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ringkasan.forEach { s ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${catatan.count { it.status == s }}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = readableColor(s.color)
                                )
                                Text(s.label, fontSize = 12.sp, color = TextPrimary.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
            if (pertemuan != null) {
                item { SectionTitle("Daftar Mahasiswa") }
                if (pertemuan.terjadwal) item {
                    HelperText("Pertemuan ini belum dimulai, jadi status kehadiran belum bisa diubah.")
                }
                if (catatan.isEmpty()) item { HelperText("Belum ada peserta.") }
                items(catatan) { c ->
                    KartuCatatan(
                        c,
                        onUbah = { statusBaru -> onUbahStatus(pertemuan, c.uid, statusBaru) },
                        onLihat = { onLihatKeterangan(pertemuan, c) },
                        bisaDiubah = !pertemuan.terjadwal
                    )
                }
                item {
                    HelperText(
                        "Status hasil verifikasi otomatis tertulis \"auto\"; " +
                            "perubahan oleh dosen tercatat sebagai \"manual\"."
                    )
                }
                if (onHapusPertemuan != null) item {
                    SecondaryButton("Hapus Pertemuan ${pertemuan.nomor}", { hapusTarget = pertemuan }, color = StatusDanger)
                }
            }
        }
    }

    hapusTarget?.let { p ->
        ConfirmDialog(
            title = "Hapus Pertemuan ${p.nomor}?",
            text = "Pertemuan ${p.nomor} (${p.tanggal}, ${p.waktu}) beserta data kehadiran dan foto bukti izin/sakit " +
                "di pertemuan ini dihapus permanen. Gunakan untuk sesi yang salah dibuat.",
            confirmText = "Hapus",
            onConfirm = {
                hapusTarget = null
                dipilihId = null
                onHapusPertemuan?.invoke(p)
            },
            onDismiss = { hapusTarget = null }
        )
    }
}

@Composable
private fun KartuCatatan(
    c: CatatanKehadiran,
    onUbah: (AttendanceStatus) -> Unit,
    onLihat: () -> Unit,
    bisaDiubah: Boolean = true
) {
    ItemCard {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ItemTitle(c.nama)
            ItemSubtitle("${c.nim} · ${if (c.manual) "manual" else "auto"}")
            KeteranganRingkas(c.keterangan, c.adaBukti, onLihat)
        }
        Spacer(Modifier.width(12.dp))
        if (bisaDiubah) PilihStatus(c.status, onUbah) else StatusPill(c.status.label, c.status.color)
    }
}

/** Isi tab "Per Mahasiswa": ringkasan kelas lalu kartu tiap mahasiswa. */
private fun androidx.compose.foundation.lazy.LazyListScope.isiPerMahasiswa(rekap: List<RekapMahasiswa>) {
    val totalPertemuan = rekap.maxOfOrNull { it.total } ?: 0
    if (rekap.isEmpty() || totalPertemuan == 0) {
        item { HelperText("Belum ada pertemuan yang selesai, jadi rekap per mahasiswa belum tersedia.") }
        return
    }
    val rataRata = rekap.map { it.persenHadir }.average().let { Math.round(it) }
    item {
        SectionCard {
            Text(
                "Rata-rata kehadiran kelas: $rataRata%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = UnsNavy
            )
            HelperText("Dari $totalPertemuan pertemuan yang sudah selesai. Diurutkan dari kehadiran terendah.")
        }
    }
    item { SectionTitle("Daftar Mahasiswa") }
    items(rekap.sortedWith(compareBy({ it.persenHadir }, { it.nama }))) { KartuRekapMahasiswa(it) }
}

@Composable
private fun KartuRekapMahasiswa(r: RekapMahasiswa) {
    val warna = when {
        r.persenHadir >= 75 -> StatusSuccess
        r.persenHadir >= 50 -> StatusWarning
        else -> StatusDanger
    }
    ItemCard {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ItemTitle(r.nama)
            ItemSubtitle("${r.nim} · Hadir ${r.hadir} dari ${r.total}")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill("I ${r.izin}", AttendanceStatus.Izin.color)
                StatusPill("S ${r.sakit}", AttendanceStatus.Sakit.color)
                StatusPill("A ${r.alpa}", AttendanceStatus.Alpa.color)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text("${r.persenHadir}%", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = readableColor(warna))
    }
}

/** Dua/lebih pilihan berjajar untuk berpindah tampilan. */
@Composable
private fun PilihanTab(opsi: List<String>, terpilih: Int, onPilih: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opsi.forEachIndexed { i, teks ->
            val aktif = i == terpilih
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onPilih(i) },
                shape = RoundedCornerShape(10.dp),
                color = if (aktif) UnsBlue else UnsBlue.copy(alpha = 0.08f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        teks,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (aktif) androidx.compose.ui.graphics.Color.White else UnsBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun PilihStatus(status: AttendanceStatus, onPilih: (AttendanceStatus) -> Unit) {
    var terbuka by remember { mutableStateOf(false) }
    Box {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { terbuka = true },
            shape = RoundedCornerShape(50),
            color = status.color.copy(alpha = 0.15f)
        ) {
            Row(
                Modifier.padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(status.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = readableColor(status.color))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Ubah status", tint = readableColor(status.color))
            }
        }
        DropdownMenu(expanded = terbuka, onDismissRequest = { terbuka = false }) {
            PilihanStatus.forEach { s ->
                DropdownMenuItem(
                    text = { Text(s.label) },
                    onClick = {
                        terbuka = false
                        if (s != status) onPilih(s)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun RekapKehadiranPreview() {
    val p = Pertemuan(3, 2, 4, "16 Okt 2026", "13:00 - 15:00")
    FatisdaTheme {
        RekapKehadiran(
            username = "Dosen",
            matkul = MataKuliah("WMC-A", "Wireless & Mobile Computing", "LAB 2 UPT", 4),
            riwayat = listOf(p),
            catatanUntuk = {
                listOf(
                    CatatanKehadiran("Andi Pratama", "M0523001", AttendanceStatus.Hadir),
                    CatatanKehadiran("Budi Santoso", "M0523002", AttendanceStatus.Hadir),
                    CatatanKehadiran("Citra Lestari", "M0523003", AttendanceStatus.Izin, manual = true, keterangan = "Lomba tingkat nasional", adaBukti = true),
                    CatatanKehadiran("Dewi Anggraini", "M0523004", AttendanceStatus.Alpa)
                )
            },
            onUbahStatus = { _, _, _ -> }, onBack = {}, onProfileClick = {}
        )
    }
}
