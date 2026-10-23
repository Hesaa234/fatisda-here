package com.fatisdahere.app.ui.mahasiswa

import androidx.compose.foundation.clickable
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
fun StudentHomeScreen(
    userName: String,
    activeSession: SessionUi?,
    courses: List<SessionUi>,
    onAbsenClick: (SessionUi) -> Unit,
    onCourseClick: (SessionUi) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier,
        topBar = { FatisdaTopBar(userName, onProfileClick) },
        containerColor = PageBackground
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (activeSession != null) item {
                SectionCard("Sesi Kelas Aktif") {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PageBackground)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LiveBadge()
                            SessionInfo(activeSession)
                            PrimaryButton("Absen", { onAbsenClick(activeSession) })
                        }
                    }
                }
            }
            item {
                Text("Mata Kuliah Saya", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = UnsNavy)
            }
            items(courses) { CourseCard(it, onClick = { onCourseClick(it) }) }
        }
    }
}

@Composable
private fun CourseCard(course: SessionUi, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(course.title, fontWeight = FontWeight.Bold, color = UnsNavy, fontSize = 15.sp)
                Text(course.lecturer, fontSize = 13.sp, color = TextPrimary.copy(alpha = 0.7f))
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(course.room, fontSize = 13.sp, color = UnsBlue, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
                Text(course.time, fontSize = 13.sp, color = TextPrimary.copy(alpha = 0.7f), textAlign = TextAlign.End)
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
        StudentHomeScreen("Andi", sampleSession, sampleCourses, {}, {}, {})
    }
}
