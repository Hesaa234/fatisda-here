package com.fatisdahere.app.di

import android.content.Context
import com.fatisdahere.app.data.AttendanceRepository
import com.fatisdahere.app.data.AuthRepository
import com.fatisdahere.app.data.CourseRepository
import com.fatisdahere.app.data.FingerprintLoader
import com.fatisdahere.app.data.RoomRepository
import com.fatisdahere.app.data.SessionRepository
import com.fatisdahere.app.data.UserRepository
import com.fatisdahere.app.data.auth.FirebaseAuthRepository
import com.fatisdahere.app.data.fake.FakeAttendanceRepository
import com.fatisdahere.app.data.fake.FakeAuthRepository
import com.fatisdahere.app.data.fake.FakeCourseRepository
import com.fatisdahere.app.data.fake.FakeDatabase
import com.fatisdahere.app.data.fake.FakeRoomRepository
import com.fatisdahere.app.data.fake.FakeSessionRepository
import com.fatisdahere.app.data.fake.FakeUserRepository
import com.fatisdahere.app.data.firestore.FirestoreAttendanceRepository
import com.fatisdahere.app.data.firestore.FirestoreCourseRepository
import com.fatisdahere.app.data.firestore.FirestoreRoomRepository
import com.fatisdahere.app.data.firestore.FirestoreSessionRepository
import com.fatisdahere.app.data.firestore.FirestoreUserRepository
import com.fatisdahere.app.positioning.FingerprintData
import com.fatisdahere.app.positioning.KnnPositioningEngine
import com.fatisdahere.app.positioning.PositioningParams
import com.fatisdahere.app.sensing.AndroidWifiScanner

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
    val positioningParams: PositioningParams by lazy { PositioningParams.default() }
    val positioningEngine: KnnPositioningEngine by lazy { KnnPositioningEngine(fingerprint, positioningParams) }
    val wifiScanner: AndroidWifiScanner by lazy { AndroidWifiScanner(appContext, fingerprint.ssid) }

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
}
