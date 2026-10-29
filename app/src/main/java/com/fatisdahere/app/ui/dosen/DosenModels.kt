package com.fatisdahere.app.ui.dosen

import com.fatisdahere.app.ui.components.AttendanceStatus

/*
 * Model tampilan untuk halaman dosen. Diisi dari data asli (repository) di DosenApp.kt.
 * Field "id"/"uid"/"sessionId" boleh kosong untuk data contoh di Preview.
 */

data class MataKuliah(
    val kode: String,
    val nama: String,
    val ruang: String,
    val jumlahPeserta: Int,
    val id: String = ""
) {
    val judul get() = "($kode) - $nama"
}

data class SesiAktif(
    val kodeMatkul: String,
    val namaMatkul: String,
    val waktu: String,
    val ruang: String,
    val hadir: Int,
    val totalMahasiswa: Int,
    val sessionId: String = ""
) {
    val judul get() = "($kodeMatkul) - $namaMatkul"
}

data class Pertemuan(
    val nomor: Int,
    val hadir: Int,
    val total: Int,
    val tanggal: String,
    val waktu: String,
    val sessionId: String = ""
)

data class Peserta(
    val nama: String,
    val nim: String,
    val uid: String = ""
)

/** Untuk Pantau Sesi. jam = jam absen (null kalau belum absen). */
data class KehadiranMahasiswa(
    val nama: String,
    val nim: String,
    val status: AttendanceStatus = AttendanceStatus.BelumAbsen,
    val jam: String? = null
)

/** Untuk Rekap Kehadiran. manual = true kalau status diubah dosen; false = hasil verifikasi otomatis. */
data class CatatanKehadiran(
    val nama: String,
    val nim: String,
    val status: AttendanceStatus,
    val manual: Boolean = false,
    val uid: String = ""
)
