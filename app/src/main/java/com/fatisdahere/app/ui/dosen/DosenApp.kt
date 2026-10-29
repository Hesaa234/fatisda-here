package com.fatisdahere.app.ui.dosen

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class LayarDosen {
    BERANDA, PANTAU_SESI, TAMBAH_MATKUL, DETAIL_MATKUL, BUKA_SESI,
    KELOLA_PESERTA, REKAP_KEHADIRAN, EDIT_MATKUL, PROFIL
}

/** Semua data yang dibutuhkan halaman dosen, dimuat sekaligus dari repository. */
private class DataDosen(
    val courses: List<Course>,
    val rooms: List<Room>,
    val users: Map<String, AppUser>,
    val semuaMahasiswa: List<AppUser>,
    val sessions: Map<String, List<AttendanceSession>>, // courseId -> sesi, urut lama ke baru
    val attendances: Map<String, List<Attendance>>, // sessionId -> kehadiran
) {
    fun namaRuang(id: String) = rooms.firstOrNull { it.id == id }?.name ?: id
    fun hadir(sessionId: String) = attendances[sessionId].orEmpty().count { it.status == PresenceStatus.HADIR }
    fun kehadiran(sessionId: String, uid: String) = attendances[sessionId]?.firstOrNull { it.studentId == uid }
    fun pesertaDari(course: Course) = course.studentIds.mapNotNull { users[it] }
}

private suspend fun muatDataDosen(c: AppContainer, uid: String): DataDosen {
    val courses = c.courseRepository.coursesForLecturer(uid).sortedBy { it.code }
    val rooms = c.roomRepository.rooms()
    val semuaMahasiswa = c.userRepository.students()
    val users = c.userRepository.usersByIds(courses.flatMap { it.studentIds }).associateBy { it.uid }
    val sessions = courses.associate { course ->
        course.id to c.sessionRepository.sessionsOfCourse(course.id).sortedBy { it.startAtMs }
    }
    val attendances = c.attendanceRepository
        .forSessions(sessions.values.flatten().map { it.id })
        .groupBy { it.sessionId }
    return DataDosen(courses, rooms, users, semuaMahasiswa, sessions, attendances)
}

/**
 * Alur dosen:
 * Beranda -> Tambah Mata Kuliah
 * Beranda -> Detail Matkul -> Buka Sesi Absen -> Beranda (sesi aktif muncul)
 *                          -> Kelola Peserta
 *                          -> Rekap Kehadiran
 *                          -> Edit Matkul
 * Beranda -> Pantau Sesi -> (Tutup sesi) -> Beranda
 * Header (nama di kanan atas) -> Profil -> Log Out
 *
 * Data diambil dari repository di AppContainer (sekarang data dummy, nanti Firebase).
 */
