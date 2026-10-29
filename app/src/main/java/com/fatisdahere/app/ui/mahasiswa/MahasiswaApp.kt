package com.fatisdahere.app.ui.mahasiswa

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.fatisdahere.app.data.AppUser
import com.fatisdahere.app.data.Attempt
import com.fatisdahere.app.data.Attendance
import com.fatisdahere.app.data.AttendanceMethod
import com.fatisdahere.app.data.AttendanceSession
import com.fatisdahere.app.data.Course
import com.fatisdahere.app.data.PresenceStatus
import com.fatisdahere.app.data.Room
import com.fatisdahere.app.di.AppContainer
import com.fatisdahere.app.positioning.MotionSummary
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.theme.PageBackground
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

private enum class LayarMahasiswa { HOME, COURSE_DETAIL, SCANNING, RESULT, PROFILE }

/** Jumlah scan Wi-Fi per percobaan absen. Android membatasi 4 scan per 2 menit, jadi jangan lebih dari 4. */
private const val SCAN_PER_ABSEN = 3

/** Semua data yang dibutuhkan halaman mahasiswa, dimuat sekaligus dari repository. */
private class DataMahasiswa(
    val courses: List<Course>,
    val rooms: Map<String, Room>,
    val lecturers: Map<String, AppUser>,
    val sessions: Map<String, List<AttendanceSession>>, // courseId -> sesi, urut lama ke baru
    val myAttendance: Map<String, Attendance>, // sessionId -> kehadiran saya
) {
    fun namaRuang(id: String) = rooms[id]?.name ?: id
    fun namaDosen(id: String) = lecturers[id]?.name ?: "-"
}

private suspend fun muatDataMahasiswa(c: AppContainer, uid: String): DataMahasiswa {
    val courses = c.courseRepository.coursesForStudent(uid).sortedBy { it.code }
    val rooms = c.roomRepository.rooms().associateBy { it.id }
    val lecturers = c.userRepository.usersByIds(courses.map { it.lecturerId }).associateBy { it.uid }
    val sessions = courses.associate { course ->
        course.id to c.sessionRepository.sessionsOfCourse(course.id).sortedBy { it.startAtMs }
    }
    val mine = c.attendanceRepository.forStudent(uid).associateBy { it.sessionId }
    return DataMahasiswa(courses, rooms, lecturers, sessions, mine)
}

private class HasilAbsen(val status: AttendanceStatus, val state: ResultState, val message: String?)

/**
 * Alur mahasiswa:
 * Beranda -> Absen (sesi aktif) -> Scanning -> Hasil -> Beranda
 * Beranda -> Detail Matkul -> Absen ...
 * Header (nama di kanan atas) -> Profil -> Log Out
 *
 * Data diambil dari repository di AppContainer (sekarang data dummy, nanti Firebase).
 */
