package com.fatisdahere.app.positioning

import kotlin.math.sqrt

data class RoomParams(val mainApPrefix: String, val rssiMin: Int, val distanceMax: Double)

data class PositioningParams(
    val k: Int,
    val minValidScans: Int,
    val maxDisplacementM: Double,
    val rooms: Map<String, RoomParams>,
) {
    companion object {
        fun default() = PositioningParams(
            k = 3,
            minValidScans = 2,
            maxDisplacementM = 1.5,
            rooms = mapOf(
                "lab2-upt" to RoomParams("d0:15:a6:d6:7b", -75, 33.0),
                "lab-upt-lt3" to RoomParams("d0:15:a6:d6:67", -75, 18.0),
                "b4-05" to RoomParams("d0:15:a6:d6:ba", -75, 33.0),
            ),
        )
    }
}

data class Neighbor(val roomId: String, val label: String, val distance: Double)

data class ScanEvaluation(
    val verdict: ScanVerdict,
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

        val strongest = scan.readings.maxByOrNull { it.rssi }
        if (strongest == null) {
            return ScanEvaluation(
                verdict = ScanVerdict(false, null, Double.NaN, RejectReason.NO_SIGNAL),
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

        val reason = when {
            !apDominant -> RejectReason.ROOM_AP_NOT_DOMINANT
            !rssiOk -> RejectReason.RSSI_TOO_WEAK
            !roomMajority -> RejectReason.WRONG_ROOM
            !distanceOk -> RejectReason.TOO_FAR
            else -> null
        }

        return ScanEvaluation(
            verdict = ScanVerdict(reason == null, neighborRoomId, avgDistance, reason),
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
