package com.fatisdahere.app.di

import android.content.Context
import com.fatisdahere.app.data.AttendanceRepository
import com.fatisdahere.app.data.AuthRepository
import com.fatisdahere.app.data.CourseRepository
import com.fatisdahere.app.data.FingerprintLoader
import com.fatisdahere.app.data.RoomRepository
import com.fatisdahere.app.data.SessionRepository
import com.fatisdahere.app.data.UserRepository
import com.fatisdahere.app.data.fake.FakeAttendanceRepository
import com.fatisdahere.app.data.fake.FakeAuthRepository
import com.fatisdahere.app.data.fake.FakeCourseRepository
import com.fatisdahere.app.data.fake.FakeDatabase
import com.fatisdahere.app.data.fake.FakeRoomRepository
import com.fatisdahere.app.data.fake.FakeSessionRepository
import com.fatisdahere.app.data.fake.FakeUserRepository
import com.fatisdahere.app.positioning.FingerprintData
import com.fatisdahere.app.positioning.KnnPositioningEngine
import com.fatisdahere.app.positioning.PositioningParams
import com.fatisdahere.app.sensing.AndroidWifiScanner

/** Satu tempat untuk membuat semua objek yang dipakai bersama oleh layar. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    // ---------- Positioning (KNN) ----------
    val fingerprint: FingerprintData by lazy { FingerprintLoader.load(appContext) }
    val positioningParams: PositioningParams by lazy { PositioningParams.default() }
    val positioningEngine: KnnPositioningEngine by lazy { KnnPositioningEngine(fingerprint, positioningParams) }
    val wifiScanner: AndroidWifiScanner by lazy { AndroidWifiScanner(appContext, fingerprint.ssid) }

    // ---------- Data ----------
    // Sekarang memakai data dummy di memori (data/fake/). Untuk pindah ke Firebase,
    // ganti isi baris-baris di bawah dengan versi Firebase; layar tidak perlu diubah.
    private val fakeDb by lazy { FakeDatabase() }

    val authRepository: AuthRepository by lazy { FakeAuthRepository(fakeDb) }
    val userRepository: UserRepository by lazy { FakeUserRepository(fakeDb) }
    val roomRepository: RoomRepository by lazy { FakeRoomRepository(fakeDb) }
    val courseRepository: CourseRepository by lazy { FakeCourseRepository(fakeDb) }
    val sessionRepository: SessionRepository by lazy { FakeSessionRepository(fakeDb) }
    val attendanceRepository: AttendanceRepository by lazy { FakeAttendanceRepository(fakeDb) }
}
