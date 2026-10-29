package com.fatisdahere.app.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.fatisdahere.app.data.AppUser
import com.fatisdahere.app.data.Course
import com.fatisdahere.app.data.Role
import com.fatisdahere.app.data.Room
import com.fatisdahere.app.di.AppContainer
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.dosen.KelolaPeserta
import com.fatisdahere.app.ui.dosen.MataKuliah
import com.fatisdahere.app.ui.dosen.Peserta
import com.fatisdahere.app.ui.theme.PageBackground
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

private enum class LayarAdmin { BERANDA, AKUN, TAMBAH_AKUN, MATKUL, TAMBAH_MATKUL, EDIT_MATKUL, PESERTA, RUANGAN, PROFIL }

private class DataAdmin(val users: List<AppUser>, val courses: List<Course>, val rooms: List<Room>) {
    val usersById = users.associateBy { it.uid }
    fun namaRuang(id: String) = rooms.firstOrNull { it.id == id }?.name ?: id
    fun namaUser(uid: String) = usersById[uid]?.name ?: "(akun tidak ditemukan)"
}

private suspend fun muatDataAdmin(c: AppContainer) = DataAdmin(
    users = c.userRepository.allUsers(),
    courses = c.courseRepository.allCourses(),
    rooms = c.roomRepository.rooms()
)

/**
 * Alur admin:
 * Beranda -> Akun -> Tambah Akun
 * Beranda -> Mata Kuliah -> Tambah Mata Kuliah
 *                        -> (pilih matkul) Edit Mata Kuliah -> Kelola Peserta
 * Beranda -> Ruangan (hanya lihat)
 * Header (nama di kanan atas) -> Profil -> Log Out
 */
