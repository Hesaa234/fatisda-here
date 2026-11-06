package com.fatisdahere.app.ui.admin

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.data.KalibrasiLokal
import com.fatisdahere.app.data.KalibrasiRepository
import com.fatisdahere.app.data.KnnTest
import com.fatisdahere.app.data.RawScan
import com.fatisdahere.app.data.TestPosition
import com.fatisdahere.app.positioning.FingerprintData
import com.fatisdahere.app.positioning.KnnPositioningEngine
import com.fatisdahere.app.positioning.MotionSummary
import com.fatisdahere.app.positioning.PositioningParams
import com.fatisdahere.app.positioning.ScanEvaluation
import com.fatisdahere.app.positioning.WifiReading
import com.fatisdahere.app.positioning.WifiScan
import com.fatisdahere.app.sensing.AndroidWifiScanner
import com.fatisdahere.app.sensing.MotionTracker
import com.fatisdahere.app.sensing.rekam
import com.fatisdahere.app.sensing.hasLocationPermission
import com.fatisdahere.app.sensing.wifiEnvironmentProblem
import com.fatisdahere.app.ui.components.*
import com.fatisdahere.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

/*
 * Menu admin "Tes KNN & Kalibrasi": pengganti aplikasi tes KNN terpisah.
 *
 * - Pilih ruangan target, berdiri di dalam (atau sengaja di luar), lalu Scan & Uji.
 * - Batas rssiMin / distanceMax bisa diubah langsung di HP ini (KalibrasiLokal). Hanya berlaku
 *   di layar ini. Kalau sudah pas, "Simpan untuk Semua" menulisnya ke Firestore config/positioning,
 *   yang dipakai absen mahasiswa di semua HP.
 * - Setiap tes disimpan ke Firestore (knnTests) beserta scan mentahnya, jadi rekap akurasi
 *   bisa dihitung ulang dengan batas apa pun tanpa scan ulang.
 */

/** Android membatasi aplikasi foreground: maks 4 scan per 2 menit (kalau throttling aktif). */
private const val BATAS_SCAN_ANDROID = 4
private const val JENDELA_THROTTLE_MS = 120_000L
private val PILIHAN_JUMLAH_SCAN = listOf(2, 3, 4)
private const val RIWAYAT_TAMPIL = 30

/**
 * Satu tes beserta evaluasi KNN tiap scan. Evaluasi dihitung sekali; batas diterapkan belakangan.
 * gerakan = hasil PDR selama scan (null untuk tes lama dari Firestore).
 */
private class TesDievaluasi(val test: KnnTest, val evals: List<ScanEvaluation>, val gerakan: MotionSummary? = null) {
    val jarak: List<Double> = evals.map { it.verdict.knnDistance }.filter { !it.isNaN() }
}

/** Lolos/tidaknya satu scan dengan batas tertentu (aturan sama dengan KnnPositioningEngine). */
private fun lolos(e: ScanEvaluation, rssiMin: Int, distanceMax: Double): Boolean {
    val terkuat = e.strongest ?: return false
    return e.fresh && e.enoughBssids && e.apDominant && terkuat.rssi >= rssiMin && e.roomMajority && e.verdict.knnDistance <= distanceMax
}

private fun diterima(t: TesDievaluasi, rssiMin: Int, distanceMax: Double, minValid: Int) =
    t.evals.count { lolos(it, rssiMin, distanceMax) } >= minValid

private class Rekap(
    val dalamTotal: Int,
    val dalamDiterima: Int,
    val luarTotal: Int,
    val luarDitolak: Int,
    val jarakDalam: List<Double>,
    val jarakLuar: List<Double>,
    /** Saran distanceMax (null kalau data dalam/luar belum cukup) dan skornya. */
    val saranJarak: Double?,
    val skorSaran: Double?,
)

