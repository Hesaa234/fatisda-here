package com.fatisdahere.app.data.fake

import com.fatisdahere.app.data.AppUser
import com.fatisdahere.app.data.Attempt
import com.fatisdahere.app.data.Attendance
import com.fatisdahere.app.data.AttendanceMethod
import com.fatisdahere.app.data.AttendanceSession
import com.fatisdahere.app.data.Course
import com.fatisdahere.app.data.PresenceStatus
import com.fatisdahere.app.data.Role
import com.fatisdahere.app.data.Room
import java.util.Calendar

/**
 * Data dummy di memori, bentuknya sama dengan koleksi Firestore (docs/ERD.md).
 * Semua perubahan (buka sesi, absen, tambah matkul, dst) hilang saat aplikasi ditutup.
 *
 * Akun demo (kata sandi apa saja, asal tidak kosong; domain email bebas):
 *   mahasiswa1 ... mahasiswa4, dosen1 ... dosen3, admin
 *
 * Jadwal sesi dibuat relatif terhadap waktu aplikasi dibuka, jadi selalu ada satu sesi
 * yang sedang berlangsung (WMC-A pertemuan 4, dosen1) untuk demo.
 */
class FakeDatabase(nowMs: Long = System.currentTimeMillis()) {

    val users = mutableListOf<AppUser>()
    val rooms = mutableListOf<Room>()
    val courses = mutableListOf<Course>()
    val sessions = mutableListOf<AttendanceSession>()
    val attendances = mutableListOf<Attendance>()
    val attempts = mutableListOf<Attempt>()

    init {
        seedUsers()
        seedRooms()
        seedCourses()
        seedSessionsAndAttendances(nowMs)
    }

    private fun seedUsers() {
        val akunMahasiswa = listOf("Andi Pratama", "Budi Santoso", "Citra Lestari", "Dewi Anggraini")
        akunMahasiswa.forEachIndexed { i, nama ->
            val n = i + 1
            users += AppUser("mhs$n", nama, "mahasiswa$n@fatisda.demo", Role.MAHASISWA, nim = "M0523%03d".format(n))
        }
        // Mahasiswa tambahan tanpa akun login, supaya daftar peserta tidak terlalu sepi.
        val tambahan = listOf(
            "Eko Prasetyo", "Fitri Handayani", "Gilang Ramadhan", "Hana Safitri", "Irfan Maulana",
            "Jihan Aulia", "Kevin Wijaya", "Laila Nurhaliza", "Muhammad Rizky", "Nadia Putri",
            "Oki Setiawan", "Putri Maharani", "Qori Rahman", "Rina Kusuma", "Satria Nugraha", "Tiara Azzahra",
        )
        tambahan.forEachIndexed { i, nama ->
            val n = i + 5
            users += AppUser("mhs$n", nama, "mhs$n@fatisda.demo", Role.MAHASISWA, nim = "M0523%03d".format(n))
        }

        users += AppUser("dsn1", "Dr. Ahmad Fauzi, M.Kom.", "dosen1@fatisda.demo", Role.DOSEN, nip = "198001012005011001")
        users += AppUser("dsn2", "Sri Wahyuni, S.Kom., M.T.", "dosen2@fatisda.demo", Role.DOSEN, nip = "198502142010122002")
        users += AppUser("dsn3", "Dr. Hendra Gunawan, M.Sc.", "dosen3@fatisda.demo", Role.DOSEN, nip = "197807202003121003")
        users += AppUser("adm1", "Admin Fatisda", "admin@fatisda.demo", Role.ADMIN)
    }

    private fun seedRooms() {
        rooms += Room("lab2-upt", "LAB 2 UPT", "UPT", 2, "d0:15:a6:d6:7b")
        rooms += Room("lab-upt-lt3", "LAB UPT LT 3", "UPT", 3, "d0:15:a6:d6:67")
        rooms += Room("b4-05", "B4.05", "Gedung B", 4, "d0:15:a6:d6:ba")
    }

    private fun mhs(vararg n: Int) = n.map { "mhs$it" }

