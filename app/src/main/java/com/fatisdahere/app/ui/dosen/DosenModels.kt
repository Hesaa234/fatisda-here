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

/** terjadwal = sesi belum dimulai (dibuka dosen untuk jam nanti). */
data class Pertemuan(
    val nomor: Int,
    val hadir: Int,
    val total: Int,
    val tanggal: String,
    val waktu: String,
    val sessionId: String = "",
    val terjadwal: Boolean = false
)

/** Rekap satu mahasiswa untuk semua pertemuan yang sudah selesai (Rekap Kehadiran > Per Mahasiswa). */
data class RekapMahasiswa(
    val nama: String,
    val nim: String,
    val hadir: Int,
    val izin: Int,
    val sakit: Int,
    val alpa: Int
) {
    val total get() = hadir + izin + sakit + alpa
    val persenHadir get() = if (total == 0) 0 else Math.round(hadir * 100f / total)
}

data class Peserta(
    val nama: String,
    val nim: String,
    val uid: String = ""
)

/**
 * Untuk Pantau Sesi. jam = jam absen (null kalau belum absen).
 * keterangan/adaBukti = isian mahasiswa saat memilih Sakit/Izin.
 */
data class KehadiranMahasiswa(
    val nama: String,
    val nim: String,
    val status: AttendanceStatus = AttendanceStatus.BelumAbsen,
    val jam: String? = null,
    val uid: String = "",
    val keterangan: String? = null,
    val adaBukti: Boolean = false
)

/**
 * Untuk Rekap Kehadiran. manual = true kalau status diubah dosen atau Sakit/Izin dari mahasiswa;
 * false = hasil verifikasi otomatis. keterangan/adaBukti = isian mahasiswa saat Sakit/Izin.
 */
data class CatatanKehadiran(
    val nama: String,
    val nim: String,
    val status: AttendanceStatus,
    val manual: Boolean = false,
    val uid: String = "",
    val keterangan: String? = null,
    val adaBukti: Boolean = false
)
