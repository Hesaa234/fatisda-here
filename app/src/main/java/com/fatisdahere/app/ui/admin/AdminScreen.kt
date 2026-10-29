package com.fatisdahere.app.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
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
    onProfileClick: () -> Unit
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
        Text("›", fontSize = 28.sp, color = UnsBlue)
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
    pesan: String? = null
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
                    HelperText("${tampil.size} akun")
                }
            }
            if (tampil.isEmpty()) item { HelperText("Tidak ada akun yang cocok.") }
            items(tampil) { a ->
                ItemCard {
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
    onProfileClick: () -> Unit
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
    val bisaSimpan = !isLoading && nama.isNotBlank() && email.isNotBlank() && sandi.length >= 6

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
                        onSelect = { role = PilihanRole[it] }
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Nama Lengkap")
                    AppTextField(nama, { nama = it }, "contoh: Andi Pratama")
                }
                if (labelNomor != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FieldLabel(labelNomor)
                        AppTextField(nomor, { nomor = it }, if (role == Role.MAHASISWA) "contoh: M0523001" else "contoh: 198001012005011001")
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
                                name = nama,
                                email = email,
                                password = sandi,
                                role = role,
                                nim = nomor.takeIf { role == Role.MAHASISWA },
                                nip = nomor.takeIf { role == Role.DOSEN }
                            )
                        )
                    },
                    enabled = bisaSimpan
                )
            }
        }
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
                SectionCard("Mata Kuliah") {
                    PrimaryButton("+ Tambah Mata Kuliah", onTambah)
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
    onKelolaPeserta: (() -> Unit)? = null
) {
    var kode by remember { mutableStateOf(kodeAwal) }
    var nama by remember { mutableStateOf(namaAwal) }
    var dosenId by remember { mutableStateOf(dosenAwal) }
    var ruanganId by remember { mutableStateOf(ruanganAwal) }

    val kodeDipakai = kodeTerpakai.any { it.equals(kode.trim(), ignoreCase = true) }
    val bisaSimpan = kode.isNotBlank() && nama.isNotBlank() && dosenId != null && ruanganId != null && !kodeDipakai

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
                PrimaryButton(
                    "Simpan",
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
        }
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
