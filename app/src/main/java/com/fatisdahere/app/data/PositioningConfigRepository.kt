package com.fatisdahere.app.data

import com.fatisdahere.app.positioning.PositioningParams

/**
 * Parameter KNN yang bisa diubah tanpa build APK baru (Firestore config/positioning).
 *
 * Isi dokumen (semua opsional; yang tidak ada memakai nilai di PositioningParams.default()):
 *   k: 3
 *   minValidScans: 2
 *   minKnownBssids: 3
 *   maxDisplacementM: 1.5
 *   rssiMin:      { "lab2-upt": -75, "lab-upt-lt3": -75, "b4-05": -75 }
 *   distanceMax:  { "lab2-upt": 33.0, "lab-upt-lt3": 30.0, "b4-05": 33.0 }
 *   mainApPrefix: { "lab2-upt": "d0:15:a6:d6:7b", ... }
 * Ruangan yang tidak ada di default() diabaikan, karena fingerprint-nya tidak ada di aplikasi.
 */
interface PositioningConfigRepository {
    /** null kalau dokumen belum ada (pakai [base] apa adanya). */
    suspend fun load(base: PositioningParams): PositioningParams?

    /** Menyimpan batas satu ruangan (dipakai menu Tes KNN admin). */
    suspend fun saveRoom(roomId: String, rssiMin: Int, distanceMax: Double)
}

/** Terapkan isi dokumen config ke [base]. Dipakai versi Firestore dan versi dummy. */
fun terapkanConfig(base: PositioningParams, data: Map<String, Any?>): PositioningParams {
    fun angka(key: String) = data[key] as? Number
    fun peta(key: String) = (data[key] as? Map<*, *>).orEmpty()

    val rssiMin = peta("rssiMin")
    val distanceMax = peta("distanceMax")
    val prefix = peta("mainApPrefix")

    return base.copy(
        k = angka("k")?.toInt()?.takeIf { it > 0 } ?: base.k,
        minValidScans = angka("minValidScans")?.toInt()?.takeIf { it > 0 } ?: base.minValidScans,
        minKnownBssids = angka("minKnownBssids")?.toInt()?.takeIf { it >= 0 } ?: base.minKnownBssids,
        maxDisplacementM = angka("maxDisplacementM")?.toDouble()?.takeIf { it > 0 } ?: base.maxDisplacementM,
        rooms = base.rooms.mapValues { (roomId, room) ->
            room.copy(
                rssiMin = (rssiMin[roomId] as? Number)?.toInt() ?: room.rssiMin,
                distanceMax = (distanceMax[roomId] as? Number)?.toDouble() ?: room.distanceMax,
                mainApPrefix = (prefix[roomId] as? String)?.takeIf { it.isNotBlank() } ?: room.mainApPrefix,
            )
        },
    )
}