@Composable
fun DosenApp(user: AppUser, container: AppContainer, onLogout: () -> Unit) {
    var layar by rememberSaveable { mutableStateOf(LayarDosen.BERANDA) }
    var courseIdTerpilih by rememberSaveable { mutableStateOf<String?>(null) }
    var versi by remember { mutableStateOf(0) } // dinaikkan setiap ada perubahan supaya data dimuat ulang
    var data by remember { mutableStateOf<DataDosen?>(null) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(user.uid, versi) {
        now = System.currentTimeMillis()
        data = muatDataDosen(container, user.uid)
    }

    // Di Pantau Sesi, daftar kehadiran diperbarui tiap 10 detik
    if (layar == LayarDosen.PANTAU_SESI) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(10_000)
                versi++
            }
        }
    }

    /** Jalankan perubahan data, lalu muat ulang. */
    fun ubah(aksi: suspend () -> Unit) {
        scope.launch {
            aksi()
            versi++
        }
    }

    // Tombol back sistem: halaman di bawah Detail Matkul -> Detail Matkul, lainnya -> Beranda
    BackHandler(enabled = layar != LayarDosen.BERANDA) {
        layar = when (layar) {
            LayarDosen.BUKA_SESI,
            LayarDosen.KELOLA_PESERTA,
            LayarDosen.REKAP_KEHADIRAN,
            LayarDosen.EDIT_MATKUL -> LayarDosen.DETAIL_MATKUL
            else -> LayarDosen.BERANDA
        }
    }

    val d = data
    if (d == null) {
        Scaffold(topBar = { FatisdaTopBar(user.name) }, containerColor = PageBackground) { padding ->
            LoadingContent(padding)
        }
        return
    }

    val keBeranda = { layar = LayarDosen.BERANDA }
    val keDetail = { layar = LayarDosen.DETAIL_MATKUL }
    val keProfil = { layar = LayarDosen.PROFIL }

    // ---------- Data tampilan ----------
    val daftarMatkul = d.courses.map {
        MataKuliah(it.code, it.name, d.namaRuang(it.roomId), it.studentIds.size, it.id)
    }
    val sesiBerjalan = d.sessions.values.flatten().filter { it.isActive(now) }.minByOrNull { it.startAtMs }
    val courseSesi = sesiBerjalan?.let { s -> d.courses.firstOrNull { it.id == s.courseId } }
    val sesiAktif = if (sesiBerjalan != null && courseSesi != null) {
        SesiAktif(
            kodeMatkul = courseSesi.code,
            namaMatkul = courseSesi.name,
            waktu = formatRentangJam(sesiBerjalan.startAtMs, sesiBerjalan.endAtMs),
            ruang = d.namaRuang(sesiBerjalan.roomId),
            hadir = d.hadir(sesiBerjalan.id),
            totalMahasiswa = courseSesi.studentIds.size,
            sessionId = sesiBerjalan.id
        )
    } else null

    val course = d.courses.firstOrNull { it.id == courseIdTerpilih }
    val matkul = daftarMatkul.firstOrNull { it.id == courseIdTerpilih }
    val riwayat: List<Pertemuan> = course?.let { c ->
        d.sessions[c.id].orEmpty().mapIndexed { i, s ->
            Pertemuan(
                nomor = i + 1,
                hadir = d.hadir(s.id),
                total = c.studentIds.size,
                tanggal = formatTanggal(s.startAtMs),
                waktu = formatRentangJam(s.startAtMs, s.endAtMs),
                sessionId = s.id
            )
        }.reversed()
    } ?: emptyList()
    val pesertaMatkul = course?.let { c ->
        d.pesertaDari(c).map { Peserta(it.name, it.nim ?: "-", it.uid) }
    } ?: emptyList()

    when {
        layar == LayarDosen.PROFIL -> ProfilDosen(
            nama = user.name,
            nip = user.nip ?: "-",
            onLogout = onLogout,
            onBack = keBeranda
        )

        layar == LayarDosen.PANTAU_SESI && sesiAktif != null && sesiBerjalan != null && courseSesi != null -> PantauSesi(
            username = user.name,
            sesi = sesiAktif,
            daftarKehadiran = d.pesertaDari(courseSesi).map { u ->
                val a = d.kehadiran(sesiBerjalan.id, u.uid)
                KehadiranMahasiswa(
                    nama = u.name,
                    nim = u.nim ?: "-",
                    status = a?.status?.toUi() ?: AttendanceStatus.BelumAbsen,
                    jam = a?.let { formatJam(it.createdAtMs) }
                )
            },
            onTutupSesi = {
                ubah { container.sessionRepository.closeSession(sesiBerjalan.id, System.currentTimeMillis()) }
                layar = LayarDosen.BERANDA
            },
            onBack = keBeranda,
            onProfileClick = keProfil
        )

        layar == LayarDosen.TAMBAH_MATKUL -> TambahMatkul(
            username = user.name,
            daftarRuangan = d.rooms.map { it.name },
            kodeTerpakai = d.courses.map { it.code },
            onSimpan = { kode, nama, ruangan ->
                val roomId = d.rooms.first { it.name == ruangan }.id
                ubah {
                    container.courseRepository.addCourse(
                        Course(id = "", code = kode, name = nama, lecturerId = user.uid, roomId = roomId, studentIds = emptyList())
                    )
                }
                layar = LayarDosen.BERANDA
            },
            onBack = keBeranda,
            onProfileClick = keProfil
        )

        layar == LayarDosen.DETAIL_MATKUL && matkul != null -> DetailMatkul(
            username = user.name,
            matkul = matkul,
            riwayat = riwayat,
            sesiBerlangsung = sesiBerjalan != null,
            onBukaSesi = { layar = LayarDosen.BUKA_SESI },
            onKelolaPeserta = { layar = LayarDosen.KELOLA_PESERTA },
            onEditMatkul = { layar = LayarDosen.EDIT_MATKUL },
            onRekap = { layar = LayarDosen.REKAP_KEHADIRAN },
            onBack = keBeranda,
            onProfileClick = keProfil
        )

        layar == LayarDosen.EDIT_MATKUL && course != null && matkul != null -> TambahMatkul(
            username = user.name,
            judul = "Edit Mata Kuliah",
            kodeAwal = course.code,
            namaAwal = course.name,
            ruanganAwal = matkul.ruang,
            daftarRuangan = d.rooms.map { it.name },
            kodeTerpakai = d.courses.filter { it.id != course.id }.map { it.code },
            onSimpan = { kode, nama, ruangan ->
                val roomId = d.rooms.first { it.name == ruangan }.id
                ubah { container.courseRepository.updateCourse(course.copy(code = kode, name = nama, roomId = roomId)) }
                layar = LayarDosen.DETAIL_MATKUL
            },
            onBack = keDetail,
            onProfileClick = keProfil
        )

        layar == LayarDosen.BUKA_SESI && matkul != null -> BukaSesi(
            username = user.name,
            daftarMatkul = daftarMatkul,
            matkulAwal = matkul,
            onBukaAbsensi = { m, jamMulai, jamSelesai ->
                val c = d.courses.first { it.id == m.id }
                val sekarang = System.currentTimeMillis()
                ubah {
                    container.sessionRepository.openSession(
                        courseId = c.id,
                        lecturerId = user.uid,
                        roomId = c.roomId,
                        startAtMs = jamHariIni(jamMulai, sekarang),
                        endAtMs = jamHariIni(jamSelesai, sekarang)
                    )
                }
                layar = LayarDosen.BERANDA
            },
            onBack = keDetail,
            onProfileClick = keProfil
        )

        layar == LayarDosen.KELOLA_PESERTA && course != null && matkul != null -> KelolaPeserta(
            username = user.name,
            matkul = matkul,
            peserta = pesertaMatkul,
            daftarMahasiswa = d.semuaMahasiswa.map { Peserta(it.name, it.nim ?: "-", it.uid) },
            onTambah = { p -> ubah { container.courseRepository.setStudents(course.id, course.studentIds + p.uid) } },
            onHapus = { p -> ubah { container.courseRepository.setStudents(course.id, course.studentIds - p.uid) } },
            onBack = keDetail,
            onProfileClick = keProfil
        )

        layar == LayarDosen.REKAP_KEHADIRAN && course != null && matkul != null -> RekapKehadiran(
            username = user.name,
            matkul = matkul,
            riwayat = riwayat,
            catatanUntuk = { p ->
                val sesi = d.sessions[course.id].orEmpty().firstOrNull { it.id == p.sessionId }
                val selesai = sesi?.hasEnded(now) ?: true
                d.pesertaDari(course).map { u ->
                    val a = d.kehadiran(p.sessionId, u.uid)
                    CatatanKehadiran(
                        nama = u.name,
                        nim = u.nim ?: "-",
                        status = a?.status?.toUi()
                            ?: if (selesai) AttendanceStatus.Alpa else AttendanceStatus.BelumAbsen,
                        manual = a?.method == AttendanceMethod.MANUAL,
                        uid = u.uid
                    )
                }
            },
            onUbahStatus = { p, uid, status ->
                ubah {
                    container.attendanceRepository.setManualStatus(p.sessionId, course.id, uid, status.toPresence())
                }
            },
            onBack = keDetail,
            onProfileClick = keProfil
        )

        else -> BerandaDosen(
            username = user.name,
            sesiAktif = sesiAktif,
            daftarMatkul = daftarMatkul,
            onPantauSesi = { layar = LayarDosen.PANTAU_SESI },
            onTambahMatkul = { layar = LayarDosen.TAMBAH_MATKUL },
            onMatkulClick = {
                courseIdTerpilih = it.id
                layar = LayarDosen.DETAIL_MATKUL
            },
            onProfileClick = keProfil
        )
    }
}
