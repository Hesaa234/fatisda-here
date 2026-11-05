package com.fatisdahere.app.data

import android.content.Context
import com.fatisdahere.app.positioning.PositioningParams
import com.fatisdahere.app.positioning.RoomParams

/**
 * Batas KNN sementara per ruangan untuk kalibrasi di lapangan, disimpan di HP ini saja
 * (SharedPreferences). Hanya dipakai menu Tes KNN admin; absen mahasiswa tetap memakai
 * PositioningParams.default() di kode.
 *
 * Kalau batas yang pas sudah ketemu, salin angkanya ke PositioningParams.default()
 * (atau nanti ke Firestore config/positioning).
 */
class KalibrasiLokal(context: Context) {
    private val prefs = context.getSharedPreferences("kalibrasi_knn", Context.MODE_PRIVATE)

    /** Parameter default dengan batas yang sudah diubah di HP ini (kalau ada). */
    fun params(base: PositioningParams = PositioningParams.default()): PositioningParams =
        base.copy(rooms = base.rooms.mapValues { (roomId, room) -> terapkan(roomId, room) })

    fun isUbah(roomId: String): Boolean = prefs.contains(kunciRssi(roomId)) || prefs.contains(kunciJarak(roomId))

    fun simpan(roomId: String, rssiMin: Int, distanceMax: Double) {
        prefs.edit()
            .putInt(kunciRssi(roomId), rssiMin)
            .putFloat(kunciJarak(roomId), distanceMax.toFloat())
            .apply()
    }

    fun reset(roomId: String) {
        prefs.edit().remove(kunciRssi(roomId)).remove(kunciJarak(roomId)).apply()
    }

    private fun terapkan(roomId: String, room: RoomParams): RoomParams {
        val rssi = if (prefs.contains(kunciRssi(roomId))) prefs.getInt(kunciRssi(roomId), room.rssiMin) else room.rssiMin
        val jarak = if (prefs.contains(kunciJarak(roomId))) {
            prefs.getFloat(kunciJarak(roomId), room.distanceMax.toFloat()).toDouble()
        } else {
            room.distanceMax
        }
        return room.copy(rssiMin = rssi, distanceMax = Math.round(jarak * 10) / 10.0)
    }

    private fun kunciRssi(roomId: String) = "rssiMin_$roomId"
    private fun kunciJarak(roomId: String) = "distanceMax_$roomId"
}
