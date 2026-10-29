package com.fatisdahere.app.data

/*
 * Model data aplikasi. Nama field mengikuti docs/ERD.md (koleksi Firestore).
 * Waktu disimpan sebagai milidetik (Long) supaya layar tidak perlu mengenal tipe Firebase.
 *
 * Layar TIDAK memakai kelas ini secara langsung untuk tampilan; layar punya model tampilan
 * sendiri (SessionUi, MataKuliah, dll). Penerjemahannya ada di MahasiswaApp.kt dan DosenApp.kt.
 */

enum class Role(val value: String) {
    MAHASISWA("mahasiswa"),
    DOSEN("dosen"),
    ADMIN("admin");

    companion object {
        fun from(value: String?): Role? = values().firstOrNull { it.value == value }
    }
}

/** Dokumen users/{uid} */
data class AppUser(
    val uid: String,
    val name: String,
    val email: String,
    val role: Role,
    val nim: String? = null,
    val nip: String? = null,
)

/** Dokumen rooms/{id} */
data class Room(
    val id: String,
    val name: String,
    val building: String,
    val floor: Int,
    val mainApPrefix: String,
)

/** Dokumen courses/{id} */
data class Course(
    val id: String,
    val code: String,
    val name: String,
    val lecturerId: String,
    val roomId: String,
    val studentIds: List<String>,
)

/** Dokumen sessions/{id} */
data class AttendanceSession(
    val id: String,
    val courseId: String,
    val lecturerId: String,
    val roomId: String,
    val startAtMs: Long,
    val endAtMs: Long,
    val closedAtMs: Long? = null,
) {
    /** Mahasiswa boleh absen: belum ditutup dosen dan sekarang ada di antara jam mulai dan selesai. */
    fun isOpen(nowMs: Long): Boolean = closedAtMs == null && nowMs in startAtMs..endAtMs

    /** Untuk dosen: sesi masih berjalan atau terjadwal (belum ditutup dan belum lewat jam selesai). */
    fun isActive(nowMs: Long): Boolean = closedAtMs == null && nowMs <= endAtMs

    /** Sesi sudah selesai (ditutup dosen atau lewat jam selesai). */
    fun hasEnded(nowMs: Long): Boolean = closedAtMs != null || nowMs > endAtMs
}

/** Nilai field "status" di attendances. Alpa = tidak ada dokumen. */
enum class PresenceStatus(val value: String) {
    HADIR("hadir"),
    IZIN("izin"),
    SAKIT("sakit");

    companion object {
        fun from(value: String?): PresenceStatus? = values().firstOrNull { it.value == value }
    }
}

/** Nilai field "method" di attendances. */
enum class AttendanceMethod(val value: String) {
    AUTO("auto"),
    MANUAL("manual");

    companion object {
        fun from(value: String?): AttendanceMethod? = values().firstOrNull { it.value == value }
    }
}

/** Dokumen attendances/{sessionId}_{uid} */
data class Attendance(
    val sessionId: String,
    val courseId: String,
    val studentId: String,
    val status: PresenceStatus,
    val method: AttendanceMethod,
    val detectedRoomId: String? = null,
    val knnDistance: Double? = null,
    val validScans: Int? = null,
    val displacementM: Double? = null,
    val fingerprintVersion: String? = null,
    val deviceModel: String? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
) {
    val id: String get() = attendanceId(sessionId, studentId)
}

fun attendanceId(sessionId: String, studentId: String): String = "${sessionId}_$studentId"

/** Dokumen attempts/{autoId}: catatan setiap percobaan absen, diterima atau ditolak. */
data class Attempt(
    val sessionId: String,
    val studentId: String,
    val accepted: Boolean,
    val reason: String? = null,
    val knnDistance: Double? = null,
    val validScans: Int? = null,
    val displacementM: Double? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
)
