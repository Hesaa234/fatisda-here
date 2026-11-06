package com.fatisdahere.app.di

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fatisdahere.app.data.AttendanceRepository
import com.fatisdahere.app.data.AuthRepository
import com.fatisdahere.app.data.CourseRepository
import com.fatisdahere.app.data.FingerprintLoader
import com.fatisdahere.app.data.KalibrasiLokal
import com.fatisdahere.app.data.KalibrasiRepository
import com.fatisdahere.app.data.PositioningConfigRepository
import com.fatisdahere.app.data.RoomRepository
import com.fatisdahere.app.data.SessionRepository
import com.fatisdahere.app.data.UserRepository
import com.fatisdahere.app.data.auth.FirebaseAuthRepository
import com.fatisdahere.app.data.fake.FakeAttendanceRepository
import com.fatisdahere.app.data.fake.FakeAuthRepository
import com.fatisdahere.app.data.fake.FakeCourseRepository
import com.fatisdahere.app.data.fake.FakeDatabase
import com.fatisdahere.app.data.fake.FakeKalibrasiRepository
import com.fatisdahere.app.data.fake.FakePositioningConfigRepository
import com.fatisdahere.app.data.fake.FakeRoomRepository
import com.fatisdahere.app.data.fake.FakeSessionRepository
import com.fatisdahere.app.data.fake.FakeUserRepository
import com.fatisdahere.app.data.firestore.FirestoreAttendanceRepository
import com.fatisdahere.app.data.firestore.FirestoreCourseRepository
import com.fatisdahere.app.data.firestore.FirestoreKalibrasiRepository
import com.fatisdahere.app.data.firestore.FirestorePositioningConfigRepository
import com.fatisdahere.app.data.firestore.FirestoreRoomRepository
import com.fatisdahere.app.data.firestore.FirestoreSessionRepository
import com.fatisdahere.app.data.firestore.FirestoreUserRepository
import com.fatisdahere.app.positioning.FingerprintData
import com.fatisdahere.app.positioning.KnnPositioningEngine
import com.fatisdahere.app.positioning.PositioningParams
import com.fatisdahere.app.sensing.AndroidWifiScanner
import com.fatisdahere.app.sensing.MotionTracker
import kotlin.coroutines.cancellation.CancellationException

/** Satu tempat untuk membuat semua objek yang dipakai bersama oleh layar. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    companion object {
        /**
         * true  = data asli dari Firebase (login dengan akun di Firebase Authentication)
         * false = data dummy di memori (akun demo mahasiswa1, dosen1, admin; sandi bebas)
         *
         * Pakai false kalau mau mengetes layar tanpa internet / tanpa mengubah data asli.
         */
        const val PAKAI_FIREBASE = true
    }

    // ---------- Positioning (KNN) ----------
    val fingerprint: FingerprintData by lazy { FingerprintLoader.load(appContext) }
    val wifiScanner: AndroidWifiScanner by lazy { AndroidWifiScanner(appContext, fingerprint.ssid) }

    /** PDR: hitung langkah dengan akselerometer selama scan. */
    val motionTracker: MotionTracker by lazy { MotionTracker(appContext) }

    /**
     * Parameter KNN yang sedang berlaku. Awalnya default() di kode; diganti isi Firestore
     * config/positioning setiap kali muatParameterKnn() berhasil. Berupa state Compose,
     * jadi layar yang membacanya ikut diperbarui.
     */
    var positioningParams: PositioningParams by mutableStateOf(PositioningParams.default())
        private set

    /** Asal parameter yang sedang berlaku, untuk ditampilkan di menu Tes KNN. */
    var sumberParameter: String by mutableStateOf("bawaan kode")
        private set

    /** Engine dengan parameter yang sedang berlaku. */
    val positioningEngine: KnnPositioningEngine
        get() = KnnPositioningEngine(fingerprint, positioningParams)

    val positioningConfigRepository: PositioningConfigRepository by lazy {
        if (PAKAI_FIREBASE) FirestorePositioningConfigRepository() else FakePositioningConfigRepository()
    }

    /**
     * Baca ulang parameter dari Firestore. Kalau gagal (offline, dokumen belum ada),
     * parameter terakhir yang berhasil tetap dipakai, atau default() di kode.
     */
    suspend fun muatParameterKnn(): PositioningParams {
        try {
            val dariServer = positioningConfigRepository.load(PositioningParams.default())
            if (dariServer != null) {
                positioningParams = dariServer
                sumberParameter = "Firestore config/positioning"
            } else {
                positioningParams = PositioningParams.default()
                sumberParameter = "bawaan kode (config/positioning belum ada)"
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // tetap pakai parameter terakhir
        }
        return positioningParams
    }

    /** Simpan batas satu ruangan ke Firestore untuk semua HP, lalu muat ulang. Khusus admin. */
    suspend fun simpanParameterRuangan(roomId: String, rssiMin: Int, distanceMax: Double) {
        positioningConfigRepository.saveRoom(roomId, rssiMin, distanceMax)
        muatParameterKnn()
    }

    /** Batas KNN sementara untuk menu Tes KNN admin (hanya di HP ini, tidak dipakai absen mahasiswa). */
    val kalibrasiLokal: KalibrasiLokal by lazy { KalibrasiLokal(appContext) }

    // ---------- Data ----------
    private val fakeDb by lazy { FakeDatabase() }

    val authRepository: AuthRepository by lazy {
        if (PAKAI_FIREBASE) FirebaseAuthRepository() else FakeAuthRepository(fakeDb)
    }
    val userRepository: UserRepository by lazy {
        if (PAKAI_FIREBASE) FirestoreUserRepository(appContext) else FakeUserRepository(fakeDb)
    }
    val roomRepository: RoomRepository by lazy {
        if (PAKAI_FIREBASE) FirestoreRoomRepository() else FakeRoomRepository(fakeDb)
    }
    val courseRepository: CourseRepository by lazy {
        if (PAKAI_FIREBASE) FirestoreCourseRepository() else FakeCourseRepository(fakeDb)
    }
    val sessionRepository: SessionRepository by lazy {
        if (PAKAI_FIREBASE) FirestoreSessionRepository() else FakeSessionRepository(fakeDb)
    }
    val attendanceRepository: AttendanceRepository by lazy {
        if (PAKAI_FIREBASE) FirestoreAttendanceRepository() else FakeAttendanceRepository(fakeDb)
    }
    val kalibrasiRepository: KalibrasiRepository by lazy {
        if (PAKAI_FIREBASE) FirestoreKalibrasiRepository() else FakeKalibrasiRepository(fakeDb)
    }
}