@Composable
fun AdminApp(user: AppUser, container: AppContainer, onLogout: () -> Unit) {
    var layar by rememberSaveable { mutableStateOf(LayarAdmin.BERANDA) }
    var courseIdTerpilih by rememberSaveable { mutableStateOf<String?>(null) }
    var versi by remember { mutableStateOf(0) }
    var data by remember { mutableStateOf<DataAdmin?>(null) }
    var galatMuat by remember { mutableStateOf<String?>(null) }
    var galatAksi by remember { mutableStateOf<String?>(null) }
    var simpanAkunLoading by remember { mutableStateOf(false) }
    var simpanAkunGalat by remember { mutableStateOf<String?>(null) }
    var pesanAkun by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(versi) {
        try {
            data = muatDataAdmin(container)
            galatMuat = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            galatMuat = pesanGalat(e)
        }
    }

    galatAksi?.let { ErrorDialog(it) { galatAksi = null } }

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

    BackHandler(enabled = layar != LayarAdmin.BERANDA) {
        layar = when (layar) {
            LayarAdmin.TAMBAH_AKUN -> LayarAdmin.AKUN
            LayarAdmin.TAMBAH_MATKUL, LayarAdmin.EDIT_MATKUL -> LayarAdmin.MATKUL
            LayarAdmin.PESERTA -> LayarAdmin.EDIT_MATKUL
            else -> LayarAdmin.BERANDA
        }
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

    val keBeranda = { layar = LayarAdmin.BERANDA }
    val keProfil = { layar = LayarAdmin.PROFIL }
    val daftarDosen = d.users.filter { it.role == Role.DOSEN }.map { it.uid to it.name }
    val daftarRuangan = d.rooms.map { it.id to it.name }
    val course = d.courses.firstOrNull { it.id == courseIdTerpilih }

    when {
        layar == LayarAdmin.PROFIL -> Scaffold(
            topBar = { FatisdaTopBar(user.name, onBack = keBeranda) },
            containerColor = PageBackground
        ) { padding ->
            ProfileContent(
                fields = listOf("Nama" to user.name, "Email" to user.email, "Peran" to "Admin"),
                onLogoutClick = onLogout,
                padding = padding
            )
        }

        layar == LayarAdmin.AKUN -> DaftarAkun(
            username = user.name,
            akun = d.users.map {
                AkunUi(it.uid, it.name, it.email, it.role, if (it.role == Role.MAHASISWA) it.nim else it.nip)
            },
            onTambah = {
                simpanAkunGalat = null
                pesanAkun = null
                layar = LayarAdmin.TAMBAH_AKUN
            },
            onBack = keBeranda,
            onProfileClick = keProfil,
            pesan = pesanAkun
        )

        layar == LayarAdmin.TAMBAH_AKUN -> FormAkun(
            username = user.name,
            isLoading = simpanAkunLoading,
            errorMessage = simpanAkunGalat,
            onSimpan = { akun ->
                scope.launch {
                    simpanAkunLoading = true
                    simpanAkunGalat = null
                    try {
                        container.userRepository.createAccount(akun)
                            .onSuccess {
                                pesanAkun = "Akun ${it.name} (${it.email}) berhasil dibuat."
                                layar = LayarAdmin.AKUN
                                versi++
                            }
                            .onFailure { simpanAkunGalat = pesanGalat(it) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        simpanAkunGalat = pesanGalat(e)
                    }
                    simpanAkunLoading = false
                }
            },
            onBack = { layar = LayarAdmin.AKUN },
            onProfileClick = keProfil
        )

        layar == LayarAdmin.MATKUL -> DaftarMatkulAdmin(
            username = user.name,
            matkul = d.courses.map {
                MatkulAdminUi(it.id, it.code, it.name, d.namaUser(it.lecturerId), d.namaRuang(it.roomId), it.studentIds.size)
            },
            onTambah = { layar = LayarAdmin.TAMBAH_MATKUL },
            onMatkulClick = {
                courseIdTerpilih = it.id
                layar = LayarAdmin.EDIT_MATKUL
            },
            onBack = keBeranda,
            onProfileClick = keProfil
        )

        layar == LayarAdmin.TAMBAH_MATKUL -> FormMatkulAdmin(
            username = user.name,
            judul = "Tambah Mata Kuliah",
            daftarDosen = daftarDosen,
            daftarRuangan = daftarRuangan,
            kodeTerpakai = d.courses.map { it.code },
            onSimpan = { kode, nama, dosenId, ruanganId ->
                ubah {
                    container.courseRepository.addCourse(
                        Course(id = "", code = kode, name = nama, lecturerId = dosenId, roomId = ruanganId, studentIds = emptyList())
                    )
                }
                layar = LayarAdmin.MATKUL
            },
            onBack = { layar = LayarAdmin.MATKUL },
            onProfileClick = keProfil
        )

        layar == LayarAdmin.EDIT_MATKUL && course != null -> FormMatkulAdmin(
            username = user.name,
            judul = "Edit Mata Kuliah",
            daftarDosen = daftarDosen,
            daftarRuangan = daftarRuangan,
            kodeTerpakai = d.courses.filter { it.id != course.id }.map { it.code },
            kodeAwal = course.code,
            namaAwal = course.name,
            dosenAwal = course.lecturerId,
            ruanganAwal = course.roomId,
            onSimpan = { kode, nama, dosenId, ruanganId ->
                ubah {
                    container.courseRepository.updateCourse(
                        course.copy(code = kode, name = nama, lecturerId = dosenId, roomId = ruanganId)
                    )
                }
                layar = LayarAdmin.MATKUL
            },
            onKelolaPeserta = { layar = LayarAdmin.PESERTA },
            onBack = { layar = LayarAdmin.MATKUL },
            onProfileClick = keProfil
        )

        layar == LayarAdmin.PESERTA && course != null -> {
            val semuaMahasiswa = d.users.filter { it.role == Role.MAHASISWA }
            KelolaPeserta(
                username = user.name,
                matkul = MataKuliah(course.code, course.name, d.namaRuang(course.roomId), course.studentIds.size, course.id),
                peserta = course.studentIds.mapNotNull { d.usersById[it] }.map { Peserta(it.name, it.nim ?: "-", it.uid) },
                daftarMahasiswa = semuaMahasiswa.map { Peserta(it.name, it.nim ?: "-", it.uid) },
                onTambah = { p -> ubah { container.courseRepository.setStudents(course.id, course.studentIds + p.uid) } },
                onHapus = { p -> ubah { container.courseRepository.setStudents(course.id, course.studentIds - p.uid) } },
                onBack = { layar = LayarAdmin.EDIT_MATKUL },
                onProfileClick = keProfil
            )
        }

        layar == LayarAdmin.RUANGAN -> DaftarRuangan(
            username = user.name,
            ruangan = d.rooms.map {
                val lokasi = listOf(it.building, if (it.floor > 0) "Lantai ${it.floor}" else "")
                    .filter { s -> s.isNotBlank() }.joinToString(" · ").ifEmpty { "-" }
                RuanganUi(it.id, it.name, lokasi, it.mainApPrefix.ifEmpty { "-" })
            },
            onBack = keBeranda,
            onProfileClick = keProfil
        )

        else -> BerandaAdmin(
            username = user.name,
            jumlahAkun = d.users.size,
            jumlahMatkul = d.courses.size,
            jumlahRuangan = d.rooms.size,
            onAkun = {
                pesanAkun = null
                layar = LayarAdmin.AKUN
            },
            onMatkul = { layar = LayarAdmin.MATKUL },
            onRuangan = { layar = LayarAdmin.RUANGAN },
            onProfileClick = keProfil
        )
    }
}
