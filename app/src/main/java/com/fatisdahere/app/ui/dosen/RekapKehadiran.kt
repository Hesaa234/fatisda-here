package com.fatisdahere.app.ui.dosen

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
 * @param riwayat pertemuan, terbaru di atas
 * @param catatanUntuk daftar status mahasiswa untuk satu pertemuan
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
    modifier: Modifier = Modifier
) {
    // Disimpan sebagai sessionId supaya pilihan tetap sama setelah data dimuat ulang
    var dipilihId by remember { mutableStateOf(riwayat.firstOrNull()?.sessionId) }
    val pertemuan = riwayat.firstOrNull { it.sessionId == dipilihId } ?: riwayat.firstOrNull()
    val catatan = if (pertemuan != null) catatanUntuk(pertemuan) else emptyList()
    val label = { p: Pertemuan -> "Pertemuan ${p.nomor} · ${p.tanggal}" }

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
                if (catatan.isEmpty()) item { HelperText("Belum ada peserta.") }
                items(catatan) { c ->
                    KartuCatatan(c) { statusBaru -> onUbahStatus(pertemuan, c.uid, statusBaru) }
                }
                item {
                    HelperText(
                        "Status hasil verifikasi otomatis tertulis \"auto\"; " +
                            "perubahan oleh dosen tercatat sebagai \"manual\"."
                    )
                }
            }
        }
    }
}

@Composable
private fun KartuCatatan(c: CatatanKehadiran, onUbah: (AttendanceStatus) -> Unit) {
    ItemCard {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ItemTitle(c.nama)
            ItemSubtitle("${c.nim} · ${if (c.manual) "manual" else "auto"}")
        }
        Spacer(Modifier.width(12.dp))
        PilihStatus(c.status, onUbah)
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
                    CatatanKehadiran("Citra Lestari", "M0523003", AttendanceStatus.Izin, manual = true),
                    CatatanKehadiran("Dewi Anggraini", "M0523004", AttendanceStatus.Alpa)
                )
            },
            onUbahStatus = { _, _, _ -> }, onBack = {}, onProfileClick = {}
        )
    }
}
