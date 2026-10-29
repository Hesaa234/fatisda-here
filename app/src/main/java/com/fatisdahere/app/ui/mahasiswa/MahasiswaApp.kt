package com.fatisdahere.app.ui.mahasiswa

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.fatisdahere.app.data.AppUser
import com.fatisdahere.app.data.Attendance
import com.fatisdahere.app.data.AttendanceMethod
import com.fatisdahere.app.data.AttendanceSession
import com.fatisdahere.app.data.Course
import com.fatisdahere.app.data.PresenceStatus
import com.fatisdahere.app.data.Room
import com.fatisdahere.app.di.AppContainer
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.theme.PageBackground
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

private enum class LayarMahasiswa { HOME, COURSE_DETAIL, SCANNING, RESULT, PROFILE }

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

    BackHandler(enabled = layar != LayarMahasiswa.HOME) {
        layar = LayarMahasiswa.HOME
    }

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

    val keHome = { layar = LayarMahasiswa.HOME }
    val keProfil = { layar = LayarMahasiswa.PROFILE }

    fun uiCourse(c: Course): SessionUi {
        val terakhir = d.sessions[c.id].orEmpty().lastOrNull()
        return SessionUi(
            code = c.code,
            name = c.name,
            time = terakhir?.let { formatHariJam(it.startAtMs) } ?: "-",
            room = d.namaRuang(c.roomId),
            lecturer = d.namaDosen(c.lecturerId),
            courseId = c.id
        )
    }

    fun uiSesi(c: Course, s: AttendanceSession) = SessionUi(
        code = c.code,
        name = c.name,
        time = formatRentangJam(s.startAtMs, s.endAtMs),
        room = d.namaRuang(s.roomId),
        lecturer = d.namaDosen(c.lecturerId),
        courseId = c.id,
        sessionId = s.id
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

    /** Simpan absen ke repository, lalu tampilkan halaman hasil. */
    fun kirimAbsen(sesi: SessionUi, status: AttendanceStatus) {
        scope.launch {
            try {
                val presence = status.toPresence() ?: PresenceStatus.HADIR
                // SIMULASI: belum memakai hasil engine KNN. Nanti field knn* diisi dari PositioningDecision.
                val result = container.attendanceRepository.save(
                    Attendance(
                        sessionId = sesi.sessionId ?: "",
                        courseId = sesi.courseId,
                        studentId = user.uid,
                        status = presence,
                        method = if (presence == PresenceStatus.HADIR) AttendanceMethod.AUTO else AttendanceMethod.MANUAL,
                        detectedRoomId = null,
                        fingerprintVersion = container.fingerprint.version,
                        deviceModel = Build.MODEL
                    )
                )
                hasil = HasilAbsen(
                    status = status,
                    state = if (result.isSuccess) ResultState.Verified else ResultState.Rejected,
                    message = result.exceptionOrNull()?.message
                )
                layar = LayarMahasiswa.RESULT
                versi++
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                galatAksi = pesanGalat(e)
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
            onSelesai = { status -> kirimAbsen(sesi, status) },
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
