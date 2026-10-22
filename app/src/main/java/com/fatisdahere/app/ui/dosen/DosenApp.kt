package com.fatisdahere.app.ui.dosen

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

private enum class LayarDosen {
    BERANDA, PANTAU_SESI, TAMBAH_MATKUL, DETAIL_MATKUL, BUKA_SESI,
    KELOLA_PESERTA, REKAP_KEHADIRAN, EDIT_MATKUL
}

/**
 * Alur dosen:
 * Beranda -> Tambah Mata Kuliah
 * Beranda -> Detail Matkul -> Buka Sesi Absen -> Beranda (sesi aktif muncul)
 *                          -> Kelola Peserta
 *                          -> Rekap Kehadiran
 *                          -> Edit Matkul
 * Beranda -> Pantau Sesi -> (Tutup sesi) -> Beranda
 *
 * Panggil dari MainActivity / NavHost: DosenApp()
 * Data di bawah masih dummy; ganti dengan data dari ViewModel/API.
 */
@Composable
fun DosenApp(username: String = "Username") {
    var layar by rememberSaveable { mutableStateOf(LayarDosen.BERANDA) }
    var kodeTerpilih by rememberSaveable { mutableStateOf<String?>(null) }
    var sesiBerjalan by remember { mutableStateOf<SesiAktif?>(null) }
    val kehadiran = remember { dummyKehadiran }
    val daftarMatkul = remember { mutableStateListOf<MataKuliah>().apply { addAll(dummyMatkul) } }

    // Peserta per matkul (key = kode matkul)
    val pesertaMap = remember {
        mutableStateMapOf<String, List<Peserta>>().apply {
            dummyMatkul.forEach { put(it.kode, dummyMahasiswa.take(it.jumlahPeserta)) }
        }
    }
    // Perubahan status rekap yang sudah diedit dosen (key = "kode#nomorPertemuan")
    val rekapMap = remember { mutableStateMapOf<String, List<CatatanKehadiran>>() }

    fun setPeserta(kode: String, baru: List<Peserta>) {
        pesertaMap[kode] = baru
        val idx = daftarMatkul.indexOfFirst { it.kode == kode }
        if (idx >= 0) daftarMatkul[idx] = daftarMatkul[idx].copy(jumlahPeserta = baru.size)
    }

    // Tombol back sistem: halaman di bawah Detail Matkul -> Detail Matkul, lainnya -> Beranda
    BackHandler(enabled = layar != LayarDosen.BERANDA) {
        layar = when (layar) {
            LayarDosen.BUKA_SESI,
            LayarDosen.KELOLA_PESERTA,
            LayarDosen.REKAP_KEHADIRAN,
            LayarDosen.EDIT_MATKUL -> LayarDosen.DETAIL_MATKUL
            else -> LayarDosen.BERANDA
        }
    }

    val sesi = sesiBerjalan
    val matkul = daftarMatkul.firstOrNull { it.kode == kodeTerpilih }
    val pesertaMatkul: List<Peserta> = matkul?.let { pesertaMap[it.kode] } ?: emptyList()

    when {
        layar == LayarDosen.PANTAU_SESI && sesi != null -> PantauSesi(
            username = username,
            sesi = sesi,
            daftarKehadiran = kehadiran,
            onTutupSesi = {
                sesiBerjalan = null
                layar = LayarDosen.BERANDA
            }
        )

        layar == LayarDosen.TAMBAH_MATKUL -> TambahMatkul(
            username = username,
            daftarRuangan = dummyRuanganFingerprint,
            kodeTerpakai = daftarMatkul.map { it.kode },
            onSimpan = { kode, nama, ruangan ->
                daftarMatkul.add(
                    MataKuliah(kode = kode, nama = nama, ruang = ruangan, jumlahPeserta = 0)
                )
                layar = LayarDosen.BERANDA
            }
        )

        layar == LayarDosen.DETAIL_MATKUL && matkul != null -> DetailMatkul(
            username = username,
            matkul = matkul,
            riwayat = dummyRiwayat,
            sesiBerlangsung = sesi != null,
            onBukaSesi = { layar = LayarDosen.BUKA_SESI },
            onKelolaPeserta = { layar = LayarDosen.KELOLA_PESERTA },
            onEditMatkul = { layar = LayarDosen.EDIT_MATKUL },
            onRekap = { layar = LayarDosen.REKAP_KEHADIRAN }
        )

        layar == LayarDosen.EDIT_MATKUL && matkul != null -> TambahMatkul(
            username = username,
            judul = "Edit Mata Kuliah",
            kodeAwal = matkul.kode,
            namaAwal = matkul.nama,
            ruanganAwal = matkul.ruang,
            daftarRuangan = dummyRuanganFingerprint,
            kodeTerpakai = daftarMatkul.map { it.kode }.filter { it != matkul.kode },
            onSimpan = { kode, nama, ruangan ->
                val lama = matkul.kode
                val idx = daftarMatkul.indexOfFirst { it.kode == lama }
                if (idx >= 0) {
                    daftarMatkul[idx] = matkul.copy(kode = kode, nama = nama, ruang = ruangan)
                }
                // Kode berubah: pindahkan data peserta & rekap ke kode baru
                if (kode != lama) {
                    pesertaMap.remove(lama)?.let { pesertaMap[kode] = it }
                    rekapMap.keys.filter { it.startsWith("$lama#") }.forEach { k ->
                        rekapMap.remove(k)?.let { rekapMap["$kode#" + k.substringAfter('#')] = it }
                    }
                    kodeTerpilih = kode
                }
                // Sesi yang sedang berjalan untuk matkul ini ikut diperbarui
                sesiBerjalan = sesiBerjalan?.let {
                    if (it.kodeMatkul == lama) {
                        it.copy(kodeMatkul = kode, namaMatkul = nama, ruang = ruangan)
                    } else it
                }
                layar = LayarDosen.DETAIL_MATKUL
            }
        )

        layar == LayarDosen.BUKA_SESI && matkul != null -> BukaSesi(
            username = username,
            daftarMatkul = daftarMatkul,
            matkulAwal = matkul,
            onBukaAbsensi = { m, jamMulai, jamSelesai ->
                sesiBerjalan = SesiAktif(
                    kodeMatkul = m.kode,
                    namaMatkul = m.nama,
                    waktu = "$jamMulai - $jamSelesai",
                    ruang = m.ruang,
                    hadir = 0,
                    totalMahasiswa = m.jumlahPeserta
                )
                layar = LayarDosen.BERANDA
            }
        )

        layar == LayarDosen.KELOLA_PESERTA && matkul != null -> KelolaPeserta(
            username = username,
            matkul = matkul,
            peserta = pesertaMatkul,
            daftarMahasiswa = dummyMahasiswa,
            onTambah = { setPeserta(matkul.kode, pesertaMatkul + it) },
            onHapus = { m -> setPeserta(matkul.kode, pesertaMatkul.filter { it.nim != m.nim }) }
        )

        layar == LayarDosen.REKAP_KEHADIRAN && matkul != null -> RekapKehadiran(
            username = username,
            matkul = matkul,
            riwayat = dummyRiwayat,
            catatanUntuk = { p ->
                rekapMap["${matkul.kode}#${p.nomor}"] ?: buatCatatan(pesertaMatkul, p)
            },
            onUbahStatus = { p, nim, status ->
                val kunci = "${matkul.kode}#${p.nomor}"
                val dasar = rekapMap[kunci] ?: buatCatatan(pesertaMatkul, p)
                rekapMap[kunci] = dasar.map {
                    if (it.nim == nim) it.copy(status = status, manual = true) else it
                }
            }
        )

        else -> BerandaDosen(
            username = username,
            sesiAktif = sesi?.copy(
                hadir = kehadiran.count { it.jamAbsen != null },
                totalMahasiswa = kehadiran.size
            ),
            daftarMatkul = daftarMatkul,
            onPantauSesi = { layar = LayarDosen.PANTAU_SESI },
            onTambahMatkul = { layar = LayarDosen.TAMBAH_MATKUL },
            onMatkulClick = {
                kodeTerpilih = it.kode
                layar = LayarDosen.DETAIL_MATKUL
            }
        )
    }
}

