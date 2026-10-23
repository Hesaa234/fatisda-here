package com.fatisdahere.app.positioning

data class WifiReading(val bssid: String, val rssi: Int)
data class WifiScan(val readings: List<WifiReading>, val timeMs: Long)
data class MotionSummary(val steps: Int, val displacementM: Double)

enum class RejectReason { NO_SIGNAL, ROOM_AP_NOT_DOMINANT, RSSI_TOO_WEAK, WRONG_ROOM, TOO_FAR, MOVING }

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
)

interface PositioningEngine {
    fun decide(scans: List<WifiScan>, motion: MotionSummary, targetRoomId: String): PositioningDecision
}
