package com.fatisdahere.app.ui.mahasiswa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.ui.components.AttendanceStatus
import com.fatisdahere.app.ui.components.SessionUi
import com.fatisdahere.app.ui.theme.*

@Composable
fun ScanningScreen(
    userName: String,
    session: SessionUi,
    wifiName: String,
    onProfileClick: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var selectedStatus by remember { mutableStateOf<AttendanceStatus?>(null) }
    var isVerified by remember { mutableStateOf(false) }
    var isScanFinished by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top Bar Header yang disamakan dengan HomeScreen
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

            // Area Profil
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onProfileClick() }
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile Icon",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = userName,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card Absensi
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Absensi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE0F2FE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Red)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sedang Berlangsung",
                                color = Color(0xFF0369A1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "(${session.code}) - ${session.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Keterangan: ${selectedStatus?.label ?: "Belum Absen"}",
                        fontSize = 14.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val statuses = listOf(
                            AttendanceStatus.Hadir to "Hadir",
                            AttendanceStatus.Sakit to "Sakit",
                            AttendanceStatus.Izin to "Izin"
                        )

                        statuses.forEach { (status, label) ->
                            val isSelected = selectedStatus == status
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) UnsBlue else Color.White)
                                    .border(1.dp, if (isSelected) UnsBlue else Color.Gray, RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (!isVerified) {
                                            selectedStatus = status
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (selectedStatus != null) {
                                isVerified = true
                            }
                        },
                        enabled = selectedStatus != null && !isVerified,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.LightGray,
                            disabledContainerColor = if (isVerified) Color(0xFFE0E0E0) else Color.LightGray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Verifikasi Kehadiran",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isVerified) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (isScanFinished) "Scanning WIFI\nComplete" else "Scanning WIFI\n($wifiName)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(160.dp)
                ) {
                    // Perbaikan pada CircularProgressIndicator (menggunakan Float langsung)
                    CircularProgressIndicator(
                        progress = if (isScanFinished) 1.0f else 0.33f,
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Gray,
                        trackColor = Color(0xFFE0E0E0),
                        strokeWidth = 12.dp
                    )
                    Text(
                        text = if (isScanFinished) "3 of 3" else "1 of 3",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isScanFinished) {
                    Text(
                        text = "Verifikasi absen diterima, anda berada\ndi dalam ruang kelas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    Text(
                        text = "Sedang Memproses",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = UnsBlue,
                        modifier = Modifier.clickable {
                            isScanFinished = true
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { isScanFinished = true }) {
                        Text(text = "(Klik untuk Simulasi Selesai)", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}