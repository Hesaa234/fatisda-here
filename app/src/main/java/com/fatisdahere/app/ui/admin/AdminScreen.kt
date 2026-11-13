package com.fatisdahere.app.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.data.NewAccount
import com.fatisdahere.app.data.Role
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.theme.*

/*
 * Layar-layar halaman admin. Alur dan datanya diatur di AdminApp.kt.
 */

// ---------- Model tampilan ----------

data class AkunUi(
    val uid: String,
    val nama: String,
    val email: String,
    val role: Role,
    val nomorInduk: String? // NIM untuk mahasiswa, NIP untuk dosen
)

data class MatkulAdminUi(
    val id: String,
    val kode: String,
    val nama: String,
    val dosen: String,
    val ruang: String,
    val jumlahPeserta: Int
) {
    val judul get() = "($kode) - $nama"
}

data class RuanganUi(val id: String, val nama: String, val lokasi: String, val apUtama: String)

fun labelRole(role: Role) = when (role) {
    Role.MAHASISWA -> "Mahasiswa"
    Role.DOSEN -> "Dosen"
    Role.ADMIN -> "Admin"
}

private fun warnaRole(role: Role) = when (role) {
    Role.MAHASISWA -> UnsBlue
    Role.DOSEN -> StatusSuccess
    Role.ADMIN -> UnsNavy
}

// ---------- Beranda admin ----------

@Composable
fun BerandaAdmin(
    username: String,
    jumlahAkun: Int,
    jumlahMatkul: Int,
    jumlahRuangan: Int,
    onAkun: () -> Unit,
    onMatkul: () -> Unit,
    onRuangan: () -> Unit,
    onProfileClick: () -> Unit,
    onTesKnn: () -> Unit = {}
) {
    Scaffold(
        topBar = { FatisdaTopBar(username, onProfileClick) },
        containerColor = PageBackground
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SectionTitle("Panel Admin") }
            item { MenuCard("Akun", "$jumlahAkun akun mahasiswa, dosen, dan admin", onAkun) }
            item { MenuCard("Mata Kuliah", "$jumlahMatkul mata kuliah · atur dosen dan peserta", onMatkul) }
            item { MenuCard("Ruangan", "$jumlahRuangan ruangan dengan data fingerprint", onRuangan) }
            item { MenuCard("Tes KNN & Kalibrasi", "Uji posisi di tiap ruangan, setel batas, rekap akurasi", onTesKnn) }
        }
    }
}

@Composable
private fun MenuCard(judul: String, keterangan: String, onClick: () -> Unit) {
    ItemCard(onClick = onClick) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(judul, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = UnsNavy)
            ItemSubtitle(keterangan)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = UnsBlue)
    }
}

// ---------- Akun ----------

private val FilterRole = listOf<Role?>(null, Role.MAHASISWA, Role.DOSEN, Role.ADMIN)

@Composable
fun DaftarAkun(
    username: String,
    akun: List<AkunUi>,
    onTambah: () -> Unit,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    pesan: String? = null,
    onAkunClick: (AkunUi) -> Unit = {}
) {
    var filter by remember { mutableStateOf<Role?>(null) }
    var cari by remember { mutableStateOf("") }
    val q = cari.trim()
    val tampil = akun.filter { a ->
        (filter == null || a.role == filter) &&
            (q.isEmpty() || a.nama.contains(q, true) || a.email.contains(q, true) || a.nomorInduk?.contains(q, true) == true)
    }

    Scaffold(
        topBar = { FatisdaTopBar(username, onProfileClick, onBack) },
        containerColor = PageBackground
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionCard("Akun") {
                    PrimaryButton("+ Tambah Akun", onTambah)
                    if (pesan != null) HelperText(pesan, color = StatusSuccess)
                    AppTextField(cari, { cari = it }, "Cari nama, email, NIM/NIP")
                    SelectField(
                        value = filter?.let { "Peran: ${labelRole(it)}" } ?: "Peran: Semua",
                        placeholder = "Peran",
                        options = FilterRole.map { it?.let(::labelRole) ?: "Semua" },
                        onSelect = { filter = FilterRole[it] }
                    )
                    HelperText("${tampil.size} akun · tekan akun untuk mengubah data atau reset kata sandi")
                }
            }
            if (tampil.isEmpty()) item { HelperText("Tidak ada akun yang cocok.") }
            items(tampil) { a ->
                ItemCard(onClick = { onAkunClick(a) }) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ItemTitle(a.nama)
                        ItemSubtitle(a.email)
                        if (a.nomorInduk != null) ItemSubtitle(a.nomorInduk)
                    }
                    Spacer(Modifier.width(12.dp))
                    StatusPill(labelRole(a.role), warnaRole(a.role))
                }
            }
        }
    }
}

