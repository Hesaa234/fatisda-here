package com.fatisdahere.app.ui.dosen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import kotlin.coroutines.cancellation.CancellationException

private enum class LayarDosen {
    BERANDA, PANTAU_SESI, TAMBAH_MATKUL, DETAIL_MATKUL, BUKA_SESI,
    KELOLA_PESERTA, REKAP_KEHADIRAN, EDIT_MATKUL, PROFIL
}

/** Semua data yang dibutuhkan halaman dosen. */
private class DataDosen(
    val courses: List<Course>,
    val rooms: List<Room>,
    val users: Map<String, AppUser>,
    val semuaMahasiswa: List<AppUser>,
    val sessions: Map<String, List<AttendanceSession>>,
    val attendances: Map<String, List<Attendance>>
) {
    fun namaRuang(id: String) = rooms.firstOrNull { it.id == id }?.name ?: id

    fun hadir(sessionId: String) =
        attendances[sessionId].orEmpty().count {
            it.status == PresenceStatus.HADIR
        }

    fun kehadiran(sessionId: String, uid: String) =
        attendances[sessionId]?.firstOrNull { it.studentId == uid }

    fun pesertaDari(course: Course) =
        course.studentIds.mapNotNull { users[it] }
}

private suspend fun muatDataDosen(c: AppContainer, uid: String): DataDosen {
    val courses = c.courseRepository.coursesForLecturer(uid).sortedBy { it.code }
    val rooms = c.roomRepository.rooms()
    val semuaMahasiswa = c.userRepository.students()

    val users = c.userRepository
        .usersByIds(courses.flatMap { it.studentIds })
        .associateBy { it.uid }

    val sessions = courses.associate { course ->
        course.id to c.sessionRepository
            .sessionsOfCourse(course.id)
            .sortedBy { it.startAtMs }
    }

    val attendances = c.attendanceRepository
        .forSessions(sessions.values.flatten().map { it.id })
        .groupBy { it.sessionId }

    return DataDosen(courses, rooms, users, semuaMahasiswa, sessions, attendances)
}

