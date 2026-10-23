package com.fatisdahere.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.fatisdahere.app.ui.auth.LoginScreen
import com.fatisdahere.app.ui.dosen.DosenApp
import com.fatisdahere.app.ui.mahasiswa.*
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // true = tes halaman dosen, false = tampilan mahasiswa
            val modeDosen = true

            var currentScreen by remember { mutableStateOf("home") }
            var isLoggedIn by remember { mutableStateOf(false) }

            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            var selectedCourse by remember { mutableStateOf<SessionUi?>(null) }

            // Data Dummy
            val sampleActiveSession = SessionUi(
                code = "IF301",
                name = "Pemrograman Mobile",
                time = "08:00 - 10:30",
                room = "Lab Komputer 3",
                lecturer = "Dosen Pengampu, M.T."
            )

            val sampleCourses = listOf(
                SessionUi("IF301", "Pemrograman Mobile", "Senin, 08:00", "Lab 3", "Dosen A"),
                SessionUi("IF302", "Kecerdasan Buatan", "Selasa, 10:00", "R. 201", "Dosen B"),
                SessionUi("IF303", "Sistem Terdistribusi", "Rabu, 13:00", "R. 202", "Dosen C"),
                SessionUi("IF304", "Jaringan Komputer", "Kamis, 08:00", "Lab 1", "Dosen D")
            )

            val sampleRecords = listOf(
                AttendanceRecord(1, AttendanceStatus.Hadir, "Lab 3", "08:00"),
                AttendanceRecord(2, AttendanceStatus.Hadir, "Lab 3", "08:00"),
                AttendanceRecord(3, AttendanceStatus.Izin, "Lab 3", "08:00"),
                AttendanceRecord(4, AttendanceStatus.BelumAbsen, "Lab 3", "08:00")
            )

            // Mengatur Tombol Back HP
            BackHandler(enabled = !modeDosen && currentScreen != "home") {
                currentScreen = "home"
            }

            if (modeDosen) {
                DosenApp()
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (currentScreen) {
                        // 1. BERANDA / HOME
                        "home" -> StudentHomeScreen(
                            userName = if (isLoggedIn) "Mahasiswa Fatisda" else "Tamu",
                            activeSession = sampleActiveSession,
                            courses = sampleCourses,
                            onAbsenClick = {
                                currentScreen = "scanning"
                            },
                            onCourseClick = { course ->
                                selectedCourse = course
                                currentScreen = "course_detail"
                            },
                            onProfileClick = {
                                if (isLoggedIn) {
                                    currentScreen = "profile"
                                } else {
                                    currentScreen = "login"
                                }
                            }
                        )

                        // 2. SCANNING / PRESENSI
                        "scanning" -> ScanningScreen(
                            userName = if (isLoggedIn) "Mahasiswa Fatisda" else "Tamu",
                            session = sampleActiveSession,
                            wifiName = "UNS-WiFi",
                            onProfileClick = {
                                if (isLoggedIn) {
                                    currentScreen = "profile"
                                } else {
                                    currentScreen = "login"
                                }
                            },
                            onBack = {
                                currentScreen = "home"
                            }
                        )

                        // 3. HASIL PRESENSI / RESULT
                        "result" -> ResultScreen(
                            userName = if (isLoggedIn) "Mahasiswa Fatisda" else "Tamu",
                            session = sampleActiveSession,
                            selectedType = AttendanceStatus.Hadir,
                            state = ResultState.Verified,
                            onRetryClick = {
                                currentScreen = "scanning"
                            },
                            onDoneClick = {
                                currentScreen = "home"
                            }
                        )

                        // 4. DETAIL MATA KULIAH
                        "course_detail" -> StudentCourseDetailScreen(
                            userName = if (isLoggedIn) "Mahasiswa Fatisda" else "Tamu",
                            session = selectedCourse ?: sampleActiveSession,
                            currentStatus = AttendanceStatus.Hadir,
                            canAbsen = true,
                            history = sampleRecords,
                            onAbsenClick = {
                                currentScreen = "scanning"
                            },
                            onBack = {
                                currentScreen = "home"
                            }
                        )

                        // 5. LOGIN SCREEN
                        "login" -> LoginScreen(
                            email = email,
                            password = password,
                            onEmailChange = { email = it },
                            onPasswordChange = { password = it },
                            onLoginClick = {
                                isLoggedIn = true
                                currentScreen = "home"
                            }
                        )

                        // 6. PROFILE SCREEN
                        "profile" -> StudentProfileScreen(
                            name = "Mahasiswa Fatisda",
                            nim = "L200220000",
                            onLogoutClick = {
                                isLoggedIn = false
                                currentScreen = "login"
                            },
                            onBack = {
                                currentScreen = "home"
                            }
                        )
                    }
                }
            }
        }
    }
}