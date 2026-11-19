package com.fatisdahere.app.ui.mahasiswa

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.theme.*
import com.fatisdahere.app.ui.components.*

@Composable
fun StudentHomeScreen(
    userName: String,
    activeSession: SessionUi?,
    courses: List<SessionUi>,
    onAbsenClick: (SessionUi) -> Unit,
    onCourseClick: (SessionUi) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    sudahAbsen: Boolean = false,
    ringkasan: RingkasanKehadiran? = null
) {
    val namaDepan = userName.trim().substringBefore(' ').ifBlank { userName }
    Scaffold(
        modifier,
        topBar = {
            HeaderGradien(
                judul = "Halo, $namaDepan",
                subjudul = "FATISDA Here",
                userName = userName,
                onUserClick = onProfileClick
            ) {
                RingkasanHeader(ringkasan)
            }
        },
        containerColor = PageBackground
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (activeSession != null) item {
                KartuSesiAktif(
                    judul = activeSession.title,
                    keterangan = "Sesi absen sedang dibuka",
                    tombol = if (sudahAbsen) "Sudah Absen ✓" else "Absen",
                    onClick = { onAbsenClick(activeSession) },
                    tombolAktif = !sudahAbsen,
                    baris = listOf("${activeSession.time} · ${activeSession.room}", activeSession.lecturer),
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }
            item {
                SectionTitle("Mata Kuliah Saya", Modifier.padding(start = 2.dp, bottom = 12.dp))
            }
            if (courses.isEmpty()) item {
                KartuKosong("Kamu belum terdaftar di mata kuliah mana pun. Hubungi dosen atau admin untuk didaftarkan.")
            }
            itemsIndexed(courses) { index, course ->
                val live = activeSession != null && activeSession.code == course.code
                BarisTimeline(
                    warna = if (live) UnsBlue else StatusGray,
                    aktif = live,
                    terisi = live,
                    pertama = index == 0,
                    terakhir = index == courses.lastIndex
                ) {
                    CourseCard(course, live, onClick = { onCourseClick(course) })
                }
            }
        }
    }
}

@Composable
private fun RingkasanHeader(ringkasan: RingkasanKehadiran?) {
    if (ringkasan == null || ringkasan.total == 0) {
        Text(
            "Belum ada pertemuan yang selesai.",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp
        )
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        CincinProgres(
            persen = ringkasan.persenHadir,
            ukuran = 88.dp,
            warna = Color.White,
            track = Color.White.copy(alpha = 0.3f),
            tebal = 9.dp,
            warnaTeks = Color.White,
            label = "Hadir"
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${ringkasan.hadir} hadir · ${ringkasan.izin + ringkasan.sakit} izin/sakit · ${ringkasan.alpa} alpa",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "dari ${ringkasan.total} pertemuan, semua mata kuliah",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CourseCard(course: SessionUi, live: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().muncul(),
        shape = BentukItem,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        border = if (live) BorderStroke(2.dp, UnsBlue) else null
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(course.title, fontWeight = FontWeight.Bold, color = UnsNavy, fontSize = 15.sp)
                Text(course.lecturer, fontSize = 13.sp, color = TextMuted)
                if (live) StatusPill("Sesi dibuka", UnsBlue, Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(course.room, fontSize = 13.sp, color = UnsBlue, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
                Text(course.time, fontSize = 12.sp, color = TextMuted, textAlign = TextAlign.End)
            }
        }
    }
}

internal val sampleSession = SessionUi("IF2101", "Pemrograman Mobile", "08:00 - 10:30", "Lab 3.2", "Dr. Budi Santoso, M.Kom.")
internal val sampleCourses = listOf(
    sampleSession,
    SessionUi("IF2102", "Basis Data", "10:30 - 12:10", "R. 2.4", "Siti Aminah, M.T."),
    SessionUi("IF2103", "Jaringan Komputer", "13:00 - 15:30", "Lab 2.1", "Ir. Agus Pratama, M.Sc.")
)

@Preview(showBackground = true)
@Composable
private fun StudentHomeScreenPreview() {
    FatisdaTheme {
        StudentHomeScreen("Andi", sampleSession, sampleCourses, {}, {}, {}, ringkasan = RingkasanKehadiran(11, 1, 0, 0))
    }
}