private val PilihanRole = listOf(Role.MAHASISWA, Role.DOSEN, Role.ADMIN)

@Composable
fun FormAkun(
    username: String,
    isLoading: Boolean,
    errorMessage: String?,
    onSimpan: (NewAccount) -> Unit,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    nimTerpakai: List<String> = emptyList(),
    nipTerpakai: List<String> = emptyList()
) {
    var nama by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var sandi by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(Role.MAHASISWA) }
    var nomor by remember { mutableStateOf("") }

    val labelNomor = when (role) {
        Role.MAHASISWA -> "NIM"
        Role.DOSEN -> "NIP"
        Role.ADMIN -> null
    }

    val emailValid = POLA_EMAIL.matches(email.trim())

    val nomorTrim = nomor.trim()
    val nomorDipakai = when (role) {
        Role.MAHASISWA -> nimTerpakai.any { it.equals(nomorTrim, ignoreCase = true) }
        Role.DOSEN -> nipTerpakai.any { it.equals(nomorTrim, ignoreCase = true) }
        Role.ADMIN -> false
    }

    val nomorValid = when (role) {
        Role.MAHASISWA, Role.DOSEN -> nomorTrim.isNotBlank() && !nomorDipakai
        Role.ADMIN -> true
    }

    val bisaSimpan = !isLoading && nama.isNotBlank() && emailValid && sandi.length >= 6 && nomorValid

    Scaffold(
        topBar = { FatisdaTopBar(username, onProfileClick, onBack) },
        containerColor = PageBackground
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionCard("Tambah Akun") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Peran")
                    SelectField(
                        value = labelRole(role),
                        placeholder = "Pilih peran",
                        options = PilihanRole.map(::labelRole),
                        onSelect = {
                            val peranBaru = PilihanRole[it]
                            if (role != peranBaru) {
                                role = peranBaru
                                nomor = ""
                            }
                        }
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Nama Lengkap")
                    AppTextField(nama, { nama = it }, "contoh: Andi Pratama")
                }
                if (labelNomor != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FieldLabel(labelNomor)
                        AppTextField(
                            nomor,
                            { nomor = it },
                            if (role == Role.MAHASISWA) "contoh: M0523001" else "contoh: 198001012005011001"
                        )
                        if (nomorDipakai) ErrorText("$labelNomor sudah terdaftar.")
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Email")
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("nama@student.uns.ac.id") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    if (email.isNotBlank() && !emailValid) ErrorText("Format email tidak valid, contoh: nama@student.uns.ac.id")
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Kata Sandi Awal")
                    OutlinedTextField(
                        value = sandi,
                        onValueChange = { sandi = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("minimal 6 karakter") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
                if (errorMessage != null) ErrorText(errorMessage)
                HelperText("Berikan email dan kata sandi awal ini ke pemilik akun.")
                PrimaryButton(
                    if (isLoading) "Menyimpan..." else "Simpan Akun",
                    onClick = {
                        onSimpan(
                            NewAccount(
                                name = nama.trim(),
                                email = email.trim(),
                                password = sandi,
                                role = role,
                                nim = nomorTrim.takeIf { role == Role.MAHASISWA },
                                nip = nomorTrim.takeIf { role == Role.DOSEN }
                            )
                        )
                    },
                    enabled = bisaSimpan
                )
            }
        }
    }
}

/** Format email sederhana: ada teks@teks.domain tanpa spasi. */
private val POLA_EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

/**
 * Edit akun: nama dan NIM/NIP bisa diubah; email dan peran tidak (email dipakai untuk login di
 * Firebase Authentication). Kata sandi tidak bisa dilihat/diubah admin, tapi admin bisa
 * mengirim email reset kata sandi ke pemilik akun.
 */
