package com.fatisdahere.app.positioning

data class WifiReading(val bssid: String, val rssi: Int)
/**
 * Satu hasil scan Wi-Fi. fresh = false kalau Android tidak memberi data baru (kena batas
 * 4 scan per 2 menit), sehingga isinya hanya hasil scan sebelumnya dan tidak boleh dinilai.
 */
data class WifiScan(val readings: List<WifiReading>, val timeMs: Long, val fresh: Boolean = true)
/** Hasil PDR selama scan. sensorAvailable = false kalau HP tidak punya akselerometer. */
data class MotionSummary(val steps: Int, val displacementM: Double, val sensorAvailable: Boolean = true)

enum class RejectReason {
    NO_SIGNAL,
    /** Scan tidak menghasilkan data baru (dibatasi Android). */
    STALE_SCAN,
    /** BSSID yang dikenal fingerprint terlalu sedikit untuk dibandingkan. */
    FEW_BSSID,
    ROOM_AP_NOT_DOMINANT,
    RSSI_TOO_WEAK,
    WRONG_ROOM,
    TOO_FAR,
    MOVING,
}

data class ScanVerdict(
    val passed: Boolean,
    val predictedRoomId: String?,
    val knnDistance: Double,
    val reason: RejectReason?,
)

data class PositioningDecision(
    val accepted: Boolean,
    val validScans: Int,
    val displacementM: Double,
    val verdicts: List<ScanVerdict>,
    val reason: RejectReason?,
) {
    /** Rata-rata jarak KNN dari scan yang lolos (atau semua scan bila tidak ada yang lolos); null kalau tidak ada sinyal. */
    val avgKnnDistance: Double?
        get() {
            val sumber = verdicts.filter { it.passed }.ifEmpty { verdicts }
            return sumber.map { it.knnDistance }.filter { !it.isNaN() }.takeIf { it.isNotEmpty() }?.average()
        }
}

interface PositioningEngine {
    fun decide(scans: List<WifiScan>, motion: MotionSummary, targetRoomId: String): PositioningDecision
}
