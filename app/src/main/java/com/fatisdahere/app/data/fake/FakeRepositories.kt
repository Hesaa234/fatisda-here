package com.fatisdahere.app.data.fake

import com.fatisdahere.app.data.AppUser
import com.fatisdahere.app.data.Attempt
import com.fatisdahere.app.data.Attendance
import com.fatisdahere.app.data.AttendanceMethod
import com.fatisdahere.app.data.AttendanceRepository
import com.fatisdahere.app.data.AttendanceSession
import com.fatisdahere.app.data.AuthRepository
import com.fatisdahere.app.data.Course
import com.fatisdahere.app.data.CourseRepository
import com.fatisdahere.app.data.PresenceStatus
import com.fatisdahere.app.data.Role
import com.fatisdahere.app.data.Room
import com.fatisdahere.app.data.RoomRepository
import com.fatisdahere.app.data.SessionRepository
import com.fatisdahere.app.data.UserRepository
import com.fatisdahere.app.data.attendanceId
import kotlinx.coroutines.delay

/*
 * Versi dummy dari semua repository. Perilakunya dibuat mirip Firestore
 * (misalnya absen dua kali di sesi yang sama ditolak) supaya saat ditukar ke Firebase
 * tidak ada kejutan di layar.
 */

/** Jeda kecil supaya indikator loading di layar ikut teruji. */
private suspend fun simulasiJaringan() = delay(250)

class FakeAuthRepository(private val db: FakeDatabase) : AuthRepository {
    override suspend fun signIn(email: String, password: String): Result<AppUser> {
        simulasiJaringan()
        if (email.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email dan kata sandi wajib diisi."))
        }
        // Domain email bebas: yang dicocokkan hanya bagian sebelum "@" (mahasiswa1, dosen1, admin, ...).
        val nama = email.trim().substringBefore('@').lowercase()
        val user = db.users.firstOrNull { it.email.substringBefore('@').lowercase() == nama }
            ?: return Result.failure(IllegalArgumentException("Akun tidak ditemukan. Coba mahasiswa1, dosen1, atau admin."))
        return Result.success(user)
    }

    override suspend fun signOut() {}
}

class FakeUserRepository(private val db: FakeDatabase) : UserRepository {
    override suspend fun usersByIds(uids: Collection<String>): List<AppUser> {
        simulasiJaringan()
        val set = uids.toSet()
        return db.users.filter { it.uid in set }
    }

    override suspend fun students(): List<AppUser> {
        simulasiJaringan()
        return db.users.filter { it.role == Role.MAHASISWA }
    }
}

class FakeRoomRepository(private val db: FakeDatabase) : RoomRepository {
    override suspend fun rooms(): List<Room> {
        simulasiJaringan()
        return db.rooms.toList()
    }
}

class FakeCourseRepository(private val db: FakeDatabase) : CourseRepository {
    override suspend fun coursesForStudent(uid: String): List<Course> {
        simulasiJaringan()
        return db.courses.filter { uid in it.studentIds }
    }

    override suspend fun coursesForLecturer(uid: String): List<Course> {
        simulasiJaringan()
        return db.courses.filter { it.lecturerId == uid }
    }

    override suspend fun addCourse(course: Course): Course {
        simulasiJaringan()
        val dasar = course.code.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "matkul" }
        var id = dasar
        var n = 2
        while (db.courses.any { it.id == id }) id = "$dasar-${n++}"
        val baru = course.copy(id = id)
        db.courses += baru
        return baru
    }

    override suspend fun updateCourse(course: Course) {
        simulasiJaringan()
        val idx = db.courses.indexOfFirst { it.id == course.id }
        if (idx >= 0) db.courses[idx] = course
    }

    override suspend fun setStudents(courseId: String, studentIds: List<String>) {
        simulasiJaringan()
        val idx = db.courses.indexOfFirst { it.id == courseId }
        if (idx >= 0) db.courses[idx] = db.courses[idx].copy(studentIds = studentIds.distinct())
    }
}

class FakeSessionRepository(private val db: FakeDatabase) : SessionRepository {
    override suspend fun openSessionsFor(courseIds: List<String>, nowMs: Long): List<AttendanceSession> {
        simulasiJaringan()
        return db.sessions.filter { it.courseId in courseIds && it.isOpen(nowMs) }
    }

    override suspend fun sessionsOfCourse(courseId: String): List<AttendanceSession> {
        simulasiJaringan()
        return db.sessions.filter { it.courseId == courseId }.sortedBy { it.startAtMs }
    }

    override suspend fun openSession(
        courseId: String,
        lecturerId: String,
        roomId: String,
        startAtMs: Long,
        endAtMs: Long,
    ): AttendanceSession {
        simulasiJaringan()
        val nomor = db.sessions.count { it.courseId == courseId } + 1
        var id = "$courseId-p$nomor"
        var n = nomor
        while (db.sessions.any { it.id == id }) id = "$courseId-p${++n}"
        val sesi = AttendanceSession(id, courseId, lecturerId, roomId, startAtMs, endAtMs)
        db.sessions += sesi
        return sesi
    }

    override suspend fun closeSession(sessionId: String, nowMs: Long) {
        simulasiJaringan()
        val idx = db.sessions.indexOfFirst { it.id == sessionId }
        if (idx >= 0) db.sessions[idx] = db.sessions[idx].copy(closedAtMs = nowMs)
    }
}

class FakeAttendanceRepository(private val db: FakeDatabase) : AttendanceRepository {
    override suspend fun hasAttended(sessionId: String, uid: String): Boolean {
        simulasiJaringan()
        return db.attendances.any { it.id == attendanceId(sessionId, uid) }
    }

    override suspend fun save(attendance: Attendance): Result<Unit> {
        simulasiJaringan()
        if (db.attendances.any { it.id == attendance.id }) {
            return Result.failure(IllegalStateException("Kamu sudah absen di sesi ini."))
        }
        db.attendances += attendance
        return Result.success(Unit)
    }

    override suspend fun saveAttempt(attempt: Attempt) {
        simulasiJaringan()
        db.attempts += attempt
    }

    override suspend fun forSessions(sessionIds: List<String>): List<Attendance> {
        simulasiJaringan()
        val set = sessionIds.toSet()
        return db.attendances.filter { it.sessionId in set }
    }

    override suspend fun forStudent(uid: String): List<Attendance> {
        simulasiJaringan()
        return db.attendances.filter { it.studentId == uid }
    }

    override suspend fun setManualStatus(sessionId: String, courseId: String, studentId: String, status: PresenceStatus?) {
        simulasiJaringan()
        val id = attendanceId(sessionId, studentId)
        val idx = db.attendances.indexOfFirst { it.id == id }
        when {
            status == null -> if (idx >= 0) db.attendances.removeAt(idx)
            idx >= 0 -> db.attendances[idx] = db.attendances[idx].copy(status = status, method = AttendanceMethod.MANUAL)
            else -> db.attendances += Attendance(sessionId, courseId, studentId, status, AttendanceMethod.MANUAL)
        }
    }
}
