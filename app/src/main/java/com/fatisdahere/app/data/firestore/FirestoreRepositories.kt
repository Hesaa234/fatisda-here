package com.fatisdahere.app.data.firestore

import android.content.Context
import com.fatisdahere.app.data.AppUser
import com.fatisdahere.app.data.Attempt
import com.fatisdahere.app.data.Attendance
import com.fatisdahere.app.data.AttendanceMethod
import com.fatisdahere.app.data.AttendanceRepository
import com.fatisdahere.app.data.AttendanceSession
import com.fatisdahere.app.data.Course
import com.fatisdahere.app.data.CourseRepository
import com.fatisdahere.app.data.NewAccount
import com.fatisdahere.app.data.PresenceStatus
import com.fatisdahere.app.data.Role
import com.fatisdahere.app.data.Room
import com.fatisdahere.app.data.RoomRepository
import com.fatisdahere.app.data.SessionRepository
import com.fatisdahere.app.data.UserRepository
import com.fatisdahere.app.data.attendanceId
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

/*
 * Versi Firebase dari semua repository (lihat data/Repositories.kt).
 * Perilakunya sama dengan versi dummy di data/fake/, jadi layar tidak perlu diubah.
 *
 * Catatan query: filter waktu dan pengurutan dilakukan di aplikasi (bukan di query)
 * supaya tidak perlu membuat composite index di Firebase Console.
 */

private const val USERS = "users"
private const val ROOMS = "rooms"
private const val COURSES = "courses"
private const val SESSIONS = "sessions"
private const val ATTENDANCES = "attendances"
private const val ATTEMPTS = "attempts"

/** Batas aman jumlah nilai untuk whereIn (SDK lama membatasi 10). */
private const val IN_LIMIT = 10

private fun slug(text: String) =
    text.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "data" }

class FirestoreUserRepository(
    private val context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : UserRepository {

    override suspend fun usersByIds(uids: Collection<String>): List<AppUser> {
        val ids = uids.distinct()
        if (ids.isEmpty()) return emptyList()
        return ids.chunked(IN_LIMIT).flatMap { chunk ->
            db.collection(USERS).whereIn(FieldPath.documentId(), chunk)
                .get().await().documents.mapNotNull { it.toAppUser() }
        }
    }

    override suspend fun students(): List<AppUser> =
        db.collection(USERS).whereEqualTo("role", Role.MAHASISWA.value)
            .get().await().documents.mapNotNull { it.toAppUser() }
            .sortedBy { it.name }

    override suspend fun allUsers(): List<AppUser> =
        db.collection(USERS).get().await().documents.mapNotNull { it.toAppUser() }
            .sortedWith(compareBy({ it.role.ordinal }, { it.name }))

    /**
     * Akun dibuat lewat "instance Firebase kedua". Kalau memakai FirebaseAuth utama,
     * Firebase otomatis me-login-kan akun baru dan admin ikut ter-logout.
     */
    override suspend fun createAccount(account: NewAccount): Result<AppUser> {
        val email = account.email.trim().lowercase()
        if (account.name.isBlank() || email.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama dan email wajib diisi."))
        }
        if (account.password.length < 6) {
            return Result.failure(IllegalArgumentException("Kata sandi minimal 6 karakter."))
        }
        return try {
            val authKedua = FirebaseAuth.getInstance(appKedua())
            val hasil = authKedua.createUserWithEmailAndPassword(email, account.password).await()
            val uid = hasil.user?.uid ?: return Result.failure(IllegalStateException("Gagal membuat akun."))
            authKedua.signOut()

            val user = AppUser(
                uid = uid,
                name = account.name.trim(),
                email = email,
                role = account.role,
                nim = account.nim?.trim()?.ifEmpty { null },
                nip = account.nip?.trim()?.ifEmpty { null },
            )
            // Ditulis dengan sesi admin (instance utama), jadi rules "hanya admin" berlaku.
            db.collection(USERS).document(uid).set(user.toMap()).await()
            Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(IllegalArgumentException("Email sudah dipakai akun lain."))
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(IllegalArgumentException("Kata sandi terlalu lemah (minimal 6 karakter)."))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(IllegalArgumentException("Format email tidak valid."))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.message ?: "Gagal membuat akun."))
        }
    }

    private fun appKedua(): FirebaseApp {
        val nama = "pembuat-akun"
        return FirebaseApp.getApps(context).firstOrNull { it.name == nama }
            ?: FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, nama)
    }
}

class FirestoreRoomRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : RoomRepository {
    override suspend fun rooms(): List<Room> =
        db.collection(ROOMS).get().await().documents.mapNotNull { it.toRoom() }.sortedBy { it.name }
}

class FirestoreCourseRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : CourseRepository {

    override suspend fun coursesForStudent(uid: String): List<Course> =
        db.collection(COURSES).whereArrayContains("studentIds", uid)
            .get().await().documents.mapNotNull { it.toCourse() }

    override suspend fun coursesForLecturer(uid: String): List<Course> =
        db.collection(COURSES).whereEqualTo("lecturerId", uid)
            .get().await().documents.mapNotNull { it.toCourse() }

