package com.fatisdahere.app.ui.debug

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fatisdahere.app.positioning.FingerprintData
import com.fatisdahere.app.positioning.KnnPositioningEngine
import com.fatisdahere.app.positioning.MotionSummary
import com.fatisdahere.app.positioning.PositioningDecision
import com.fatisdahere.app.positioning.PositioningParams
import com.fatisdahere.app.positioning.ScanEvaluation
import com.fatisdahere.app.positioning.WifiScan
import com.fatisdahere.app.sensing.AndroidWifiScanner
import com.fatisdahere.app.sensing.hasLocationPermission
import com.fatisdahere.app.sensing.wifiEnvironmentProblem
import com.fatisdahere.app.ui.components.pesan
import kotlinx.coroutines.launch
import java.util.Locale

private const val SCANS_PER_ATTEMPT = 3

private class TestResult(
    val targetRoomId: String,
    val scans: List<WifiScan>,
    val evaluations: List<ScanEvaluation>,
    val decision: PositioningDecision,
    val staleScans: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugPositioningScreen(
    fingerprint: FingerprintData,
    engine: KnnPositioningEngine,
    params: PositioningParams,
    scanner: AndroidWifiScanner,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var selectedRoomId by remember { mutableStateOf(fingerprint.rooms.first().roomId) }
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var result by remember { mutableStateOf<TestResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Tes Positioning (X-01)", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Fingerprint ${fingerprint.version} · ${fingerprint.device} · SSID \"${fingerprint.ssid}\" · " +
                "${fingerprint.references.size} scan referensi · ${fingerprint.bssids.size} BSSID",
            style = MaterialTheme.typography.bodySmall,
        )

        Text("Ruangan target", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            fingerprint.rooms.forEach { room ->
                FilterChip(
                    selected = room.roomId == selectedRoomId,
                    onClick = { selectedRoomId = room.roomId },
                    label = { Text(room.name) },
                    enabled = !scanning,
                )
            }
        }

        if (!hasPermission) {
            Text(
                "Izin lokasi diperlukan untuk membaca daftar Wi-Fi. Izin ini hanya dipakai untuk scan Wi-Fi.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                Text("Berikan izin lokasi")
            }
        } else {
            Button(
                enabled = !scanning,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val problem = wifiEnvironmentProblem(context)
                    if (problem != null) {
                        errorMessage = problem
                        return@Button
                    }
                    errorMessage = null
                    scope.launch {
                        scanning = true
                        progress = 0
                        try {
                            val scans = scanner.scan(SCANS_PER_ATTEMPT) { progress = it }
                            val evaluations = scans.map { engine.evaluateScan(it, selectedRoomId) }
                            // PDR belum dipasang di aplikasi tes: perpindahan dianggap 0.
                            val decision = engine.decide(scans, MotionSummary(0, 0.0), selectedRoomId)
                            result = TestResult(selectedRoomId, scans, evaluations, decision, scanner.staleScanCount)
                        } catch (e: SecurityException) {
                            errorMessage = "Izin lokasi ditolak sistem."
                        } finally {
                            scanning = false
                        }
                    }
                },
            ) {
                Text(if (scanning) "Scan $progress/$SCANS_PER_ATTEMPT ..." else "Scan & tebak ruangan")
            }
        }

        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        result?.let { ResultSection(it, fingerprint, params) }
    }
}

@Composable
private fun ResultSection(result: TestResult, fingerprint: FingerprintData, params: PositioningParams) {
    val decision = result.decision
    val targetName = fingerprint.roomName(result.targetRoomId)
    val roomParams = params.rooms.getValue(result.targetRoomId)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (decision.accepted) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (decision.accepted) "ABSEN DITERIMA" else "ABSEN DITOLAK",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            decision.reason?.let { Text(it.pesan(targetName, fingerprint.ssid)) }
            Text("Scan lolos: ${decision.validScans}/${result.scans.size} (minimal ${params.minValidScans})")
            Text("Perpindahan PDR: belum aktif (dianggap 0 m)")
            val guess = result.evaluations.mapNotNull { it.neighborRoomId }
                .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            Text("Tebakan KNN murni: ${guess?.let { fingerprint.roomName(it) } ?: "-"}")
            Text(
                "Target: $targetName · AP utama ${roomParams.mainApPrefix} · rssiMin ${roomParams.rssiMin} · " +
                    "distanceMax ${roomParams.distanceMax}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (result.staleScans > 0) {
                Text(
                    "Peringatan: ${result.staleScans} scan tidak menghasilkan data baru (dibatasi Android: " +
                        "maks 4 scan per 2 menit). Tunggu sebentar lalu ulangi.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    result.evaluations.forEachIndexed { index, eval ->
        ScanCard(index + 1, result.scans[index], eval, fingerprint, roomParams.distanceMax)
    }
}

@Composable
private fun ScanCard(
    number: Int,
    scan: WifiScan,
    eval: ScanEvaluation,
    fingerprint: FingerprintData,
    distanceMax: Double,
) {
    var showRaw by remember { mutableStateOf(false) }
    val mono = FontFamily.Monospace

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Scan $number: ${if (eval.verdict.passed) "LOLOS" else "GAGAL"}",
                style = MaterialTheme.typography.titleMedium,
            )
            val strongest = eval.strongest
            if (strongest == null) {
                Text("Tidak ada AP \"${fingerprint.ssid}\" terbaca.")
            } else {
                Text("AP terkuat: ${strongest.bssid} (${strongest.rssi} dBm)", fontFamily = mono, fontSize = 12.sp)
                Text(
                    "Lapis 1 (AP utama + RSSI): ${passFail(eval.apDominant && eval.rssiOk)}" +
                        "  [AP utama: ${passFail(eval.apDominant)}, RSSI: ${passFail(eval.rssiOk)}]",
                )
                Text(
                    "Lapis 2 (KNN): ${passFail(eval.roomMajority && eval.distanceOk)}" +
                        "  [mayoritas ruangan: ${passFail(eval.roomMajority)}, jarak: ${passFail(eval.distanceOk)}]",
                )
                Text(
                    "Rata-rata jarak ${fmt(eval.verdict.knnDistance)} (maks ${fmt(distanceMax)}) · " +
                        "tebakan ${eval.neighborRoomId?.let { fingerprint.roomName(it) } ?: "-"}",
                )
                eval.neighbors.forEachIndexed { i, n ->
                    Text(
                        "${i + 1}. ${fingerprint.roomName(n.roomId)} [${n.label}]  d=${fmt(n.distance)}",
                        fontFamily = mono,
                        fontSize = 12.sp,
                    )
                }
                eval.verdict.reason?.let { Text("Alasan: $it", color = MaterialTheme.colorScheme.error) }
            }

            Divider()
            TextButton(onClick = { showRaw = !showRaw }) {
                Text(if (showRaw) "Sembunyikan RSSI mentah" else "Tampilkan RSSI mentah (${scan.readings.size} AP)")
            }
            if (showRaw) {
                scan.readings.sortedByDescending { it.rssi }.forEach {
                    val known = if (fingerprint.bssidIndex.containsKey(it.bssid)) "" else "  (tidak ada di fingerprint)"
                    Text("${it.bssid}  ${it.rssi}$known", fontFamily = mono, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun passFail(ok: Boolean) = if (ok) "lolos" else "gagal"

private fun fmt(value: Double) = String.format(Locale.US, "%.1f", value)
