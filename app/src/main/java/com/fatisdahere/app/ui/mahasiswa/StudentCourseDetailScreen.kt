package com.fatisdahere.app.ui.mahasiswa

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.theme.*
import com.fatisdahere.app.ui.components.*

data class RingkasanKehadiran(val hadir: Int, val izin: Int, val sakit: Int, val alpa: Int) {
    val total get() = hadir + izin + sakit + alpa
    val persenHadir get() = if (total == 0) 0 else Math.round(hadir * 100f / total)

    companion object {
        fun dari(riwayat: List<AttendanceRecord>) = RingkasanKehadiran(
            hadir = riwayat.count { it.status == AttendanceStatus.Hadir },
            izin = riwayat.count { it.status == AttendanceStatus.Izin },
            sakit = riwayat.count { it.status == AttendanceStatus.Sakit },
            alpa = riwayat.count { it.status == AttendanceStatus.Alpa },
        )
    }
}

@Composable
fun StudentCourseDetailScreen(
    userName: String,
    session: SessionUi,
    currentStatus: AttendanceStatus,
    canAbsen: Boolean,
    history: List<AttendanceRecord>,
    onAbsenClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    sudahAbsen: Boolean = false,
    ringkasan: RingkasanKehadiran? = null,
    onProfileClick: () -> Unit = {}
) {
    Scaffold(
        modifier,
        topBar = {
            HeaderGradien(
                judul = session.name,
                subjudul = "${session.code} · ${session.lecturer}",
                onBack = onBack,
                userName = userName,
                onUserClick = onProfileClick
            ) {
                if (ringkasan != null && ringkasan.total > 0) RingkasanDetail(ringkasan)
            }
        },
        containerColor = PageBackground
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                SectionCard(modifier = Modifier.padding(bottom = 20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(Modifier.weight(1f)) { SessionInfo(session) }
                        StatusPill(currentStatus.label, currentStatus.color)
                    }
                    if (canAbsen || sudahAbsen) PrimaryButton(
                        if (sudahAbsen) "Sudah Absen ✓" else "Absen",
                        onAbsenClick,
                        enabled = !sudahAbsen
                    )
                }
            }
            item {
                SectionTitle("Riwayat Absensi", Modifier.padding(start = 2.dp, bottom = 12.dp))
            }
            if (history.isEmpty()) item {
                KartuKosong("Belum ada pertemuan yang selesai untuk mata kuliah ini.")
            }
            itemsIndexed(history, key = { _, r -> r.meeting }) { index, record ->
                BarisTimeline(
                    warna = record.status.color,
                    terisi = record.status != AttendanceStatus.BelumAbsen,
                    pertama = index == 0,
                    terakhir = index == history.lastIndex
                ) {
                    RiwayatCard(record)
                }
            }
        }
    }
}

@Composable
private fun RingkasanDetail(r: RingkasanKehadiran) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        CincinProgres(
            persen = r.persenHadir,
            ukuran = 84.dp,
            warna = Color.White,
            track = Color.White.copy(alpha = 0.3f),
            tebal = 9.dp,
            warnaTeks = Color.White,
            label = "Hadir"
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Hadir ${r.hadir} dari ${r.total} pertemuan", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("Izin ${r.izin} · Sakit ${r.sakit} · Alpa ${r.alpa}", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun RiwayatCard(record: AttendanceRecord) {
    ItemCard {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Pertemuan ${record.meeting}", fontWeight = FontWeight.Bold, color = UnsNavy, fontSize = 15.sp)
            Text(record.time, fontSize = 12.sp, color = TextMuted)
            Text(record.room, fontSize = 12.sp, color = UnsBlue, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(12.dp))
        StatusPill(record.status.label, record.status.color)
    }
}

@Preview(showBackground = true)
@Composable
private fun StudentCourseDetailPreview() {
    val history = listOf(
        AttendanceRecord(8, AttendanceStatus.Hadir, "Lab 3.2", "Sen, 20 Okt 08:00"),
        AttendanceRecord(7, AttendanceStatus.Izin, "Lab 3.2", "Sen, 13 Okt 08:00"),
        AttendanceRecord(6, AttendanceStatus.Alpa, "Lab 3.2", "Sen, 6 Okt 08:00"),
        AttendanceRecord(5, AttendanceStatus.Sakit, "Lab 3.2", "Sen, 29 Sep 08:00"),
        AttendanceRecord(4, AttendanceStatus.Hadir, "Lab 3.2", "Sen, 22 Sep 08:00")
    )
    FatisdaTheme {
        StudentCourseDetailScreen(
            "Andi", sampleSession, AttendanceStatus.Hadir, true, history, {}, {},
            ringkasan = RingkasanKehadiran.dari(history)
        )
    }
}