private fun hitungRekap(tes: List<TesDievaluasi>, rssiMin: Int, distanceMax: Double, minValid: Int): Rekap {
    val dalam = tes.filter { it.test.position == TestPosition.DALAM }
    val luar = tes.filter { it.test.position == TestPosition.LUAR }

    // Saran: distanceMax yang paling seimbang antara "dalam diterima" dan "luar ditolak"
    var saran: Double? = null
    var skorTerbaik: Double? = null
    if (dalam.isNotEmpty() && luar.isNotEmpty()) {
        val kandidat = (20..140).map { it * 0.5 } // 10.0 .. 70.0
        val skor = kandidat.map { d ->
            val a = dalam.count { diterima(it, rssiMin, d, minValid) }.toDouble() / dalam.size
            val b = luar.count { !diterima(it, rssiMin, d, minValid) }.toDouble() / luar.size
            (a + b) / 2
        }
        val terbaik = skor.max()
        val semuaTerbaik = kandidat.filterIndexed { i, _ -> skor[i] == terbaik }
        saran = semuaTerbaik[semuaTerbaik.size / 2] // tengah dari rentang terbaik, lebih aman
        skorTerbaik = terbaik
    }

    return Rekap(
        dalamTotal = dalam.size,
        dalamDiterima = dalam.count { diterima(it, rssiMin, distanceMax, minValid) },
        luarTotal = luar.size,
        luarDitolak = luar.count { !diterima(it, rssiMin, distanceMax, minValid) },
        jarakDalam = dalam.flatMap { it.jarak },
        jarakLuar = luar.flatMap { it.jarak },
        saranJarak = saran,
        skorSaran = skorTerbaik,
    )
}

private fun RawScan.keWifiScan() = WifiScan(readings.map { (b, r) -> WifiReading(b, r) }, timeMs, fresh)

private fun fmt(v: Double) = String.format(Locale.US, "%.1f", v)

private fun persen(a: Int, b: Int) = if (b == 0) "-" else "${(a * 100.0 / b).roundToInt()}%"

private fun ringkasJarak(list: List<Double>): String {
    if (list.isEmpty()) return "-"
    val urut = list.sorted()
    val median = if (urut.size % 2 == 1) urut[urut.size / 2] else (urut[urut.size / 2 - 1] + urut[urut.size / 2]) / 2
    return "median ${fmt(median)} (min ${fmt(urut.first())}, maks ${fmt(urut.last())}, ${urut.size} scan)"
}

