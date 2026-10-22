package com.fatisdahere.app.ui.mahasiswa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.components.AttendanceStatus
import com.fatisdahere.app.ui.components.ResultState
import com.fatisdahere.app.ui.components.SessionUi
import com.fatisdahere.app.ui.theme.*

@Composable
fun ResultScreen(
    userName: String,
    session: SessionUi,
    selectedType: AttendanceStatus,
    state: ResultState,
    onRetryClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(UnsBlue)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FATISDA Here",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = userName.take(1), fontWeight = FontWeight.Bold, color = UnsBlue)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = userName, color = Color.White, fontSize = 14.sp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Card Hasil Absensi
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (state == ResultState.Verified) "Absensi Berhasil!" else "Absensi Gagal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (state == ResultState.Verified) Color(0xFF10B981) else Color.Red
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "(${session.code}) - ${session.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Keterangan: ${selectedType.label}", fontSize = 14.sp, color = Color.DarkGray)
                    Text(text = "Ruang: ${session.room}", fontSize = 14.sp, color = Color.DarkGray)
                    Text(text = "Waktu: ${session.time}", fontSize = 14.sp, color = Color.DarkGray)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tombol Selesai
            Button(
                onClick = onDoneClick,
                colors = ButtonDefaults.buttonColors(containerColor = UnsBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Selesai", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}