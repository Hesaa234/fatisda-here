package com.fatisdahere.app.ui.mahasiswa

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.sensing.hasLocationPermission
import com.fatisdahere.app.sensing.wifiEnvironmentProblem
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/** Tahap proses absen yang sedang berjalan, diatur oleh MahasiswaApp. */
sealed interface ProsesAbsen {
    object Siap : ProsesAbsen
    data class Memindai(val selesai: Int, val total: Int) : ProsesAbsen
    object Memproses : ProsesAbsen
}

/** Isian tambahan untuk Sakit/Izin. fotoBase64 = foto bukti yang sudah dikompres (lihat BuktiFoto). */
data class KeteranganIzin(val catatan: String, val fotoBase64: String)

/** Batas panjang keterangan Sakit/Izin. */
private const val CATATAN_MAKS = 300
private const val CATATAN_MIN = 5

/**
 * Halaman absen. Keterangan Hadir diverifikasi lewat scan Wi-Fi + engine KNN (dijalankan MahasiswaApp);
 * Sakit/Izin tanpa scan, tapi wajib mengisi keterangan dan melampirkan foto bukti.
 *
 * Halaman ini hanya memastikan izin lokasi, Wi-Fi, dan lokasi HP siap (Hadir) atau isian lengkap
 * (Sakit/Izin) sebelum memanggil [onKirim].
 *
 * @param proses tahap yang sedang berjalan, untuk menampilkan progres scan
 * @param onKirim dipanggil saat mahasiswa menekan tombol kirim, membawa keterangan yang dipilih;
 *   untuk Sakit/Izin juga membawa catatan dan foto bukti
 */
@Composable
fun ScanningScreen(
    userName: String,
    session: SessionUi,
    wifiName: String,
    proses: ProsesAbsen,
    onKirim: (AttendanceStatus, KeteranganIzin?) -> Unit,
    onProfileClick: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedStatus by remember { mutableStateOf<AttendanceStatus?>(null) }
    var galat by remember { mutableStateOf<String?>(null) }
    val sibuk = proses != ProsesAbsen.Siap

    // Isian Sakit/Izin
    var catatan by remember { mutableStateOf("") }
    var fotoBase64 by remember { mutableStateOf<String?>(null) }
    var fotoPratinjau by remember { mutableStateOf<ImageBitmap?>(null) }
    var memprosesFoto by remember { mutableStateOf(false) }
    var galatFoto by remember { mutableStateOf<String?>(null) }
    var konfirmasi by remember { mutableStateOf(false) }

    val perluBukti = selectedStatus == AttendanceStatus.Sakit || selectedStatus == AttendanceStatus.Izin
    val catatanCukup = catatan.trim().length >= CATATAN_MIN
    val isianLengkap = !perluBukti || (catatanCukup && fotoBase64 != null)

    /** Hadir perlu Wi-Fi dan lokasi aktif; kalau belum, tampilkan pesan dan jangan kirim. */
    fun kirimJikaSiap(status: AttendanceStatus) {
        if (status == AttendanceStatus.Hadir) {
            galat = wifiEnvironmentProblem(context)
            if (galat != null) return
            galat = null
            onKirim(status, null)
            return
        }
        val foto = fotoBase64 ?: return
        galat = null
        onKirim(status, KeteranganIzin(catatan.trim(), foto))
    }

    /** Foto dari galeri dikompres di latar belakang supaya layar tidak macet. */
    fun prosesFoto(uri: Uri) {
        scope.launch {
            memprosesFoto = true
            galatFoto = null
            try {
                val teks = withContext(Dispatchers.IO) { BuktiFoto.kompres(context, uri) }
                val pratinjau = withContext(Dispatchers.Default) { BuktiFoto.keBitmap(teks) }
                fotoBase64 = teks
                fotoPratinjau = pratinjau?.asImageBitmap()
            } catch (e: CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                galatFoto = "Foto terlalu besar untuk diproses. Coba foto lain."
            } catch (e: Exception) {
                galatFoto = e.message ?: "Foto tidak bisa diproses."
            } finally {
                memprosesFoto = false
            }
        }
    }

    val pilihFoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) prosesFoto(uri)
    }

    val izinLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { diberi ->
        if (diberi) {
            kirimJikaSiap(AttendanceStatus.Hadir)
        } else {
            galat = "Izin lokasi diperlukan untuk memindai Wi-Fi ruangan. Izin ini hanya dipakai saat absen."
        }
    }

    Scaffold(
        topBar = { FatisdaTopBar(userName, onProfileClick, onBack) },
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
            SectionCard("Absensi") {
                LiveBadge()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(session.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = UnsNavy)
                    Spacer(Modifier.height(4.dp))
                    InfoRow("Waktu", session.time)
                    InfoRow("Ruang", session.room)
                }
                Text(
                    "Keterangan: ${selectedStatus?.label ?: "Belum dipilih"}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(AttendanceStatus.Hadir, AttendanceStatus.Sakit, AttendanceStatus.Izin).forEach { status ->
                        PilihanKeterangan(
                            label = status.label,
                            selected = selectedStatus == status,
                            enabled = !sibuk,
                            onClick = {
                                selectedStatus = status
                                galat = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (selectedStatus == AttendanceStatus.Hadir) {
                    HelperText("Pastikan kamu sudah di dalam ruangan ${session.room}, lalu diam di tempat selama scan.")
                }
                if (perluBukti) {
                    IsianIzin(
                        catatan = catatan,
                        onCatatanChange = { catatan = it.take(CATATAN_MAKS) },
                        catatanCukup = catatanCukup,
                        fotoBase64 = fotoBase64,
                        fotoPratinjau = fotoPratinjau,
                        memprosesFoto = memprosesFoto,
                        galatFoto = galatFoto,
                        enabled = !sibuk,
                        onPilihFoto = { pilihFoto.launch("image/*") }
                    )
                }
                PrimaryButton(
                    if (perluBukti) "Kirim Keterangan" else "Verifikasi Kehadiran",
                    onClick = {
                        val status = selectedStatus ?: return@PrimaryButton
                        when {
                            perluBukti -> konfirmasi = true
                            !hasLocationPermission(context) -> izinLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            else -> kirimJikaSiap(status)
                        }
                    },
                    enabled = selectedStatus != null && isianLengkap && !sibuk && !memprosesFoto
                )
                galat?.let { ErrorText(it) }
            }

            when (proses) {
                is ProsesAbsen.Memindai -> ProgresScan(wifiName, proses)
                ProsesAbsen.Memproses -> ProgresMemproses()
                ProsesAbsen.Siap -> Unit
            }
        }
    }

    val status = selectedStatus
    if (konfirmasi && status != null) {
        ConfirmDialog(
            title = "Kirim keterangan ${status.label}?",
            text = "Keterangan dan foto bukti akan dilihat dosen. Setelah dikirim, keterangan tidak bisa diubah.",
            confirmText = "Kirim",
            danger = false,
            onConfirm = {
                konfirmasi = false
                kirimJikaSiap(status)
            },
            onDismiss = { konfirmasi = false }
        )
    }
}

