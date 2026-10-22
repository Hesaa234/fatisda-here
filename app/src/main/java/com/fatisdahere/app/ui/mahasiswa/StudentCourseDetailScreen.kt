package com.fatisdahere.app.ui.mahasiswa

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.theme.*
import com.fatisdahere.app.ui.components.*

@Composable
fun StudentCourseDetailScreen(
    userName: String,
    session: SessionUi,
    currentStatus: AttendanceStatus,
    canAbsen: Boolean,
    history: List<AttendanceRecord>,
    onAbsenClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier,
        topBar = { FatisdaTopBar(userName, onBack = onBack) },
        containerColor = PageBackground
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Box(Modifier.weight(1f)) { SessionInfo(session) }
                        StatusPill(currentStatus.label, currentStatus.color)
                    }
                    if (canAbsen) PrimaryButton("Absen", onAbsenClick)
                }
            }
            item {
                Text("Riwayat Absensi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = UnsNavy)
            }
            items(history, key = { it.meeting }) { HistoryCard(it) }
        }
    }
}

@Composable
private fun HistoryCard(record: AttendanceRecord) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, record.status.color.copy(alpha = 0.25f))
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pertemuan ${record.meeting}", fontWeight = FontWeight.Bold, color = UnsNavy)
                StatusPill(record.status.label, record.status.color)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(record.room, fontSize = 13.sp, color = UnsBlue, textAlign = TextAlign.End)
                Text(record.time, fontSize = 13.sp, color = TextPrimary.copy(alpha = 0.7f), textAlign = TextAlign.End)
            }
        }
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
        StudentCourseDetailScreen("Andi", sampleSession, AttendanceStatus.Hadir, true, history, {}, {})
    }
}
