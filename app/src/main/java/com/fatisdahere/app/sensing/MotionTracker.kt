package com.fatisdahere.app.sensing

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.fatisdahere.app.positioning.MotionSummary
import kotlin.math.sqrt

/**
 * PDR (Pedestrian Dead Reckoning) sederhana dengan akselerometer: menghitung langkah selama
 * scan Wi-Fi, lalu perkiraan perpindahan = langkah x panjang langkah.
 *
 * Dipilih akselerometer (bukan sensor step counter) karena tidak butuh izin
 * ACTIVITY_RECOGNITION dan tersedia di hampir semua HP.
 *
 * Cara mendeteksi langkah:
 *  - besar percepatan |a| = sqrt(x² + y² + z²); saat diam ≈ 9,8 m/s² (gravitasi)
 *  - |a| dihaluskan (low-pass) supaya getaran tangan kecil tidak terhitung
 *  - satu langkah = |a| naik melewati AMBANG_ATAS lalu turun lagi di bawah AMBANG_BAWAH,
 *    dengan jarak antarlangkah minimal JEDA_MIN_MS
 *
 * Pemakaian: mulai() sebelum scan, selesai() sesudahnya (di thread utama).
 */
class MotionTracker(context: Context) {
    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val akselerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val tersedia: Boolean get() = akselerometer != null

    private var langkah = 0
    private var halus = SensorManager.GRAVITY_EARTH.toDouble()
    private var diPuncak = false
    private var waktuLangkahTerakhir = 0L
    private var aktif = false

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0].toDouble()
            val y = event.values[1].toDouble()
            val z = event.values[2].toDouble()
            val besar = sqrt(x * x + y * y + z * z)
            halus = ALPHA * halus + (1 - ALPHA) * besar

            val sekarang = event.timestamp / 1_000_000 // ns -> ms
            if (!diPuncak && halus > AMBANG_ATAS && sekarang - waktuLangkahTerakhir >= JEDA_MIN_MS) {
                diPuncak = true
                langkah++
                waktuLangkahTerakhir = sekarang
            } else if (diPuncak && halus < AMBANG_BAWAH) {
                diPuncak = false
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    /** Mulai menghitung langkah dari nol. */
    fun mulai() {
        berhentiMendengar()
        langkah = 0
        halus = SensorManager.GRAVITY_EARTH.toDouble()
        diPuncak = false
        waktuLangkahTerakhir = 0L
        val sensor = akselerometer ?: return
        aktif = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    /** Berhenti dan kembalikan ringkasan gerakan sejak mulai(). */
    fun selesai(): MotionSummary {
        berhentiMendengar()
        return MotionSummary(
            steps = langkah,
            displacementM = langkah * PANJANG_LANGKAH_M,
            sensorAvailable = akselerometer != null
        )
    }

    private fun berhentiMendengar() {
        if (aktif) {
            sensorManager.unregisterListener(listener)
            aktif = false
        }
    }

    private companion object {
        /** Panjang langkah rata-rata orang dewasa (meter). */
        const val PANJANG_LANGKAH_M = 0.7

        /** Faktor penghalusan: makin besar makin halus (tapi makin lambat bereaksi). */
        const val ALPHA = 0.8

        /** Ambang puncak langkah: ±1,2 m/s² di atas gravitasi. */
        const val AMBANG_ATAS = 11.0

        /** Harus turun ke sini dulu sebelum langkah berikutnya bisa terhitung. */
        const val AMBANG_BAWAH = 9.9

        /** Orang berjalan paling cepat ±3 langkah per detik. */
        const val JEDA_MIN_MS = 300L
    }
}

/** Rekam gerakan selama [blok] berjalan (misalnya selama scan Wi-Fi). */
suspend fun <T> MotionTracker.rekam(blok: suspend () -> T): Pair<T, MotionSummary> {
    mulai()
    var hasil: MotionSummary? = null
    try {
        val nilai = blok()
        hasil = selesai()
        return nilai to hasil
    } finally {
        if (hasil == null) selesai()
    }
}
