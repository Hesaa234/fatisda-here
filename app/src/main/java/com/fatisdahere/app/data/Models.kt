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
    /** Keterangan dari mahasiswa untuk Izin/Sakit, misalnya "Demam, ada surat dokter". */
    val note: String? = null,
    /** true kalau ada foto bukti di proofs/{id}. */
    val hasProof: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis(),
) {
    val id: String get() = attendanceId(sessionId, studentId)
}

/**
 * Dokumen proofs/{sessionId}_{uid}: foto bukti Izin/Sakit.
 * Foto disimpan sebagai teks Base64 (JPEG yang sudah dikompres) karena Firebase Storage berbayar.
 * Dipisah dari attendances supaya daftar kehadiran tidak ikut mengunduh foto.
 */
data class Proof(
    val sessionId: String,
    val courseId: String,
    val studentId: String,
    val imageBase64: String,
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

/** Label posisi penguji saat tes KNN: benar-benar di dalam ruangan, atau sengaja di luar (koridor, ruang sebelah). */
enum class TestPosition(val value: String, val label: String) {
    DALAM("dalam", "Dalam"),
    LUAR("luar", "Luar");

    companion object {
        fun from(value: String?): TestPosition? = values().firstOrNull { it.value == value }
    }
}

/** Satu scan Wi-Fi mentah yang disimpan untuk kalibrasi: bssid -> RSSI (dBm). */
data class RawScan(val timeMs: Long, val readings: Map<String, Int>)

/**
 * Dokumen knnTests/{autoId}: satu kali uji KNN oleh admin di lapangan (menu Tes KNN).
 * Scan mentah ikut disimpan supaya hasilnya bisa dihitung ulang dengan batas lain,
 * dan nantinya bisa dipakai menambah titik fingerprint (misalnya titik "luar").
 */
data class KnnTest(
    val id: String = "",
    val roomId: String,
    val position: TestPosition,
    val note: String,
    val scans: List<RawScan>,
    val accepted: Boolean,
    val reason: String?,
    val validScans: Int,
    val avgDistance: Double?,
    val rssiMin: Int,
    val distanceMax: Double,
    val staleScans: Int,
    /** Ringkasan per scan agar mudah dibaca di Firebase Console. */
    val summaries: List<String> = emptyList(),
    val fingerprintVersion: String = "",
    val deviceModel: String = "",
    val testerId: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
)
