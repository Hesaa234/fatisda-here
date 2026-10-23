package com.fatisdahere.app.data

import android.content.Context
import com.fatisdahere.app.positioning.FingerprintData
import com.fatisdahere.app.positioning.ReferencePoint
import com.fatisdahere.app.positioning.RoomInfo
import org.json.JSONObject

object FingerprintLoader {
    const val ASSET_NAME = "fingerprint_v1.json"

    fun load(context: Context): FingerprintData {
        val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        return parse(text)
    }

    fun parse(text: String): FingerprintData {
        val root = JSONObject(text)

        val roomsJson = root.getJSONArray("rooms")
        val rooms = (0 until roomsJson.length()).map {
            val r = roomsJson.getJSONObject(it)
            RoomInfo(r.getString("roomId"), r.getString("name"))
        }

        val bssidsJson = root.getJSONArray("bssids")
        val bssids = (0 until bssidsJson.length()).map { bssidsJson.getString(it) }

        val pointsJson = root.getJSONArray("points")
        val points = (0 until pointsJson.length()).map { i ->
            val p = pointsJson.getJSONObject(i)
            val scansJson = p.getJSONArray("scans")
            val scans = (0 until scansJson.length()).map { j ->
                val s = scansJson.getJSONObject(j)
                s.keys().asSequence().associateWith { key -> s.getInt(key) }
            }
            ReferencePoint(p.getString("roomId"), p.getString("label"), scans)
        }

        return FingerprintData(
            version = root.getString("version"),
            device = root.optString("device"),
            ssid = root.getString("ssid"),
            missingRssi = root.optInt("missingRssi", -100),
            rooms = rooms,
            bssids = bssids,
            points = points,
        )
    }
}
