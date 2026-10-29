package com.fatisdahere.app.data

/*
 * "Pintu" data aplikasi. Layar hanya memanggil fungsi di sini, tidak pernah langsung ke Firebase.
 *
 * Sekarang semua interface diisi versi dummy di folder data/fake/ (data di memori, hilang
 * saat aplikasi ditutup). Nanti dibuat versi Firebase (data/firestore/ dan data/auth/) dengan
 * fungsi yang sama persis, lalu cukup ditukar di di/AppContainer.kt. Layar tidak perlu diubah.
 */

interface AuthRepository {
    /** Berhasil: data user (termasuk role). Gagal: Result.failure dengan pesan yang bisa ditampilkan. */
    suspend fun signIn(email: String, password: String): Result<AppUser>
    suspend fun signOut()
}

/** Data untuk membuat akun baru dari halaman admin. */
data class NewAccount(
    val name: String,
    val email: String,
    val password: String,
    val role: Role,
    val nim: String? = null,
    val nip: String? = null,
)

interface UserRepository {
    suspend fun usersByIds(uids: Collection<String>): List<AppUser>

    /** Semua user dengan role mahasiswa (untuk Kelola Peserta). */
    suspend fun students(): List<AppUser>

    /** Semua user (untuk admin). */
    suspend fun allUsers(): List<AppUser>

    /** Admin: buat akun login + dokumen users/{uid}. Admin tetap login. */
    suspend fun createAccount(account: NewAccount): Result<AppUser>
}

interface RoomRepository {
    suspend fun rooms(): List<Room>
}

interface CourseRepository {
    suspend fun coursesForStudent(uid: String): List<Course>
    suspend fun coursesForLecturer(uid: String): List<Course>

    /** Semua mata kuliah (untuk admin). */
    suspend fun allCourses(): List<Course>

    /** id kosong akan dibuatkan. Mengembalikan course yang tersimpan (dengan id). */
    suspend fun addCourse(course: Course): Course
    suspend fun updateCourse(course: Course)
    suspend fun setStudents(courseId: String, studentIds: List<String>)
}

interface SessionRepository {
    /** Sesi yang sedang terbuka (bisa diabsen) untuk daftar matkul. */
    suspend fun openSessionsFor(courseIds: List<String>, nowMs: Long): List<AttendanceSession>
    suspend fun sessionsOfCourse(courseId: String): List<AttendanceSession>
    suspend fun openSession(
        courseId: String,
        lecturerId: String,
        roomId: String,
        startAtMs: Long,
        endAtMs: Long,
    ): AttendanceSession
    suspend fun closeSession(sessionId: String, nowMs: Long)
}

interface AttendanceRepository {
    suspend fun hasAttended(sessionId: String, uid: String): Boolean

    /** Gagal kalau mahasiswa sudah absen di sesi itu. */
    suspend fun save(attendance: Attendance): Result<Unit>
    suspend fun saveAttempt(attempt: Attempt)
    suspend fun forSessions(sessionIds: List<String>): List<Attendance>
    suspend fun forStudent(uid: String): List<Attendance>

    /** Dipakai dosen di Rekap Kehadiran. status null = Alpa (dokumen dihapus). */
    suspend fun setManualStatus(sessionId: String, courseId: String, studentId: String, status: PresenceStatus?)
}