@Composable
fun MahasiswaApp(user: AppUser, container: AppContainer, onLogout: () -> Unit) {
    var layar by rememberSaveable { mutableStateOf(LayarMahasiswa.HOME) }
    var courseIdTerpilih by rememberSaveable { mutableStateOf<String?>(null) }
    var sesiAbsen by remember { mutableStateOf<SessionUi?>(null) }
    var hasil by remember { mutableStateOf<HasilAbsen?>(null) }
    var versi by remember { mutableStateOf(0) }
    var data by remember { mutableStateOf<DataMahasiswa?>(null) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var galatMuat by remember { mutableStateOf<String?>(null) } // gagal memuat data
    var galatAksi by remember { mutableStateOf<String?>(null) } // gagal menyimpan perubahan
    var proses by remember { mutableStateOf<ProsesAbsen>(ProsesAbsen.Siap) }
    var jobAbsen by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(user.uid, versi) {
        try {
            now = System.currentTimeMillis()
            data = muatDataMahasiswa(container, user.uid)
            galatMuat = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            galatMuat = pesanGalat(e)
        }
    }

    galatAksi?.let { ErrorDialog(it) { galatAksi = null } }

    // Keluar dari halaman absen membatalkan scan yang sedang berjalan
    val keHome = {
        jobAbsen?.cancel()
        layar = LayarMahasiswa.HOME
    }

    BackHandler(enabled = layar != LayarMahasiswa.HOME, onBack = keHome)

    val d = data
    if (d == null) {
        val pesan = galatMuat
        Scaffold(topBar = { FatisdaTopBar(user.name) }, containerColor = PageBackground) { padding ->
            if (pesan != null) {
                ErrorContent(pesan, onRetry = { galatMuat = null; versi++ }, padding = padding, onLogout = onLogout)
            } else {
                LoadingContent(padding)
            }
        }
        return
    }

    val keProfil = {
        jobAbsen?.cancel()
        layar = LayarMahasiswa.PROFILE
    }

    fun uiCourse(c: Course): SessionUi {
        val terakhir = d.sessions[c.id].orEmpty().lastOrNull()
        return SessionUi(
            code = c.code,
            name = c.name,
            time = terakhir?.let { formatHariJam(it.startAtMs) } ?: "-",
            room = d.namaRuang(c.roomId),
            lecturer = d.namaDosen(c.lecturerId),
            courseId = c.id,
            roomId = c.roomId
        )
    }

    fun uiSesi(c: Course, s: AttendanceSession) = SessionUi(
        code = c.code,
        name = c.name,
        time = formatRentangJam(s.startAtMs, s.endAtMs),
        room = d.namaRuang(s.roomId),
        lecturer = d.namaDosen(c.lecturerId),
        courseId = c.id,
        sessionId = s.id,
        roomId = s.roomId
    )

    fun mulaiAbsen(sesi: SessionUi) {
        val sessionId = sesi.sessionId ?: return
        sesiAbsen = sesi
        if (d.myAttendance.containsKey(sessionId)) {
            hasil = HasilAbsen(
                d.myAttendance.getValue(sessionId).status.toUi(),
                ResultState.Rejected,
                "Kamu sudah absen di sesi ini."
            )
            layar = LayarMahasiswa.RESULT
        } else {
            layar = LayarMahasiswa.SCANNING
        }
    }

    fun tampilkanHasil(h: HasilAbsen) {
        hasil = h
        layar = LayarMahasiswa.RESULT
        versi++
    }

    /**
     * Hadir: scan Wi-Fi, putuskan dengan engine KNN, catat percobaannya, lalu simpan kalau diterima.
     * Sakit/Izin: langsung disimpan sebagai keterangan manual.
     */
    fun kirimAbsen(sesi: SessionUi, status: AttendanceStatus) {
        if (jobAbsen?.isActive == true) return
        val sessionId = sesi.sessionId ?: return
        jobAbsen = scope.launch {
            try {
                // Cek ulang: sesi bisa saja sudah lewat atau ditutup dosen sejak halaman dibuka
                suspend fun sesiMasihBuka() = container.sessionRepository.sessionsOfCourse(sesi.courseId)
                    .firstOrNull { it.id == sessionId }?.isOpen(System.currentTimeMillis()) == true
                val sesiBerakhir = HasilAbsen(status, ResultState.Rejected, "Sesi absen sudah berakhir atau ditutup dosen.")

                proses = ProsesAbsen.Memproses
                if (!sesiMasihBuka()) return@launch tampilkanHasil(sesiBerakhir)

                var attendance = Attendance(
                    sessionId = sessionId,
                    courseId = sesi.courseId,
                    studentId = user.uid,
                    status = status.toPresence() ?: PresenceStatus.HADIR,
                    method = AttendanceMethod.MANUAL,
                    fingerprintVersion = container.fingerprint.version,
                    deviceModel = Build.MODEL
                )

                if (status == AttendanceStatus.Hadir) {
                    if (sesi.roomId !in container.positioningParams.rooms) {
                        return@launch tampilkanHasil(
                            HasilAbsen(status, ResultState.Rejected, "Ruangan ${sesi.room} belum punya data fingerprint Wi-Fi. Hubungi dosen.")
                        )
                    }

                    val scanner = container.wifiScanner
                    proses = ProsesAbsen.Memindai(0, SCAN_PER_ABSEN)
                    val scans = scanner.scan(SCAN_PER_ABSEN) { proses = ProsesAbsen.Memindai(it, SCAN_PER_ABSEN) }
                    // PDR belum dipasang: perpindahan dianggap 0 m.
                    val decision = container.positioningEngine.decide(scans, MotionSummary(0, 0.0), sesi.roomId)

                    proses = ProsesAbsen.Memproses
                    // Log percobaan tidak boleh menggagalkan absen
                    try {
                        container.attendanceRepository.saveAttempt(
                            Attempt(
                                sessionId = sessionId,
                                studentId = user.uid,
                                accepted = decision.accepted,
                                reason = decision.reason?.name,
                                knnDistance = decision.avgKnnDistance,
                                validScans = decision.validScans,
                                displacementM = decision.displacementM
                            )
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // abaikan
                    }

                    if (!decision.accepted) {
                        val alasan = decision.reason?.pesan(sesi.room, container.fingerprint.ssid)
                            ?: "Posisimu belum terkonfirmasi di dalam ruangan. Coba lagi."
                        val catatanThrottle = if (scanner.staleScanCount > 0) {
                            "\n\nAndroid membatasi scan Wi-Fi (maks 4 kali per 2 menit). Tunggu sebentar sebelum mencoba lagi."
                        } else ""
                        return@launch tampilkanHasil(HasilAbsen(status, ResultState.Rejected, alasan + catatanThrottle))
                    }

                    attendance = attendance.copy(
                        method = AttendanceMethod.AUTO,
                        detectedRoomId = sesi.roomId,
                        knnDistance = decision.avgKnnDistance,
                        validScans = decision.validScans,
                        displacementM = decision.displacementM
                    )
                    // Scan bisa sampai 30 detik; sesi mungkin berakhir di tengahnya
                    if (!sesiMasihBuka()) return@launch tampilkanHasil(sesiBerakhir)
                }

                val result = container.attendanceRepository.save(attendance)
                tampilkanHasil(
                    HasilAbsen(
                        status = status,
                        state = if (result.isSuccess) ResultState.Verified else ResultState.Rejected,
                        message = result.exceptionOrNull()?.message
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                galatAksi = "Izin lokasi ditolak sistem. Berikan izin lokasi lalu coba lagi."
            } catch (e: Exception) {
                galatAksi = pesanGalat(e)
            } finally {
                proses = ProsesAbsen.Siap
            }
        }
    }

    // Sesi terbuka pertama dari semua matkul saya
    val sesiTerbuka = d.courses.flatMap { c ->
        d.sessions[c.id].orEmpty().filter { it.isOpen(now) }.map { c to it }
    }.minByOrNull { it.second.startAtMs }

    val course = d.courses.firstOrNull { it.id == courseIdTerpilih }
    val sesi = sesiAbsen

    when {
        layar == LayarMahasiswa.PROFILE -> StudentProfileScreen(
            name = user.name,
            nim = user.nim ?: "-",
            onLogoutClick = onLogout,
            onBack = keHome
        )

        layar == LayarMahasiswa.COURSE_DETAIL && course != null -> {
            val daftarSesi = d.sessions[course.id].orEmpty()
            val terbuka = daftarSesi.firstOrNull { it.isOpen(now) }
            val riwayat = daftarSesi.mapIndexedNotNull { i, s ->
                if (!s.hasEnded(now)) return@mapIndexedNotNull null
                AttendanceRecord(
                    meeting = i + 1,
                    status = d.myAttendance[s.id]?.status?.toUi() ?: AttendanceStatus.Alpa,
                    room = d.namaRuang(s.roomId),
                    time = formatHariTanggalJam(s.startAtMs)
                )
            }.reversed()
            val statusSekarang = when {
                terbuka != null -> d.myAttendance[terbuka.id]?.status?.toUi() ?: AttendanceStatus.BelumAbsen
                else -> riwayat.firstOrNull()?.status ?: AttendanceStatus.BelumAbsen
            }
            val uiHeader = if (terbuka != null) uiSesi(course, terbuka) else uiCourse(course)

            StudentCourseDetailScreen(
                userName = user.name,
                session = uiHeader,
                currentStatus = statusSekarang,
                canAbsen = terbuka != null && !d.myAttendance.containsKey(terbuka.id),
                history = riwayat,
                onAbsenClick = { mulaiAbsen(uiHeader) },
                onBack = keHome
            )
        }

        layar == LayarMahasiswa.SCANNING && sesi != null -> ScanningScreen(
            userName = user.name,
            session = sesi,
            wifiName = container.fingerprint.ssid,
            proses = proses,
            onKirim = { status -> kirimAbsen(sesi, status) },
            onProfileClick = keProfil,
            onBack = keHome
        )

        layar == LayarMahasiswa.RESULT && sesi != null && hasil != null -> {
            val h = hasil!!
            ResultScreen(
                userName = user.name,
                session = sesi,
                selectedType = h.status,
                state = h.state,
                message = h.message,
                onRetryClick = { mulaiAbsen(sesi) },
                onDoneClick = keHome
            )
        }

        else -> StudentHomeScreen(
            userName = user.name,
            activeSession = sesiTerbuka?.let { (c, s) -> uiSesi(c, s) },
            courses = d.courses.map { uiCourse(it) },
            onAbsenClick = { mulaiAbsen(it) },
            onCourseClick = {
                courseIdTerpilih = it.courseId
                layar = LayarMahasiswa.COURSE_DETAIL
            },
            onProfileClick = keProfil
        )
    }
}
