package com.fatisdahere.app.positioning

import kotlin.math.sqrt

data class RoomParams(val mainApPrefix: String, val rssiMin: Int, val distanceMax: Double)

/**
 * Parameter KNN. Nilai di default() dipakai kalau Firestore config/positioning tidak bisa dibaca;
 * kalau ada, nilai di Firestore menimpanya (lihat data/PositioningConfigRepository).
 */
data class PositioningParams(
    val k: Int,
    val minValidScans: Int,
    val maxDisplacementM: Double,
    val rooms: Map<String, RoomParams>,
    /** Minimal BSSID yang dikenal fingerprint dalam satu scan; kurang dari ini scan tidak dinilai. */
    val minKnownBssids: Int = 3,
) {
    companion object {
        fun default() = PositioningParams(
            k = 3,
            minValidScans = 2,
            maxDisplacementM = 1.5,
            minKnownBssids = 3,
            rooms = mapOf(
                "lab2-upt" to RoomParams("d0:15:a6:d6:7b", -75, 33.0),
                // 18 -> 30: dengan 18 hampir semua absen di dalam LT 3 gagal di lapis 2 (jarak), lihat uji 31 Okt
                "lab-upt-lt3" to RoomParams("d0:15:a6:d6:67", -75, 30.0),
                "b4-05" to RoomParams("d0:15:a6:d6:ba", -75, 33.0),
            ),
        )
    }
}

data class Neighbor(val roomId: String, val label: String, val distance: Double)

data class ScanEvaluation(
    val verdict: ScanVerdict,
    /** false = scan basi (tidak ada data baru dari Android). */
    val fresh: Boolean,
    /** Jumlah BSSID berbeda di scan ini yang ada di fingerprint. */
    val knownBssids: Int,
    val enoughBssids: Boolean,
    val strongest: WifiReading?,
    val apDominant: Boolean,
    val rssiOk: Boolean,
    val neighbors: List<Neighbor>,
    val roomMajority: Boolean,
    val distanceOk: Boolean,
    val neighborRoomId: String?,
)

class KnnPositioningEngine(
    private val fingerprint: FingerprintData,
    private val params: PositioningParams,
) : PositioningEngine {

    fun evaluateScan(scan: WifiScan, targetRoomId: String): ScanEvaluation {
        val room = requireNotNull(params.rooms[targetRoomId]) { "Parameter ruangan $targetRoomId tidak ada" }

        val knownBssids = scan.readings.map { it.bssid.lowercase() }.distinct()
            .count { it in fingerprint.bssidIndex }
        val enoughBssids = knownBssids >= params.minKnownBssids

        val strongest = scan.readings.maxByOrNull { it.rssi }
        if (strongest == null) {
            return ScanEvaluation(
                verdict = ScanVerdict(false, null, Double.NaN, if (scan.fresh) RejectReason.NO_SIGNAL else RejectReason.STALE_SCAN),
                fresh = scan.fresh,
                knownBssids = 0,
                enoughBssids = false,
                strongest = null,
                apDominant = false,
                rssiOk = false,
                neighbors = emptyList(),
                roomMajority = false,
                distanceOk = false,
                neighborRoomId = null,
            )
        }

        val apDominant = strongest.bssid.lowercase().startsWith(room.mainApPrefix.lowercase())
        val rssiOk = strongest.rssi >= room.rssiMin

        val query = DoubleArray(fingerprint.bssids.size) { fingerprint.missingRssi.toDouble() }
        for (reading in scan.readings) {
            val idx = fingerprint.bssidIndex[reading.bssid.lowercase()] ?: continue
            query[idx] = maxOf(query[idx], reading.rssi.toDouble())
        }

        val neighbors = fingerprint.references
            .map { ref -> Neighbor(ref.roomId, ref.label, euclidean(query, ref.vector)) }
            .sortedBy { it.distance }
            .take(params.k)

        val avgDistance = neighbors.map { it.distance }.average()
        val neighborRoomId = majorityRoom(neighbors)
        val roomMajority = neighbors.count { it.roomId == targetRoomId } * 2 > params.k
        val distanceOk = avgDistance <= room.distanceMax

        // Urutan alasan: data tidak layak dulu (basi / terlalu sedikit), lalu lapis 1, lalu lapis 2
        val reason = when {
            !scan.fresh -> RejectReason.STALE_SCAN
            !enoughBssids -> RejectReason.FEW_BSSID
            !apDominant -> RejectReason.ROOM_AP_NOT_DOMINANT
            !rssiOk -> RejectReason.RSSI_TOO_WEAK
            !roomMajority -> RejectReason.WRONG_ROOM
            !distanceOk -> RejectReason.TOO_FAR
            else -> null
        }

        return ScanEvaluation(
            verdict = ScanVerdict(reason == null, neighborRoomId, avgDistance, reason),
            fresh = scan.fresh,
            knownBssids = knownBssids,
            enoughBssids = enoughBssids,
            strongest = strongest,
            apDominant = apDominant,
            rssiOk = rssiOk,
            neighbors = neighbors,
            roomMajority = roomMajority,
            distanceOk = distanceOk,
            neighborRoomId = neighborRoomId,
        )
    }

    override fun decide(scans: List<WifiScan>, motion: MotionSummary, targetRoomId: String): PositioningDecision {
        val verdicts = scans.map { evaluateScan(it, targetRoomId).verdict }
        val validScans = verdicts.count { it.passed }
        val moving = motion.displacementM > params.maxDisplacementM
        val accepted = !moving && validScans >= params.minValidScans
        val reason = when {
            accepted -> null
            moving -> RejectReason.MOVING
            else -> verdicts.mapNotNull { it.reason }
                .groupingBy { it }.eachCount()
                .maxByOrNull { it.value }?.key
        }
        return PositioningDecision(accepted, validScans, motion.displacementM, verdicts, reason)
    }

    private fun majorityRoom(neighbors: List<Neighbor>): String? {
        if (neighbors.isEmpty()) return null
        val counts = neighbors.groupingBy { it.roomId }.eachCount()
        val top = counts.values.max()
        return neighbors.first { counts.getValue(it.roomId) == top }.roomId
    }

    private fun euclidean(a: DoubleArray, b: DoubleArray): Double {
        var sum = 0.0
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }
}