    override suspend fun allCourses(): List<Course> =
        db.collection(COURSES).get().await().documents.mapNotNull { it.toCourse() }.sortedBy { it.code }

    override suspend fun addCourse(course: Course): Course {
        // id dari kode, misalnya "WMC-A" -> "wmc-a"; ditambah -2, -3 kalau sudah dipakai
        val dasar = slug(course.code)
        var id = dasar
        var n = 2
        while (db.collection(COURSES).document(id).get().await().exists()) id = "$dasar-${n++}"
        val baru = course.copy(id = id)
        db.collection(COURSES).document(id).set(baru.toMap()).await()
        return baru
    }

    override suspend fun updateCourse(course: Course) {
        db.collection(COURSES).document(course.id).set(course.toMap()).await()
    }

    override suspend fun setStudents(courseId: String, studentIds: List<String>) {
        db.collection(COURSES).document(courseId).update("studentIds", studentIds.distinct()).await()
    }
}

class FirestoreSessionRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : SessionRepository {

    override suspend fun openSessionsFor(courseIds: List<String>, nowMs: Long): List<AttendanceSession> {
        if (courseIds.isEmpty()) return emptyList()
        return courseIds.distinct().chunked(IN_LIMIT).flatMap { chunk ->
            db.collection(SESSIONS).whereIn("courseId", chunk)
                .get().await().documents.mapNotNull { it.toSession() }
        }.filter { it.isOpen(nowMs) }
    }

    override suspend fun sessionsOfCourse(courseId: String): List<AttendanceSession> =
        db.collection(SESSIONS).whereEqualTo("courseId", courseId)
            .get().await().documents.mapNotNull { it.toSession() }
            .sortedBy { it.startAtMs }

    override suspend fun openSession(
        courseId: String,
        lecturerId: String,
        roomId: String,
        startAtMs: Long,
        endAtMs: Long,
    ): AttendanceSession {
        // id "<courseId>-p<nomor>", contoh "wmc-a-p4"
        var nomor = sessionsOfCourse(courseId).size + 1
        var id = "$courseId-p$nomor"
        while (db.collection(SESSIONS).document(id).get().await().exists()) id = "$courseId-p${++nomor}"
        val sesi = AttendanceSession(id, courseId, lecturerId, roomId, startAtMs, endAtMs)
        db.collection(SESSIONS).document(id).set(sesi.toMap()).await()
        return sesi
    }

    override suspend fun closeSession(sessionId: String, nowMs: Long) {
        db.collection(SESSIONS).document(sessionId).update("closedAt", ts(nowMs)).await()
    }
}

class FirestoreAttendanceRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AttendanceRepository {

    override suspend fun hasAttended(sessionId: String, uid: String): Boolean =
        db.collection(ATTENDANCES).document(attendanceId(sessionId, uid)).get().await().exists()

    /** Memakai transaksi supaya absen dua kali di sesi yang sama pasti ditolak. */
    override suspend fun save(attendance: Attendance): Result<Unit> = try {
        val ref = db.collection(ATTENDANCES).document(attendance.id)
        db.runTransaction { tx ->
            if (tx.get(ref).exists()) throw IllegalStateException("Kamu sudah absen di sesi ini.")
            tx.set(ref, attendance.toMap())
            null
        }.await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Pesan dari dalam transaksi kadang terbungkus exception lain
        val pesan = generateSequence<Throwable>(e) { it.cause }
            .mapNotNull { it.message }
            .firstOrNull { it.contains("sudah absen") }
        Result.failure(IllegalStateException(pesan ?: e.message ?: "Gagal menyimpan absen."))
    }

    override suspend fun saveAttempt(attempt: Attempt) {
        db.collection(ATTEMPTS).add(attempt.toMap()).await()
    }

    override suspend fun forSessions(sessionIds: List<String>): List<Attendance> {
        if (sessionIds.isEmpty()) return emptyList()
        return sessionIds.distinct().chunked(IN_LIMIT).flatMap { chunk ->
            db.collection(ATTENDANCES).whereIn("sessionId", chunk)
                .get().await().documents.mapNotNull { it.toAttendance() }
        }
    }

    override suspend fun forStudent(uid: String): List<Attendance> =
        db.collection(ATTENDANCES).whereEqualTo("studentId", uid)
            .get().await().documents.mapNotNull { it.toAttendance() }

    override suspend fun setManualStatus(sessionId: String, courseId: String, studentId: String, status: PresenceStatus?) {
        val ref = db.collection(ATTENDANCES).document(attendanceId(sessionId, studentId))
        if (status == null) {
            ref.delete().await() // Alpa = tidak ada dokumen
            return
        }
        if (ref.get().await().exists()) {
            ref.update(mapOf("status" to status.value, "method" to AttendanceMethod.MANUAL.value)).await()
        } else {
            ref.set(Attendance(sessionId, courseId, studentId, status, AttendanceMethod.MANUAL).toMap()).await()
        }
    }
}
