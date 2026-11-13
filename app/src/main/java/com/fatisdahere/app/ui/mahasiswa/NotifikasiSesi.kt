package com.fatisdahere.app.ui.mahasiswa

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Notifikasi "sesi absen dibuka" untuk mahasiswa.
 *
 * Cara kerja: selama aplikasi berjalan (dibuka, atau masih di latar belakang / recent apps),
 * MahasiswaApp memantau koleksi sessions secara realtime. Kalau dosen membuka sesi baru di
 * matkul mahasiswa, notifikasi ini muncul.
 *
 * Keterbatasan: kalau aplikasi sudah ditutup penuh, notifikasi tidak muncul. Notifikasi
 * push sungguhan butuh server (Firebase Cloud Functions + FCM) yang memerlukan paket berbayar.
 */
object NotifikasiSesi {
    private const val CHANNEL_ID = "sesi_absen"

    /** true kalau perlu meminta izin notifikasi dulu (Android 13 ke atas). */
    fun perluIzin(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    private fun buatChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Sesi absen dibuka", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Pemberitahuan saat dosen membuka sesi absen di mata kuliahmu"
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** Tampilkan notifikasi; diam saja kalau izin belum diberikan. */
    fun tampilkan(context: Context, id: Int, judul: String, isi: String) {
        if (perluIzin(context)) return
        buatChannel(context)
        // Ketuk notifikasi -> buka aplikasi
        val buka = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pending = buka?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(judul)
            .setContentText(isi)
            .setStyle(NotificationCompat.BigTextStyle().bigText(isi))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .apply { if (pending != null) setContentIntent(pending) }
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notif)
        } catch (e: SecurityException) {
            // izin dicabut di tengah jalan: abaikan
        }
    }
}