/** Isian keterangan + foto bukti untuk Sakit/Izin. */
@Composable
private fun IsianIzin(
    catatan: String,
    onCatatanChange: (String) -> Unit,
    catatanCukup: Boolean,
    fotoBase64: String?,
    fotoPratinjau: ImageBitmap?,
    memprosesFoto: Boolean,
    galatFoto: String?,
    enabled: Boolean,
    onPilihFoto: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel("Keterangan")
        OutlinedTextField(
            value = catatan,
            onValueChange = onCatatanChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
            enabled = enabled,
            placeholder = { Text("contoh: Demam sejak semalam, sudah ke dokter") },
            singleLine = false,
            maxLines = 5,
            shape = RoundedCornerShape(12.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (catatan.isNotEmpty() && !catatanCukup) {
                ErrorText("Minimal $CATATAN_MIN karakter.")
            } else {
                Spacer(Modifier.width(1.dp))
            }
            HelperText("${catatan.length}/$CATATAN_MAKS")
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel("Foto Bukti")
        if (fotoPratinjau != null) {
            Image(
                bitmap = fotoPratinjau,
                contentDescription = "Pratinjau foto bukti",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            fotoBase64?.let { HelperText("Foto siap dikirim (±${BuktiFoto.ukuranTeks(it)} setelah dikompres).") }
        }
        SecondaryButton(
            when {
                memprosesFoto -> "Memproses foto..."
                fotoBase64 != null -> "Ganti Foto"
                else -> "Pilih Foto dari Galeri"
            },
            onClick = onPilihFoto,
            enabled = enabled && !memprosesFoto
        )
        galatFoto?.let { ErrorText(it) }
        HelperText("Contoh bukti: surat dokter, surat izin, atau undangan kegiatan. Satu foto saja.")
    }
}

@Composable
private fun ProgresScan(wifiName: String, proses: ProsesAbsen.Memindai) {
    SectionCard {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Scanning Wi-Fi ($wifiName)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = UnsNavy,
                textAlign = TextAlign.Center
            )
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                CircularProgressIndicator(
                    progress = proses.selesai.toFloat() / proses.total,
                    modifier = Modifier.fillMaxSize(),
                    color = UnsBlue,
                    trackColor = PageBackground,
                    strokeWidth = 12.dp
                )
                Text(
                    "${proses.selesai} dari ${proses.total}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = UnsNavy
                )
            }
            Text(
                "Tetap diam di dalam ruangan. Proses ini bisa memakan waktu hingga 30 detik.",
                fontSize = 14.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Tanpa CircularProgressIndicator berputar: crash di proyek ini (lihat catatan di LoadingContent). */
@Composable
private fun ProgresMemproses() {
    SectionCard {
        Text(
            "Memproses absen...",
            Modifier.fillMaxWidth(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = UnsBlue,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PilihanKeterangan(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) UnsBlue else SurfaceWhite,
        border = BorderStroke(1.dp, if (selected) UnsBlue else TextPrimary.copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) Color.White else TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanningScreenPreview() {
    FatisdaTheme { ScanningScreen("Andi", sampleSession, "UNS SOLO", ProsesAbsen.Memindai(1, 3), { _, _ -> }) }
}