@Composable
fun TesKnnScreen(
    username: String,
    userId: String,
    fingerprint: FingerprintData,
    defaultParams: PositioningParams,
    kalibrasi: KalibrasiLokal,
    scanner: AndroidWifiScanner,
    motionTracker: MotionTracker,
    repository: KalibrasiRepository,
    sumberParameter: String,
    muatParameter: suspend () -> Unit,
    simpanUntukSemua: suspend (roomId: String, rssiMin: Int, distanceMax: Double) -> Unit,
    onBack: () -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember(fingerprint, defaultParams) { KnnPositioningEngine(fingerprint, defaultParams) }
    val ruangan = remember(fingerprint, defaultParams) { fingerprint.rooms.filter { it.roomId in defaultParams.rooms } }

    // Ambil batas terbaru dari Firestore saat halaman dibuka (defaultParams ikut berubah)
    LaunchedEffect(Unit) {
        try {
            muatParameter()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // tetap pakai yang ada
        }
    }

    // Layar tetap menyala selama di halaman tes
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    var roomId by rememberSaveable { mutableStateOf(ruangan.firstOrNull()?.roomId) }
    var versiBatas by remember { mutableStateOf(0) } // naik setiap batas diubah
    val params = remember(versiBatas, defaultParams) { kalibrasi.params(defaultParams) }
    val batas = roomId?.let { params.rooms[it] }
    val batasDefault = roomId?.let { defaultParams.rooms[it] }

    var posisi by remember { mutableStateOf(TestPosition.DALAM) }
    var catatan by remember { mutableStateOf("") }
    var jumlahScan by remember { mutableStateOf(3) }
    var memindai by remember { mutableStateOf(false) }
    var progres by remember { mutableStateOf(0) }
    var galat by remember { mutableStateOf<String?>(null) }
    var pesanSimpan by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // teks, isError

    val daftarTes = remember { mutableStateListOf<TesDievaluasi>() }
    var memuatRiwayat by remember { mutableStateOf(false) }
    var galatRiwayat by remember { mutableStateOf<String?>(null) }
    var hapusTarget by remember { mutableStateOf<TesDievaluasi?>(null) }
    var konfirmasiSimpanSemua by remember { mutableStateOf(false) }
    var menyimpanSemua by remember { mutableStateOf(false) }
    var pesanSimpanSemua by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    // Waktu scan terakhir untuk perkiraan throttling Android
    val waktuScan = remember { mutableStateListOf<Long>() }
    var sekarang by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            sekarang = System.currentTimeMillis()
            delay(1000)
        }
    }
    val scanTerakhir = waktuScan.filter { sekarang - it < JENDELA_THROTTLE_MS }.sorted()
    val harusExpire = scanTerakhir.size + jumlahScan - BATAS_SCAN_ANDROID
    val tungguDetik = if (harusExpire > 0) {
        ((scanTerakhir[harusExpire - 1] + JENDELA_THROTTLE_MS - sekarang) / 1000).coerceAtLeast(1)
    } else 0L

    // Muat riwayat tes ruangan dari Firestore lalu evaluasi ulang
    LaunchedEffect(roomId) {
        val id = roomId ?: return@LaunchedEffect
        daftarTes.clear()
        memuatRiwayat = true
        galatRiwayat = null
        try {
            val tes = repository.testsForRoom(id)
            val hasil = withContext(Dispatchers.Default) {
                tes.map { t -> TesDievaluasi(t, t.scans.map { engine.evaluateScan(it.keWifiScan(), id) }) }
            }
            daftarTes.addAll(hasil.sortedByDescending { it.test.createdAtMs })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            galatRiwayat = "Gagal memuat riwayat: ${pesanGalat(e)}"
        } finally {
            memuatRiwayat = false
        }
    }

    fun ubahBatas(rssiMin: Int, distanceMax: Double) {
        val id = roomId ?: return
        kalibrasi.simpan(id, rssiMin.coerceIn(-100, -30), distanceMax.coerceIn(1.0, 150.0))
        versiBatas++
    }

    fun jalankanTes() {
        val id = roomId ?: return
        val b = batas ?: return
        val masalah = wifiEnvironmentProblem(context)
        if (masalah != null) {
            galat = masalah
            return
        }
        galat = null
        pesanSimpan = null
        scope.launch {
            memindai = true
            progres = 0
            try {
                val (scans, gerakan) = motionTracker.rekam {
                    scanner.scan(jumlahScan) {
                        progres = it
                        waktuScan += System.currentTimeMillis()
                    }
                }
                val basi = scanner.staleScanCount
                val evals = withContext(Dispatchers.Default) { scans.map { engine.evaluateScan(it, id) } }
                val lolosCount = evals.count { lolos(it, b.rssiMin, b.distanceMax) }
                val diterimaTes = lolosCount >= params.minValidScans
                val alasan = if (diterimaTes) null else evals.mapNotNull { e ->
                    when {
                        !e.fresh -> "STALE_SCAN"
                        e.strongest == null -> "NO_SIGNAL"
                        !e.enoughBssids -> "FEW_BSSID"
                        !e.apDominant -> "ROOM_AP_NOT_DOMINANT"
                        e.strongest!!.rssi < b.rssiMin -> "RSSI_TOO_WEAK"
                        !e.roomMajority -> "WRONG_ROOM"
                        e.verdict.knnDistance > b.distanceMax -> "TOO_FAR"
                        else -> null
                    }
                }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
                val jarak = evals.map { it.verdict.knnDistance }.filter { !it.isNaN() }

                val test = KnnTest(
                    roomId = id,
                    position = posisi,
                    note = catatan.trim(),
                    scans = scans.map { s -> RawScan(s.timeMs, s.readings.associate { it.bssid to it.rssi }, s.fresh) },
                    accepted = diterimaTes,
                    reason = alasan,
                    validScans = lolosCount,
                    avgDistance = jarak.takeIf { it.isNotEmpty() }?.average(),
                    rssiMin = b.rssiMin,
                    distanceMax = b.distanceMax,
                    staleScans = basi,
                    summaries = evals.map { ringkasScan(it, b.rssiMin, b.distanceMax) },
                    fingerprintVersion = fingerprint.version,
                    deviceModel = Build.MODEL,
                    testerId = userId,
                )
                val baru = TesDievaluasi(test, evals, gerakan)
                daftarTes.add(0, baru)

                // Simpan ke Firestore; kalau gagal, hasil tetap tampil di layar
                pesanSimpan = try {
                    val docId = repository.saveTest(test)
                    val idx = daftarTes.indexOf(baru)
                    if (idx >= 0) daftarTes[idx] = TesDievaluasi(test.copy(id = docId), evals, gerakan)
                    "Tersimpan ke Firestore (knnTests)." to false
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    "Gagal menyimpan ke Firestore: ${pesanGalat(e)}. Hasil hanya ada di layar ini." to true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                galat = "Izin lokasi ditolak sistem."
            } catch (e: Exception) {
                galat = pesanGalat(e)
            } finally {
                memindai = false
            }
        }
    }

    val izinLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { diberi ->
        if (diberi) jalankanTes() else galat = "Izin lokasi diperlukan untuk scan Wi-Fi."
    }

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
            // ---------- Ruangan ----------
            SectionCard("Tes KNN & Kalibrasi") {
                HelperText(
                    "Fingerprint ${fingerprint.version} · ${fingerprint.device} · SSID \"${fingerprint.ssid}\" · " +
                        "${fingerprint.references.size} scan referensi · ${fingerprint.bssids.size} BSSID"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Ruangan target")
                    SelectField(
                        value = roomId?.let { fingerprint.roomName(it) },
                        placeholder = "Pilih ruangan",
                        options = ruangan.map { it.name },
                        onSelect = { roomId = ruangan[it].roomId },
                        enabled = !memindai,
                        emptyText = "Belum ada ruangan dengan parameter KNN"
                    )
                }
            }

            // ---------- Batas ----------
            if (roomId != null && batas != null && batasDefault != null) {
                SectionCard("Batas (hanya di HP ini)") {
                    HelperText(
                        "Perubahan di sini hanya berlaku di layar ini. Absen mahasiswa memakai batas aktif " +
                            "(rssiMin ${batasDefault.rssiMin}, distanceMax ${fmt(batasDefault.distanceMax)}; sumber: $sumberParameter)."
                    )
                    HelperText(
                        "AP utama: ${batas.mainApPrefix} · minimal ${params.minValidScans} scan lolos · k = ${params.k} · " +
                            "minimal ${params.minKnownBssids} BSSID dikenal · gerak maks ${fmt(params.maxDisplacementM)} m"
                    )
                    Penyetel(
                        label = "rssiMin (dBm)",
                        nilai = "${batas.rssiMin}",
                        berubah = batas.rssiMin != batasDefault.rssiMin,
                        enabled = !memindai,
                        langkah = listOf(
                            "−5" to { ubahBatas(batas.rssiMin - 5, batas.distanceMax) },
                            "−1" to { ubahBatas(batas.rssiMin - 1, batas.distanceMax) },
                            "+1" to { ubahBatas(batas.rssiMin + 1, batas.distanceMax) },
                            "+5" to { ubahBatas(batas.rssiMin + 5, batas.distanceMax) },
                        )
                    )
                    Penyetel(
                        label = "distanceMax",
                        nilai = fmt(batas.distanceMax),
                        berubah = batas.distanceMax != batasDefault.distanceMax,
                        enabled = !memindai,
                        langkah = listOf(
                            "−5" to { ubahBatas(batas.rssiMin, batas.distanceMax - 5) },
                            "−1" to { ubahBatas(batas.rssiMin, batas.distanceMax - 1) },
                            "+1" to { ubahBatas(batas.rssiMin, batas.distanceMax + 1) },
                            "+5" to { ubahBatas(batas.rssiMin, batas.distanceMax + 5) },
                        )
                    )
                    val beda = batas.rssiMin != batasDefault.rssiMin || batas.distanceMax != batasDefault.distanceMax
                    if (beda) {
                        PrimaryButton(
                            if (menyimpanSemua) "Menyimpan..." else "Simpan untuk Semua (Firestore)",
                            onClick = { konfirmasiSimpanSemua = true },
                            enabled = !memindai && !menyimpanSemua
                        )
                    }
                    if (kalibrasi.isUbah(roomId!!)) {
                        SecondaryButton(
                            "Kembalikan ke Batas Aktif",
                            onClick = {
                                kalibrasi.reset(roomId!!)
                                versiBatas++
                            },
                            enabled = !memindai && !menyimpanSemua
                        )
                    }
                    pesanSimpanSemua?.let { (teks, error) ->
                        if (error) ErrorText(teks) else HelperText(teks, color = StatusSuccess)
                    }
                }

                // ---------- Tes ----------
                SectionCard("Uji Posisi") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FieldLabel("Posisi kamu sekarang")
                        Pilihan(
                            opsi = TestPosition.values().map { "${it.label} ruangan" },
                            terpilih = posisi.ordinal,
                            onPilih = { posisi = TestPosition.values()[it] },
                            enabled = !memindai
                        )
                        HelperText(
                            if (posisi == TestPosition.DALAM) "Seharusnya DITERIMA. Coba tengah, pojok, dan dekat pintu."
                            else "Seharusnya DITOLAK. Coba koridor, depan pintu, tangga, dan ruang sebelah."
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FieldLabel("Catatan posisi")
                        AppTextField(catatan, { catatan = it.take(100) }, "contoh: pojok belakang kiri / koridor depan pintu")
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FieldLabel("Jumlah scan per tes")
                        Pilihan(
                            opsi = PILIHAN_JUMLAH_SCAN.map { "$it scan" },
                            terpilih = PILIHAN_JUMLAH_SCAN.indexOf(jumlahScan).coerceAtLeast(0),
                            onPilih = { jumlahScan = PILIHAN_JUMLAH_SCAN[it] },
                            enabled = !memindai
                        )
                    }
                    if (tungguDetik > 0 && !memindai) {
                        Text(
                            "Tunggu ±$tungguDetik detik supaya scan tidak basi (Android maks $BATAS_SCAN_ANDROID scan per 2 menit). " +
                                "Abaikan kalau \"Wi-Fi scan throttling\" sudah dimatikan di Opsi Pengembang.",
                            fontSize = 13.sp,
                            color = readableColor(StatusWarning)
                        )
                    }
                    PrimaryButton(
                        if (memindai) "Scan $progres/$jumlahScan..." else "Scan & Uji",
                        onClick = {
                            if (!hasLocationPermission(context)) {
                                izinLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            } else {
                                jalankanTes()
                            }
                        },
                        enabled = !memindai
                    )
                    HelperText("Berdiri diam selama scan. Satu tes = $jumlahScan scan, diterima kalau minimal ${params.minValidScans} lolos.")
                    galat?.let { ErrorText(it) }
                    pesanSimpan?.let { (teks, error) ->
                        if (error) ErrorText(teks) else HelperText(teks, color = StatusSuccess)
                    }
                }

                // ---------- Hasil terakhir ----------
                daftarTes.firstOrNull()?.let { terakhir ->
                    SectionCard("Hasil Tes Terakhir") {
                        DetailTes(terakhir, fingerprint, batas.rssiMin, batas.distanceMax, params.minValidScans)
                    }
                }

                // ---------- Rekap ----------
                val rekap = remember(daftarTes.toList(), batas.rssiMin, batas.distanceMax) {
                    hitungRekap(daftarTes.toList(), batas.rssiMin, batas.distanceMax, params.minValidScans)
                }
                SectionCard("Rekap Akurasi") {
                    HelperText("Dihitung ulang dari semua tes ruangan ini dengan batas di atas (rssiMin ${batas.rssiMin}, distanceMax ${fmt(batas.distanceMax)}).")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KotakAngka(
                            "Dalam diterima",
                            "${rekap.dalamDiterima}/${rekap.dalamTotal}",
                            persen(rekap.dalamDiterima, rekap.dalamTotal),
                            Modifier.weight(1f)
                        )
                        KotakAngka(
                            "Luar ditolak",
                            "${rekap.luarDitolak}/${rekap.luarTotal}",
                            persen(rekap.luarDitolak, rekap.luarTotal),
                            Modifier.weight(1f)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FieldLabel("Jarak KNN per scan")
                        HelperText("Dalam: ${ringkasJarak(rekap.jarakDalam)}")
                        HelperText("Luar: ${ringkasJarak(rekap.jarakLuar)}")
                    }
                    val saran = rekap.saranJarak
                    if (saran != null && rekap.skorSaran != null) {
                        HelperText(
                            "Saran distanceMax ≈ ${fmt(saran)} (rata-rata akurasi dalam & luar ${(rekap.skorSaran * 100).roundToInt()}%, " +
                                "dengan rssiMin ${batas.rssiMin}).",
                            color = UnsBlue
                        )
                        if (saran != batas.distanceMax) {
                            SecondaryButton(
                                "Pakai ${fmt(saran)} di HP ini",
                                onClick = { ubahBatas(batas.rssiMin, saran) },
                                enabled = !memindai
                            )
                        }
                    } else {
                        HelperText("Saran batas muncul setelah ada tes \"Dalam\" dan \"Luar\".")
                    }
                }

                // ---------- Riwayat ----------
                SectionTitle("Riwayat Tes (${daftarTes.size})")
                when {
                    memuatRiwayat -> HelperText("Memuat riwayat...")
                    galatRiwayat != null -> ErrorText(galatRiwayat!!)
                    daftarTes.isEmpty() -> HelperText("Belum ada tes di ruangan ini.")
                }
                daftarTes.take(RIWAYAT_TAMPIL).forEach { t ->
                    KartuRiwayat(
                        t,
                        fingerprint,
                        batas.rssiMin,
                        batas.distanceMax,
                        params.minValidScans,
                        onHapus = { hapusTarget = t }
                    )
                }
                if (daftarTes.size > RIWAYAT_TAMPIL) {
                    HelperText("${daftarTes.size - RIWAYAT_TAMPIL} tes lama tidak ditampilkan (tetap ikut dihitung di rekap).")
                }
            }
        }
    }

    val idSimpan = roomId
    val batasSimpan = idSimpan?.let { params.rooms[it] }
    if (konfirmasiSimpanSemua && idSimpan != null && batasSimpan != null) {
        ConfirmDialog(
            title = "Simpan untuk semua HP?",
            text = "Batas ${fingerprint.roomName(idSimpan)} (rssiMin ${batasSimpan.rssiMin}, distanceMax ${fmt(batasSimpan.distanceMax)}) " +
                "disimpan ke Firestore config/positioning dan langsung dipakai absen mahasiswa berikutnya.",
            confirmText = "Simpan",
            danger = false,
            onConfirm = {
                konfirmasiSimpanSemua = false
                scope.launch {
                    menyimpanSemua = true
                    pesanSimpanSemua = try {
                        simpanUntukSemua(idSimpan, batasSimpan.rssiMin, batasSimpan.distanceMax)
                        kalibrasi.reset(idSimpan) // batas lokal sudah sama dengan batas aktif
                        versiBatas++
                        "Tersimpan. Absen mahasiswa sekarang memakai batas ini." to false
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        "Gagal menyimpan: ${pesanGalat(e)}" to true
                    } finally {
                        menyimpanSemua = false
                    }
                }
            },
            onDismiss = { konfirmasiSimpanSemua = false }
        )
    }

    hapusTarget?.let { t ->
        ConfirmDialog(
            title = "Hapus tes ini?",
            text = "Tes ${t.test.position.label.lowercase()} \"${t.test.note.ifBlank { "-" }}\" (${formatHariTanggalJam(t.test.createdAtMs)}) " +
                "dihapus dari Firestore dan tidak dihitung di rekap.",
            confirmText = "Hapus",
            onConfirm = {
                hapusTarget = null
                scope.launch {
                    try {
                        if (t.test.id.isNotEmpty()) repository.deleteTest(t.test.id)
                        daftarTes.remove(t)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        galatRiwayat = "Gagal menghapus: ${pesanGalat(e)}"
                    }
                }
            },
            onDismiss = { hapusTarget = null }
        )
    }
}