    private fun seedCourses() {
        courses += Course("wmc-a", "WMC-A", "Wireless & Mobile Computing", "dsn1", "lab2-upt", mhs(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14))
        courses += Course("jarkom-a", "JARKOM-A", "Jaringan Komputer", "dsn1", "b4-05", mhs(1, 2, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18))
        courses += Course("pbo-a", "PBO-A", "Pemrograman Berorientasi Objek", "dsn2", "lab-upt-lt3", mhs(1, 2, 3, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20))
        courses += Course("basdat-a", "BASDAT-A", "Basis Data", "dsn3", "b4-05", mhs(1, 4, 5, 6, 7, 8, 9, 10, 11, 12))
    }

    /** Jam tertentu pada (hari ini + offsetHari). */
    private fun at(nowMs: Long, offsetHari: Int, jam: Int, menit: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, offsetHari)
            set(Calendar.HOUR_OF_DAY, jam)
            set(Calendar.MINUTE, menit)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun seedSessionsAndAttendances(nowMs: Long) {
        // courseId -> (hari pertemuan lampau relatif hari ini, jam mulai, menit mulai)
        val jadwal = mapOf(
            "wmc-a" to Triple(listOf(-21, -14, -7), 13, 0),
            "jarkom-a" to Triple(listOf(-20, -13, -6), 8, 0),
            "pbo-a" to Triple(listOf(-19, -12, -5), 10, 0),
            "basdat-a" to Triple(listOf(-18, -11, -4), 15, 30),
        )
        val duaJam = 2 * 60 * 60 * 1000L

        for (course in courses) {
            val (hari, jam, menit) = jadwal[course.id] ?: continue
            hari.forEachIndexed { idx, offset ->
                val start = at(nowMs, offset, jam, menit)
                val session = AttendanceSession(
                    id = "${course.id}-p${idx + 1}",
                    courseId = course.id,
                    lecturerId = course.lecturerId,
                    roomId = course.roomId,
                    startAtMs = start,
                    endAtMs = start + duaJam,
                    closedAtMs = start + duaJam,
                )
                sessions += session
                seedAttendances(course, session, meeting = idx + 1)
            }
        }

        // Sesi yang sedang berlangsung untuk demo: WMC-A pertemuan 4, mulai 30 menit lalu.
        val wmc = courses.first { it.id == "wmc-a" }
        val start = nowMs - 30 * 60 * 1000L
        val live = AttendanceSession("wmc-a-p4", wmc.id, wmc.lecturerId, wmc.roomId, start, start + duaJam)
        sessions += live
        // mahasiswa1-4 sengaja belum absen supaya bisa dicoba saat demo.
        wmc.studentIds.forEachIndexed { i, uid ->
            if (i >= 4 && i % 3 != 0) {
                attendances += Attendance(
                    live.id, wmc.id, uid, PresenceStatus.HADIR, AttendanceMethod.AUTO,
                    detectedRoomId = wmc.roomId, fingerprintVersion = "v1",
                    createdAtMs = start + (3 + i) * 60 * 1000L,
                )
            }
        }
    }

    /** Pola dummy: sebagian besar hadir, beberapa izin/sakit, beberapa alpa (tanpa dokumen). */
    private fun seedAttendances(course: Course, session: AttendanceSession, meeting: Int) {
        course.studentIds.forEachIndexed { i, uid ->
            val status = when ((i * 3 + meeting * 5) % 10) {
                0 -> null
                1 -> PresenceStatus.IZIN
                2 -> PresenceStatus.SAKIT
                else -> PresenceStatus.HADIR
            } ?: return@forEachIndexed
            attendances += Attendance(
                sessionId = session.id,
                courseId = course.id,
                studentId = uid,
                status = status,
                method = if (status == PresenceStatus.HADIR) AttendanceMethod.AUTO else AttendanceMethod.MANUAL,
                detectedRoomId = if (status == PresenceStatus.HADIR) course.roomId else null,
                fingerprintVersion = if (status == PresenceStatus.HADIR) "v1" else null,
                createdAtMs = session.startAtMs + (2 + i) * 60 * 1000L,
            )
        }
    }
}
