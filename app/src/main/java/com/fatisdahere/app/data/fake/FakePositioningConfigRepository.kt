package com.fatisdahere.app.data.fake

import com.fatisdahere.app.data.PositioningConfigRepository
import com.fatisdahere.app.data.terapkanConfig
import com.fatisdahere.app.positioning.PositioningParams

/** Versi dummy: config disimpan di memori selama aplikasi berjalan. */
class FakePositioningConfigRepository : PositioningConfigRepository {
    private val rssiMin = mutableMapOf<String, Int>()
    private val distanceMax = mutableMapOf<String, Double>()

    override suspend fun load(base: PositioningParams): PositioningParams? {
        if (rssiMin.isEmpty() && distanceMax.isEmpty()) return null
        return terapkanConfig(base, mapOf("rssiMin" to rssiMin.toMap(), "distanceMax" to distanceMax.toMap()))
    }

    override suspend fun saveRoom(roomId: String, rssiMin: Int, distanceMax: Double) {
        this.rssiMin[roomId] = rssiMin
        this.distanceMax[roomId] = distanceMax
    }
}
