package com.fatisdahere.app.positioning

data class RoomInfo(val roomId: String, val name: String)

data class ReferencePoint(
    val roomId: String,
    val label: String,
    val scans: List<Map<String, Int>>,
)

class ReferenceScan(val roomId: String, val label: String, val vector: DoubleArray)

class FingerprintData(
    val version: String,
    val device: String,
    val ssid: String,
    val missingRssi: Int,
    val rooms: List<RoomInfo>,
    val bssids: List<String>,
    val points: List<ReferencePoint>,
) {
    val bssidIndex: Map<String, Int> = bssids.withIndex().associate { (i, b) -> b.lowercase() to i }

    val references: List<ReferenceScan> = points.flatMap { point ->
        point.scans.map { scan ->
            val vector = DoubleArray(bssids.size) { missingRssi.toDouble() }
            for ((bssid, rssi) in scan) {
                val idx = bssidIndex[bssid.lowercase()] ?: continue
                vector[idx] = rssi.toDouble()
            }
            ReferenceScan(point.roomId, point.label, vector)
        }
    }

    fun roomName(roomId: String): String = rooms.firstOrNull { it.roomId == roomId }?.name ?: roomId
}
