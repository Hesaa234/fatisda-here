package com.fatisdahere.app.positioning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnnPositioningEngineTest {
    private val apA = "aa:aa:aa:aa:aa:01"
    private val apB = "bb:bb:bb:bb:bb:01"

    private val fingerprint = FingerprintData(
        version = "test",
        device = "test",
        ssid = "UNS SOLO",
        missingRssi = -100,
        rooms = listOf(RoomInfo("A", "Ruang A"), RoomInfo("B", "Ruang B")),
        bssids = listOf(apA, apB),
        points = listOf(
            ReferencePoint("A", "0,0", List(3) { mapOf(apA to -50, apB to -90) }),
            ReferencePoint("A", "0,1", List(3) { mapOf(apA to -52, apB to -88) }),
            ReferencePoint("B", "0,0", List(3) { mapOf(apA to -90, apB to -50) }),
        ),
    )

    private val params = PositioningParams(
        k = 3,
        minValidScans = 2,
        maxDisplacementM = 1.5,
        rooms = mapOf(
            "A" to RoomParams("aa:aa:aa:aa:aa", -75, 30.0),
            "B" to RoomParams("bb:bb:bb:bb:bb", -75, 30.0),
        ),
    )

    private val engine = KnnPositioningEngine(fingerprint, params)

    private fun scan(vararg readings: Pair<String, Int>) =
        WifiScan(readings.map { WifiReading(it.first, it.second) }, 0L)

    private val still = MotionSummary(0, 0.0)

    @Test
    fun acceptsWhenInsideTargetRoom() {
        val scans = List(3) { scan(apA to -51, apB to -89) }
        val decision = engine.decide(scans, still, "A")
        assertTrue(decision.accepted)
        assertEquals(3, decision.validScans)
    }

    @Test
    fun rejectsWhenMainApIsNotStrongest() {
        val scans = List(3) { scan(apA to -90, apB to -50) }
        val decision = engine.decide(scans, still, "A")
        assertFalse(decision.accepted)
        assertEquals(RejectReason.ROOM_AP_NOT_DOMINANT, decision.reason)
    }

    @Test
    fun rejectsWhenSignalTooWeak() {
        val scans = List(3) { scan(apA to -80, apB to -95) }
        val decision = engine.decide(scans, still, "A")
        assertEquals(RejectReason.RSSI_TOO_WEAK, decision.reason)
    }

    @Test
    fun rejectsWhenNoSignal() {
        val decision = engine.decide(List(3) { scan() }, still, "A")
        assertEquals(RejectReason.NO_SIGNAL, decision.reason)
    }

    @Test
    fun acceptsWithTwoOfThreeValidScans() {
        val good = scan(apA to -51, apB to -89)
        val bad = scan(apA to -90, apB to -50)
        val decision = engine.decide(listOf(good, bad, good), still, "A")
        assertTrue(decision.accepted)
        assertEquals(2, decision.validScans)
    }

    @Test
    fun rejectsWhenMoving() {
        val scans = List(3) { scan(apA to -51, apB to -89) }
        val decision = engine.decide(scans, MotionSummary(3, 2.0), "A")
        assertFalse(decision.accepted)
        assertEquals(RejectReason.MOVING, decision.reason)
    }
}