/** Keterangan Sakit/Izin yang sedang dibuka dosen di dialog. */
private data class BuktiTerbuka(
    val sessionId: String,
    val uid: String,
    val judul: String,
    val keterangan: String?,
    val adaBukti: Boolean
)

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
    var versi by remember { mutableStateOf(0) }
    var data by remember { mutableStateOf<DataDosen?>(null) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var galatMuat by remember { mutableStateOf<String?>(null) } // gagal memuat data
    var galatAksi by remember { mutableStateOf<String?>(null) } // gagal menyimpan perubahan
    var buktiTerbuka by remember { mutableStateOf<BuktiTerbuka?>(null) }
    // Sesi yang sedang dipantau (supaya tahu kalau sesinya berakhir saat halaman Pantau terbuka)
    var pantauSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var waktuMuat by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(user.uid, versi) {
        try {
            now = System.currentTimeMillis()
            data = muatDataDosen(container, user.uid)
            waktuMuat = System.currentTimeMillis()
            galatMuat = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            galatMuat = pesanGalat(e)
        }
    }

    // Perbarui waktu setiap detik ketika halaman Detail Matkul terbuka.
    if (layar == LayarDosen.DETAIL_MATKUL) {
        LaunchedEffect(layar) {
            while (true) {
                now = System.currentTimeMillis()
                delay(1_000)
            }
        }
    }

    galatAksi?.let {
        ErrorDialog(it) { galatAksi = null }
    }

    buktiTerbuka?.let { b ->
        BuktiDialog(
            judul = b.judul,
            keterangan = b.keterangan,
            muatFoto = {
                if (b.adaBukti) container.attendanceRepository.proofOf(b.sessionId, b.uid)?.imageBase64 else null
            },
            onDismiss = { buktiTerbuka = null }
        )
    }

    // Perbarui data kehadiran setiap 10 detik di halaman Pantau Sesi.
    if (layar == LayarDosen.PANTAU_SESI) {
        LaunchedEffect(layar) {
            while (true) {
                delay(10_000)
                versi++
            }
        }
    }

    /** Jalankan perubahan data, lalu muat ulang. */
    fun ubah(aksi: suspend () -> Unit) {
        scope.launch {
            try {
                aksi()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                galatAksi = pesanGalat(e)
            }
            versi++
        }
    }

    // Tombol back sistem.
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
        val pesan = galatMuat

        Scaffold(
            topBar = { FatisdaTopBar(user.name) },
            containerColor = PageBackground
        ) { padding ->
            if (pesan != null) {
                ErrorContent(
                    pesan,
                    onRetry = { galatMuat = null; versi++ },
                    padding = padding,
                    onLogout = onLogout
                )
            } else {
                LoadingContent(padding)
            }
        }
        return
    }

    val keBeranda = { layar = LayarDosen.BERANDA }
    val keDetail = { layar = LayarDosen.DETAIL_MATKUL }
    val keProfil = { layar = LayarDosen.PROFIL }

    // ---------- Data tampilan ----------

    val daftarMatkul = d.courses.map {
        MataKuliah(
            it.code,
            it.name,
            d.namaRuang(it.roomId),
            it.studentIds.size,
            it.id
        )
    }

    // Hanya sesi yang benar-benar sudah dimulai dan belum selesai.
    val sesiBerjalan = d.sessions.values
        .flatten()
        .filter {
            it.closedAtMs == null &&
                    now >= it.startAtMs &&
                    now <= it.endAtMs
        }
        .minByOrNull { it.startAtMs }

    val courseSesi = sesiBerjalan?.let { s ->
        d.courses.firstOrNull { it.id == s.courseId }
    }

    val sesiAktif = if (sesiBerjalan != null && courseSesi != null) {
        SesiAktif(
            kodeMatkul = courseSesi.code,
            namaMatkul = courseSesi.name,
            waktu = formatRentangJam(
                sesiBerjalan.startAtMs,
                sesiBerjalan.endAtMs
            ),
            ruang = d.namaRuang(sesiBerjalan.roomId),
            hadir = d.hadir(sesiBerjalan.id),
            totalMahasiswa = courseSesi.studentIds.size,
            sessionId = sesiBerjalan.id
        )
    } else null

    val course = d.courses.firstOrNull {
        it.id == courseIdTerpilih
    }

    val matkul = daftarMatkul.firstOrNull {
        it.id == courseIdTerpilih
    }

    // Cari sesi yang masih terjadwal atau sedang berlangsung
    // khusus untuk mata kuliah yang sedang dipilih.
    val sesiMatkul = course?.let { c ->
        d.sessions[c.id].orEmpty()
            .filter {
                it.closedAtMs == null && now <= it.endAtMs
            }
            .minByOrNull { it.startAtMs }
    }

    // Tentukan status sesi berdasarkan waktu mulai dan selesai.
    val statusSesiMatkul = when {
        sesiMatkul == null -> null

        now < sesiMatkul.startAtMs ->
            "Terjadwal, mulai jam ${formatJam(sesiMatkul.startAtMs)}"

        else ->
            "Sedang Berlangsung"
    }

    val riwayat: List<Pertemuan> = course?.let { c ->
        d.sessions[c.id].orEmpty().mapIndexed { i, s ->
            Pertemuan(
                nomor = i + 1,
                hadir = d.hadir(s.id),
                total = c.studentIds.size,
                tanggal = formatTanggal(s.startAtMs),
                waktu = formatRentangJam(s.startAtMs, s.endAtMs),
                sessionId = s.id,
                terjadwal = s.closedAtMs == null && now < s.startAtMs
            )
        }.reversed()
    } ?: emptyList()

    // Rekap per mahasiswa: hanya pertemuan yang sudah selesai; tidak ada catatan = Alpa
    val rekapMahasiswa: List<RekapMahasiswa> = course?.let { c ->
        val selesai = d.sessions[c.id].orEmpty().filter { it.hasEnded(now) }
        d.pesertaDari(c).map { u ->
            val status = selesai.map { s -> d.kehadiran(s.id, u.uid)?.status?.toUi() ?: AttendanceStatus.Alpa }
            RekapMahasiswa(
                nama = u.name,
                nim = u.nim ?: "-",
                hadir = status.count { it == AttendanceStatus.Hadir },
                izin = status.count { it == AttendanceStatus.Izin },
                sakit = status.count { it == AttendanceStatus.Sakit },
                alpa = status.count { it == AttendanceStatus.Alpa }
            )
        }
    } ?: emptyList()

    // Ruangan yang punya data fingerprint (selain ini, absen mahasiswa pasti ditolak)
    val ruanganBisaDipakai = d.rooms.filter { it.id in container.positioningParams.rooms }

    // Sesi yang dipantau: yang dipilih dari Beranda, atau sesi berjalan pertama
    val sesiDipantau = d.sessions.values.flatten().firstOrNull { it.id == pantauSessionId }
    val courseDipantau = sesiDipantau?.let { s -> d.courses.firstOrNull { it.id == s.courseId } }

    val pesertaMatkul = course?.let { c ->
        d.pesertaDari(c).map {
            Peserta(it.name, it.nim ?: "-", it.uid)
        }
    } ?: emptyList()

    when {

        layar == LayarDosen.PROFIL -> ProfilDosen(
            nama = user.name,
            nip = user.nip ?: "-",
            onLogout = onLogout,
            onBack = keBeranda
        )

        layar == LayarDosen.PANTAU_SESI &&
                (sesiDipantau == null || courseDipantau == null || sesiDipantau.closedAtMs != null || now > sesiDipantau.endAtMs) ->
            SesiBerakhir(
                username = user.name,
                judul = courseDipantau?.let { "(${it.code}) - ${it.name}" },
                onLihatRekap = courseDipantau?.let { c ->
                    {
                        courseIdTerpilih = c.id
                        layar = LayarDosen.REKAP_KEHADIRAN
                    }
                },
                onBack = keBeranda,
                onProfileClick = keProfil
            )

        layar == LayarDosen.PANTAU_SESI && sesiDipantau != null && courseDipantau != null -> PantauSesi(
            username = user.name,
            sesi = SesiAktif(
                kodeMatkul = courseDipantau.code,
                namaMatkul = courseDipantau.name,
                waktu = formatRentangJam(sesiDipantau.startAtMs, sesiDipantau.endAtMs),
                ruang = d.namaRuang(sesiDipantau.roomId),
                hadir = d.hadir(sesiDipantau.id),
                totalMahasiswa = courseDipantau.studentIds.size,
                sessionId = sesiDipantau.id
            ),
            diperbarui = waktuMuat?.let { formatJamDetik(it) },
            daftarKehadiran = d.pesertaDari(courseDipantau).map { u ->
                val a = d.kehadiran(sesiDipantau.id, u.uid)

                KehadiranMahasiswa(
                    nama = u.name,
                    nim = u.nim ?: "-",
                    status = a?.status?.toUi() ?: AttendanceStatus.BelumAbsen,
                    jam = a?.let { formatJam(it.createdAtMs) },
                    uid = u.uid,
                    keterangan = a?.note,
                    adaBukti = a?.hasProof == true
                )
            },
            onLihatKeterangan = { m ->
                buktiTerbuka = BuktiTerbuka(sesiDipantau.id, m.uid, "${m.status.label} · ${m.nama}", m.keterangan, m.adaBukti)
            },
            onTutupSesi = {
                ubah {
                    container.sessionRepository.closeSession(
                        sesiDipantau.id,
                        System.currentTimeMillis()
                    )
                }
                layar = LayarDosen.BERANDA
            },
            onBack = keBeranda,
            onProfileClick = keProfil
        )

        layar == LayarDosen.TAMBAH_MATKUL -> TambahMatkul(
            username = user.name,
            daftarRuangan = ruanganBisaDipakai.map { it.name },
            kodeTerpakai = d.courses.map { it.code },
            onSimpan = { kode, nama, ruangan ->
                val roomId = d.rooms.first { it.name == ruangan }.id

                ubah {
                    container.courseRepository.addCourse(
                        Course(
                            id = "",
                            code = kode,
                            name = nama,
                            lecturerId = user.uid,
                            roomId = roomId,
                            studentIds = emptyList()
                        )
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
            statusSesi = statusSesiMatkul,
            onBukaSesi = {
                layar = LayarDosen.BUKA_SESI
            },
            onKelolaPeserta = {
                layar = LayarDosen.KELOLA_PESERTA
            },
            onEditMatkul = {
                layar = LayarDosen.EDIT_MATKUL
            },
            onRekap = {
                layar = LayarDosen.REKAP_KEHADIRAN
            },
            onBack = keBeranda,
            onProfileClick = keProfil
        )

        layar == LayarDosen.EDIT_MATKUL &&
                course != null &&
                matkul != null -> TambahMatkul(
            username = user.name,
            judul = "Edit Mata Kuliah",
            kodeAwal = course.code,
            namaAwal = course.name,
            ruanganAwal = matkul.ruang,
            // ruangan lama tetap ditampilkan walaupun belum punya fingerprint, supaya pilihan awal tidak hilang
            daftarRuangan = (ruanganBisaDipakai + d.rooms.filter { it.id == course.roomId }).distinct().map { it.name },
            kodeTerpakai = d.courses
                .filter { it.id != course.id }
                .map { it.code },
            onSimpan = { kode, nama, ruangan ->
                val roomId = d.rooms.first { it.name == ruangan }.id

                ubah {
                    container.courseRepository.updateCourse(
                        course.copy(
                            code = kode,
                            name = nama,
                            roomId = roomId
                        )
                    )
                }

                layar = LayarDosen.DETAIL_MATKUL
            },
            onHapus = {
                ubah { container.courseRepository.deleteCourse(course.id) }
                courseIdTerpilih = null
                layar = LayarDosen.BERANDA
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

        layar == LayarDosen.KELOLA_PESERTA &&
                course != null &&
                matkul != null -> KelolaPeserta(
            username = user.name,
            matkul = matkul,
            peserta = pesertaMatkul,
            daftarMahasiswa = d.semuaMahasiswa.map {
                Peserta(it.name, it.nim ?: "-", it.uid)
            },
            onTambah = { p ->
                ubah {
                    container.courseRepository.addStudent(
                        course.id,
                        p.uid
                    )
                }
            },
            onHapus = { p ->
                ubah {
                    container.courseRepository.removeStudent(
                        course.id,
                        p.uid
                    )
                }
            },
            onBack = keDetail,
            onProfileClick = keProfil
        )

        layar == LayarDosen.REKAP_KEHADIRAN &&
                course != null &&
                matkul != null -> RekapKehadiran(
            username = user.name,
            matkul = matkul,
            riwayat = riwayat,
            catatanUntuk = { p ->
                val sesi = d.sessions[course.id].orEmpty()
                    .firstOrNull { it.id == p.sessionId }

                val selesai = sesi?.hasEnded(now) ?: true

                d.pesertaDari(course).map { u ->
                    val a = d.kehadiran(p.sessionId, u.uid)

                    CatatanKehadiran(
                        nama = u.name,
                        nim = u.nim ?: "-",
                        status = a?.status?.toUi()
                            ?: if (selesai) AttendanceStatus.Alpa
                            else AttendanceStatus.BelumAbsen,
                        manual = a?.method == AttendanceMethod.MANUAL,
                        uid = u.uid,
                        keterangan = a?.note,
                        adaBukti = a?.hasProof == true
                    )
                }
            },
            onLihatKeterangan = { p, c ->
                buktiTerbuka = BuktiTerbuka(p.sessionId, c.uid, "${c.status.label} · ${c.nama}", c.keterangan, c.adaBukti)
            },
            rekapMahasiswa = rekapMahasiswa,
            onHapusPertemuan = { p -> ubah { container.sessionRepository.deleteSession(p.sessionId) } },
            buatCsv = { buatCsvRekap(course, d.sessions[course.id].orEmpty().filter { it.hasEnded(now) }, d) },
            namaFileCsv = "rekap-${course.code}-${formatTanggalFile(now)}.csv",
            onUbahStatus = { p, uid, status ->
                ubah {
                    container.attendanceRepository.setManualStatus(
                        p.sessionId,
                        course.id,
                        uid,
                        status.toPresence()
                    )
                }
            },
            onBack = keDetail,
            onProfileClick = keProfil
        )

        else -> BerandaDosen(
            username = user.name,
            sesiAktif = sesiAktif,
            daftarMatkul = daftarMatkul,
            onPantauSesi = {
                pantauSessionId = sesiBerjalan?.id
                layar = LayarDosen.PANTAU_SESI
            },
            onTambahMatkul = {
                layar = LayarDosen.TAMBAH_MATKUL
            },
            onMatkulClick = {
                courseIdTerpilih = it.id
                layar = LayarDosen.DETAIL_MATKUL
            },
            onProfileClick = keProfil
        )
    }
}

/** Jam:menit:detik, untuk "diperbarui 13:05:20" di Pantau Sesi. */
private fun formatJamDetik(ms: Long): String =
    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale("id", "ID")).format(java.util.Date(ms))

/** Ditampilkan kalau sesi yang sedang dipantau sudah berakhir atau ditutup. */
@Composable
private fun SesiBerakhir(
    username: String,
    judul: String?,
    onLihatRekap: (() -> Unit)?,
    onBack: () -> Unit,
    onProfileClick: () -> Unit
) {
    Scaffold(
        topBar = { FatisdaTopBar(username, onProfileClick, onBack) },
        containerColor = PageBackground
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("Sesi Telah Berakhir") {
                if (judul != null) ItemTitle(judul)
                HelperText(
                    "Sesi absen ini sudah selesai atau ditutup. Mahasiswa yang belum absen tercatat Alpa, " +
                        "dan statusnya masih bisa diubah di Rekap Kehadiran."
                )
                if (onLihatRekap != null) PrimaryButton("Lihat Rekap Kehadiran", onLihatRekap)
                SecondaryButton("Kembali ke Beranda", onBack)
            }
        }
    }
}

/** Tanggal untuk nama file, misalnya 2026-11-13. */
private fun formatTanggalFile(ms: Long): String =
    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(ms))

/**
 * Rekap kehadiran satu matkul dalam format CSV (pemisah ";" supaya langsung rapi di Excel
 * berbahasa Indonesia). Baris = mahasiswa, kolom = pertemuan yang sudah selesai.
 */
private fun buatCsvRekap(course: Course, selesai: List<AttendanceSession>, d: DataDosen): String {
    fun sel(teks: String) = if (teks.contains(';') || teks.contains('"')) "\"" + teks.replace("\"", "\"\"") + "\"" else teks
    val sb = StringBuilder()
    sb.append("Rekap Kehadiran;").append(sel("(${course.code}) ${course.name}")).append("\n")
    val header = listOf("No", "Nama", "NIM") +
        selesai.mapIndexed { i, s -> "P${i + 1} (${formatTanggal(s.startAtMs)})" } +
        listOf("Hadir", "Izin", "Sakit", "Alpa", "% Hadir")
    sb.append(header.joinToString(";") { sel(it) }).append("\n")
    d.pesertaDari(course).forEachIndexed { i, u ->
        val status = selesai.map { s -> d.kehadiran(s.id, u.uid)?.status?.toUi() ?: AttendanceStatus.Alpa }
        val hadir = status.count { it == AttendanceStatus.Hadir }
        val persen = if (status.isEmpty()) 0 else Math.round(hadir * 100f / status.size)
        val baris = listOf("${i + 1}", u.name, u.nim ?: "-") +
            status.map { it.label.first().toString() } +
            listOf(
                "$hadir",
                "${status.count { it == AttendanceStatus.Izin }}",
                "${status.count { it == AttendanceStatus.Sakit }}",
                "${status.count { it == AttendanceStatus.Alpa }}",
                "$persen%"
            )
        sb.append(baris.joinToString(";") { sel(it) }).append("\n")
    }
    sb.append("Keterangan: H = Hadir, I = Izin, S = Sakit, A = Alpa\n")
    return sb.toString()
}