@Composable
fun EditAkun(
    username: String,
    akun: AkunUi,
    nomorTerpakai: List<String>,
    isLoading: Boolean,
    errorMessage: String?,
    pesan: String?,
    onSimpan: (nama: String, nomor: String?) -> Unit,
    onKirimResetSandi: () -> Unit,
    onBack: () -> Unit,
    onProfileClick: () -> Unit
) {
    var nama by remember(akun.uid) { mutableStateOf(akun.nama) }
    var nomor by remember(akun.uid) { mutableStateOf(akun.nomorInduk.orEmpty()) }
    var konfirmasiReset by remember { mutableStateOf(false) }

    val labelNomor = when (akun.role) {
        Role.MAHASISWA -> "NIM"
        Role.DOSEN -> "NIP"
        Role.ADMIN -> null
    }
    val nomorTrim = nomor.trim()
    val nomorDipakai = labelNomor != null &&
        !nomorTrim.equals(akun.nomorInduk.orEmpty(), ignoreCase = true) &&
        nomorTerpakai.any { it.equals(nomorTrim, ignoreCase = true) }
    val nomorValid = labelNomor == null || (nomorTrim.isNotBlank() && !nomorDipakai)
    val berubah = nama.trim() != akun.nama || (labelNomor != null && nomorTrim != akun.nomorInduk.orEmpty())
    val bisaSimpan = !isLoading && nama.isNotBlank() && nomorValid && berubah

    Scaffold(
        topBar = { FatisdaTopBar(username, onProfileClick, onBack) },
        containerColor = PageBackground
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard("Edit Akun") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Email (untuk login)")
                        Text(akun.email, fontSize = 15.sp, color = TextPrimary)
                    }
                    StatusPill(labelRole(akun.role), warnaRole(akun.role))
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Nama Lengkap")
                    AppTextField(nama, { nama = it }, "contoh: Andi Pratama")
                }
                if (labelNomor != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FieldLabel(labelNomor)
                        AppTextField(nomor, { nomor = it }, if (akun.role == Role.MAHASISWA) "contoh: M0523001" else "contoh: 198001012005011001")
                        if (nomorDipakai) ErrorText("$labelNomor sudah dipakai akun lain.")
                        if (nomorTrim.isBlank()) ErrorText("$labelNomor wajib diisi.")
                    }
                }
                if (errorMessage != null) ErrorText(errorMessage)
                if (pesan != null) HelperText(pesan, color = StatusSuccess)
                PrimaryButton(
                    if (isLoading) "Menyimpan..." else "Simpan Perubahan",
                    onClick = { onSimpan(nama.trim(), nomorTrim.takeIf { labelNomor != null }) },
                    enabled = bisaSimpan
                )
            }
            SectionCard("Kata Sandi") {
                HelperText(
                    "Admin tidak bisa melihat kata sandi. Kalau pemilik akun lupa, kirim email reset: " +
                        "pemilik akun membuat kata sandi baru lewat tautan di email ${akun.email}."
                )
                SecondaryButton("Kirim Email Reset Kata Sandi", { konfirmasiReset = true }, enabled = !isLoading)
            }
        }
    }

    if (konfirmasiReset) {
        ConfirmDialog(
            title = "Kirim email reset?",
            text = "Tautan untuk membuat kata sandi baru dikirim ke ${akun.email}.",
            confirmText = "Kirim",
            danger = false,
            onConfirm = {
                konfirmasiReset = false
                onKirimResetSandi()
            },
            onDismiss = { konfirmasiReset = false }
        )
    }
}

// ---------- Mata kuliah ----------

@Composable
fun DaftarMatkulAdmin(
    username: String,
    matkul: List<MatkulAdminUi>,
    onTambah: () -> Unit,
    onMatkulClick: (MatkulAdminUi) -> Unit,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    pesan: String? = null
) {
    Scaffold(
        topBar = { FatisdaTopBar(username, onProfileClick, onBack) },
        containerColor = PageBackground
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionCard("Mata Kuliah") {
                    PrimaryButton("+ Tambah Mata Kuliah", onTambah)
                    if (pesan != null) HelperText(pesan, color = StatusSuccess)
                    HelperText("Pilih mata kuliah untuk mengubah dosen, ruangan, atau peserta.")
                }
            }
            if (matkul.isEmpty()) item { HelperText("Belum ada mata kuliah.") }
            items(matkul) { m ->
                ItemCard(onClick = { onMatkulClick(m) }) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ItemTitle(m.judul)
                        ItemSubtitle(m.dosen)
                        ItemSubtitle("${m.jumlahPeserta} peserta")
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(m.ruang, fontSize = 13.sp, color = UnsBlue, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
                }
            }
        }
    }
}

/**
 * Form tambah/edit mata kuliah oleh admin (bisa memilih dosen pengampu).
 * onKelolaPeserta null = mode tambah (tombol peserta disembunyikan).
 */
