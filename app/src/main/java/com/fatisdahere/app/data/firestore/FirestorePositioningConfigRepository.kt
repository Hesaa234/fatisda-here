package com.fatisdahere.app.data.firestore

import com.fatisdahere.app.data.PositioningConfigRepository
import com.fatisdahere.app.data.terapkanConfig
import com.fatisdahere.app.positioning.PositioningParams
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/** Dokumen config/positioning. Rules: semua yang login boleh membaca, hanya admin yang menulis. */
class FirestorePositioningConfigRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : PositioningConfigRepository {

    private val dokumen get() = db.collection("config").document("positioning")

    override suspend fun load(base: PositioningParams): PositioningParams? {
        val snap = dokumen.get().await()
        val data = snap.data ?: return null
        return terapkanConfig(base, data)
    }

    override suspend fun saveRoom(roomId: String, rssiMin: Int, distanceMax: Double) {
        // merge: hanya ruangan ini yang berubah, ruangan lain dan field umum tetap
        dokumen.set(
            mapOf(
                "rssiMin" to mapOf(roomId to rssiMin),
                "distanceMax" to mapOf(roomId to distanceMax),
            ),
            SetOptions.merge()
        ).await()
    }
}