/** Ringkasan satu scan untuk disimpan di Firestore (mudah dibaca di Console). */
private fun ringkasScan(e: ScanEvaluation, rssiMin: Int, distanceMax: Double): String {
    val basi = if (e.fresh) "" else " (BASI)"
    val terkuat = e.strongest ?: return "GAGAL$basi · tidak ada AP terbaca"
    val status = (if (lolos(e, rssiMin, distanceMax)) "LOLOS" else "GAGAL") + basi + " · ${e.knownBssids} BSSID dikenal"
    val tetangga = e.neighbors.joinToString("; ") { "${it.roomId}[${it.label}] ${fmt(it.distance)}" }
    return "$status · AP ${terkuat.bssid} ${terkuat.rssi} dBm · d=${fmt(e.verdict.knnDistance)} · $tetangga"
}

/** Detail satu tes: keputusan + rincian tiap scan. */
@Composable
private fun DetailTes(t: TesDievaluasi, fingerprint: FingerprintData, rssiMin: Int, distanceMax: Double, minValid: Int) {
    val ok = diterima(t, rssiMin, distanceMax, minValid)
    val lolosCount = t.evals.count { lolos(it, rssiMin, distanceMax) }
    val sesuaiHarapan = (t.test.position == TestPosition.DALAM) == ok

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusPill(if (ok) "DITERIMA" else "DITOLAK", if (ok) StatusSuccess else StatusDanger)
        StatusPill(t.test.position.label, UnsBlue)
        StatusPill(if (sesuaiHarapan) "Sesuai harapan" else "Tidak sesuai", if (sesuaiHarapan) StatusSuccess else StatusWarning)
    }
    HelperText("Scan lolos $lolosCount/${t.evals.size} (minimal $minValid) · ${formatHariTanggalJam(t.test.createdAtMs)}")
    if (t.test.note.isNotBlank()) HelperText("Catatan: ${t.test.note}")
    if (t.test.rssiMin != rssiMin || t.test.distanceMax != distanceMax) {
        HelperText(
            "Saat tes: ${if (t.test.accepted) "diterima" else "ditolak"} dengan rssiMin ${t.test.rssiMin}, " +
                "distanceMax ${fmt(t.test.distanceMax)}."
        )
    }
    if (t.test.staleScans > 0) {
        Text(
            "${t.test.staleScans} scan basi (dibatasi Android), hasilnya mungkin sama dengan scan sebelumnya.",
            fontSize = 13.sp,
            color = readableColor(StatusWarning)
        )
    }
    t.gerakan?.let { g ->
        HelperText(
            if (!g.sensorAvailable) "Gerakan: HP tidak punya akselerometer (PDR tidak aktif)."
            else "Gerakan selama scan: ${g.steps} langkah (±${fmt(g.displacementM)} m)" +
                if (g.steps > 0) " — saat absen asli, lebih dari batas gerak akan ditolak." else ""
        )
    }
    val tebakan = t.evals.mapNotNull { it.neighborRoomId }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    HelperText("Tebakan KNN murni: ${tebakan?.let { fingerprint.roomName(it) } ?: "-"}")
    t.evals.forEachIndexed { i, e ->
        KartuScan(i + 1, t.test.scans.getOrNull(i), e, fingerprint, rssiMin, distanceMax)
    }
}