@Composable
fun FormMatkulAdmin(
    username: String,
    judul: String,
    daftarDosen: List<Pair<String, String>>, // uid, nama
    daftarRuangan: List<Pair<String, String>>, // id, nama
    kodeTerpakai: List<String>,
    onSimpan: (kode: String, nama: String, dosenId: String, ruanganId: String) -> Unit,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    kodeAwal: String = "",
    namaAwal: String = "",
    dosenAwal: String? = null,
    ruanganAwal: String? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onKelolaPeserta: (() -> Unit)? = null,
    onHapus: (() -> Unit)? = null // null = mode tambah (tombol hapus disembunyikan)
) {
    var konfirmasiHapus by remember { mutableStateOf(false) }
    var kode by remember { mutableStateOf(kodeAwal) }
    var nama by remember { mutableStateOf(namaAwal) }
    var dosenId by remember { mutableStateOf(dosenAwal) }
    var ruanganId by remember { mutableStateOf(ruanganAwal) }

    val kodeDipakai = kodeTerpakai.any { it.equals(kode.trim(), ignoreCase = true) }
    val bisaSimpan = !isLoading && kode.isNotBlank() && nama.isNotBlank() && dosenId != null && ruanganId != null && !kodeDipakai

    Scaffold(
        topBar = { FatisdaTopBar(username, onProfileClick, onBack) },
        containerColor = PageBackground
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(judul) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Kode Mata Kuliah")
                    AppTextField(kode, { kode = it }, "contoh: WMC-A")
                    if (kodeDipakai) ErrorText("Kode mata kuliah sudah dipakai.")
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Nama Mata Kuliah")
                    AppTextField(nama, { nama = it }, "contoh: Wireless & Mobile Computing")
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Dosen Pengampu")
                    SelectField(
                        value = daftarDosen.firstOrNull { it.first == dosenId }?.second,
                        placeholder = "Pilih dosen",
                        options = daftarDosen.map { it.second },
                        onSelect = { dosenId = daftarDosen[it].first },
                        emptyText = "Belum ada akun dosen"
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Ruangan")
                    SelectField(
                        value = daftarRuangan.firstOrNull { it.first == ruanganId }?.second,
                        placeholder = "Pilih ruangan",
                        options = daftarRuangan.map { it.second },
                        onSelect = { ruanganId = daftarRuangan[it].first },
                        emptyText = "Belum ada data ruangan"
                    )
                }
                if (errorMessage != null) ErrorText(errorMessage)
                PrimaryButton(
                    if (isLoading) "Menyimpan..." else "Simpan",
                    onClick = {
                        val d = dosenId
                        val r = ruanganId
                        if (d != null && r != null) onSimpan(kode.trim(), nama.trim(), d, r)
                    },
                    enabled = bisaSimpan
                )
            }
            if (onKelolaPeserta != null) {
                SecondaryButton("Kelola Peserta", onKelolaPeserta)
            }
            if (onHapus != null) {
                DangerButton("Hapus Mata Kuliah", { konfirmasiHapus = true })
            }
        }
    }
    if (konfirmasiHapus && onHapus != null) {
        ConfirmDialog(
            title = "Hapus mata kuliah ini?",
            text = "Mata kuliah $namaAwal beserta SEMUA pertemuan, data kehadiran, dan foto bukti izin/sakit " +
                "akan dihapus permanen dan tidak bisa dikembalikan.",
            confirmText = "Hapus",
            onConfirm = {
                konfirmasiHapus = false
                onHapus()
            },
            onDismiss = { konfirmasiHapus = false }
        )
    }
}

// ---------- Ruangan ----------

@Composable
fun DaftarRuangan(
    username: String,
    ruangan: List<RuanganUi>,
    onBack: () -> Unit,
    onProfileClick: () -> Unit
) {
    Scaffold(
        topBar = { FatisdaTopBar(username, onProfileClick, onBack) },
        containerColor = PageBackground
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionCard("Ruangan") {
                    HelperText(
                        "Ruangan dibuat dari hasil kalibrasi fingerprint, jadi tidak ditambah dari aplikasi. " +
                            "Data diubah lewat Firebase Console (koleksi rooms)."
                    )
                }
            }
            if (ruangan.isEmpty()) item { HelperText("Belum ada data ruangan.") }
            items(ruangan) { r ->
                ItemCard {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ItemTitle(r.nama)
                        ItemSubtitle(r.lokasi)
                        ItemSubtitle("AP utama: ${r.apUtama}")
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(r.id, fontSize = 12.sp, color = TextPrimary.copy(alpha = 0.6f), textAlign = TextAlign.End)
                }
            }
        }
    }
}

// ---------- Preview ----------

@Preview(showBackground = true)
@Composable
private fun BerandaAdminPreview() {
    FatisdaTheme { BerandaAdmin("Admin", 12, 4, 3, {}, {}, {}, {}) }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun FormAkunPreview() {
    FatisdaTheme { FormAkun("Admin", false, null, {}, {}, {}) }
}