/** Dummy: bentuk catatan awal sebuah pertemuan dari jumlah hadir di riwayat. */
private fun buatCatatan(peserta: List<Peserta>, p: Pertemuan): List<CatatanKehadiran> {
    val sisa = listOf(StatusHadir.Alpa, StatusHadir.Izin, StatusHadir.Sakit)
    return peserta.mapIndexed { i, m ->
        val status = if (i < p.hadir) StatusHadir.Hadir else sisa[(i - p.hadir) % sisa.size]
        CatatanKehadiran(m.nama, m.nim, status)
    }
}

// ---------- Data dummy ----------
private val dummyMatkul = listOf(
    MataKuliah("IF301", "Pemrograman Mobile", "Lab 3", 25),
    MataKuliah("IF302", "Kecerdasan Buatan", "R. 201", 30),
    MataKuliah("IF303", "Sistem Terdistribusi", "R. 202", 28)
)

// Ruangan yang sudah punya data fingerprint
private val dummyRuanganFingerprint = listOf("LAB 2 UPT", "LAB UPT LT 3", "B4.05")

private val dummyRiwayat = listOf(
    Pertemuan(8, 23, 25, "16 Okt 2026", "13:00 - 15:00"),
    Pertemuan(7, 22, 25, "9 Okt 2026", "13:00 - 15:00"),
    Pertemuan(6, 25, 25, "2 Okt 2026", "13:00 - 15:00")
)

private val dummyKehadiran = List(25) { i ->
    KehadiranMahasiswa(
        nama = "Mahasiswa ${i + 1}",
        nim = "M0723%03d".format(i + 1),
        jamAbsen = if (i < 18) "08:%02d".format(i + 1) else null
    )
}

private val dummyNama = listOf(
    "Andi Pratama", "Budi Santoso", "Citra Lestari", "Dewi Anggraini", "Eko Prasetyo",
    "Fitri Handayani", "Gilang Ramadhan", "Hana Safitri", "Irfan Maulana", "Jihan Aulia",
    "Kevin Wijaya", "Laila Nurhaliza", "Muhammad Rizky", "Nadia Putri", "Oki Setiawan",
    "Putri Maharani", "Qori Rahman", "Rina Kusuma", "Satria Nugraha", "Tiara Azzahra",
    "Umar Faruq", "Vina Oktaviani", "Wahyu Hidayat", "Xavier Pranata", "Yusuf Alamsyah",
    "Zahra Salsabila", "Agus Salim", "Bella Safira", "Candra Dirgantara", "Dimas Anggara",
    "Elsa Marlina", "Farhan Hakim", "Galih Permana", "Hesti Rahayu", "Ilham Saputra",
    "Jasmine Putri", "Kurnia Dewi", "Lutfi Ananda", "Mega Wulandari", "Naufal Ghifari"
)

// Semua mahasiswa yang bisa ditambahkan sebagai peserta
private val dummyMahasiswa = dummyNama.mapIndexed { i, nama ->
    Peserta(nama, "L2002200%02d".format(i + 1))
}