@Composable
private fun KartuScan(
    nomor: Int,
    scan: RawScan?,
    e: ScanEvaluation,
    fingerprint: FingerprintData,
    rssiMin: Int,
    distanceMax: Double
) {
    var tampilMentah by remember { mutableStateOf(false) }
    val mono = FontFamily.Monospace
    val ok = lolos(e, rssiMin, distanceMax)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PageBackground,
        border = BorderStroke(1.dp, (if (ok) StatusSuccess else StatusDanger).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Scan $nomor: ${if (ok) "LOLOS" else "GAGAL"}", fontWeight = FontWeight.Bold, color = if (ok) StatusSuccess else StatusDanger)
            if (!e.fresh) {
                Text("Scan basi: Android tidak memberi data baru, scan ini tidak dinilai.", fontSize = 13.sp, color = readableColor(StatusWarning))
            }
            Text(
                "BSSID dikenal: ${e.knownBssids} ${tanda(e.enoughBssids)}",
                fontSize = 13.sp,
                color = TextPrimary
            )
            val terkuat = e.strongest
            if (terkuat == null) {
                Text("Tidak ada AP \"${fingerprint.ssid}\" terbaca.", fontSize = 13.sp, color = TextPrimary)
            } else {
                Text("AP terkuat: ${terkuat.bssid} (${terkuat.rssi} dBm)", fontFamily = mono, fontSize = 12.sp, color = TextPrimary)
                Text(
                    "Lapis 1: AP utama ${tanda(e.apDominant)} · RSSI ≥ $rssiMin ${tanda(terkuat.rssi >= rssiMin)}",
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text(
                    "Lapis 2: mayoritas ruangan ${tanda(e.roomMajority)} · jarak ${fmt(e.verdict.knnDistance)} ≤ ${fmt(distanceMax)} " +
                        tanda(e.verdict.knnDistance <= distanceMax),
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text("3 tetangga terdekat:", fontSize = 12.sp, color = TextPrimary.copy(alpha = 0.7f))
                e.neighbors.forEachIndexed { i, n ->
                    Text(
                        "${i + 1}. ${fingerprint.roomName(n.roomId)} [${n.label}]  d=${fmt(n.distance)}",
                        fontFamily = mono,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }
            if (scan != null) {
                Text(
                    if (tampilMentah) "Sembunyikan RSSI mentah" else "Tampilkan RSSI mentah (${scan.readings.size} AP)",
                    Modifier.clickable { tampilMentah = !tampilMentah }.padding(vertical = 4.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = UnsBlue
                )
                if (tampilMentah) {
                    scan.readings.entries.sortedByDescending { it.value }.forEach { (bssid, rssi) ->
                        val dikenal = if (fingerprint.bssidIndex.containsKey(bssid.lowercase())) "" else "  (tidak ada di fingerprint)"
                        Text("$bssid  $rssi$dikenal", fontFamily = mono, fontSize = 12.sp, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun KartuRiwayat(
    t: TesDievaluasi,
    fingerprint: FingerprintData,
    rssiMin: Int,
    distanceMax: Double,
    minValid: Int,
    onHapus: () -> Unit
) {
    var terbuka by remember { mutableStateOf(false) }
    val ok = diterima(t, rssiMin, distanceMax, minValid)
    val sesuai = (t.test.position == TestPosition.DALAM) == ok
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { terbuka = !terbuka },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ItemTitle("${t.test.position.label} · ${t.test.note.ifBlank { "tanpa catatan" }}")
                    ItemSubtitle(
                        "${formatHariTanggalJam(t.test.createdAtMs)} · jarak rata-rata " +
                            (t.jarak.takeIf { it.isNotEmpty() }?.average()?.let { fmt(it) } ?: "-")
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusPill(if (ok) "Diterima" else "Ditolak", if (sesuai) StatusSuccess else StatusDanger)
            }
            if (terbuka) {
                DetailTes(t, fingerprint, rssiMin, distanceMax, minValid)
                SecondaryButton("Hapus Tes Ini", onHapus, color = StatusDanger)
            }
        }
    }
}

/** Baris penyetel angka: label, nilai, dan tombol langkah (−5 −1 +1 +5). */
@Composable
private fun Penyetel(
    label: String,
    nilai: String,
    berubah: Boolean,
    enabled: Boolean,
    langkah: List<Pair<String, () -> Unit>>
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FieldLabel(label)
            Spacer(Modifier.weight(1f))
            Text(nilai, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (berubah) UnsBlue else UnsNavy)
            if (berubah) {
                Spacer(Modifier.width(6.dp))
                StatusPill("diubah", UnsBlue)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            langkah.forEach { (teks, aksi) ->
                OutlinedButton(
                    onClick = aksi,
                    enabled = enabled,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) { Text(teks, fontWeight = FontWeight.Bold, color = UnsBlue) }
            }
        }
    }
}

/** Pilihan berbentuk tombol berjajar (seperti Hadir/Sakit/Izin). */
@Composable
private fun Pilihan(opsi: List<String>, terpilih: Int, onPilih: (Int) -> Unit, enabled: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opsi.forEachIndexed { i, teks ->
            val aktif = i == terpilih
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = enabled) { onPilih(i) },
                shape = RoundedCornerShape(12.dp),
                color = if (aktif) UnsBlue else SurfaceWhite,
                border = BorderStroke(1.dp, if (aktif) UnsBlue else TextPrimary.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(teks, color = if (aktif) Color.White else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun KotakAngka(label: String, angka: String, persen: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(12.dp), color = PageBackground) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(persen, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = UnsBlue)
            Text(angka, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(label, fontSize = 12.sp, color = TextPrimary.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        }
    }
}

private fun tanda(ok: Boolean) = if (ok) "✓" else "✗"
