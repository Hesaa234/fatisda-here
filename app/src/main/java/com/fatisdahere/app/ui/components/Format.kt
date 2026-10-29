package com.fatisdahere.app.ui.components

import com.fatisdahere.app.data.PresenceStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/*
 * Pengubah waktu dan status dari data ke teks tampilan. Dipakai semua halaman
 * supaya format tanggal dan jam sama di mana-mana.
 */

private val LocaleId = Locale("id", "ID")

private fun format(pattern: String, ms: Long): String = SimpleDateFormat(pattern, LocaleId).format(Date(ms))

/** 13:00 */
fun formatJam(ms: Long): String = format("HH:mm", ms)

/** 13:00 - 15:00 */
fun formatRentangJam(startMs: Long, endMs: Long): String = "${formatJam(startMs)} - ${formatJam(endMs)}"

/** 16 Okt 2026 */
fun formatTanggal(ms: Long): String = format("d MMM yyyy", ms)

/** Rabu, 13:00 */
fun formatHariJam(ms: Long): String = format("EEEE, HH:mm", ms)

/** Rab, 21 Okt 13:00 */
fun formatHariTanggalJam(ms: Long): String = format("EEE, d MMM HH:mm", ms)

/** "HH:mm" pada hari yang sama dengan nowMs, dalam milidetik. */
fun jamHariIni(hhmm: String, nowMs: Long = System.currentTimeMillis()): Long {
    val (jam, menit) = hhmm.split(":").map { it.toInt() }
    return Calendar.getInstance().apply {
        timeInMillis = nowMs
        set(Calendar.HOUR_OF_DAY, jam)
        set(Calendar.MINUTE, menit)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun PresenceStatus.toUi(): AttendanceStatus = when (this) {
    PresenceStatus.HADIR -> AttendanceStatus.Hadir
    PresenceStatus.IZIN -> AttendanceStatus.Izin
    PresenceStatus.SAKIT -> AttendanceStatus.Sakit
}

/** Alpa dan Belum Absen = tidak ada dokumen kehadiran (null). */
fun AttendanceStatus.toPresence(): PresenceStatus? = when (this) {
    AttendanceStatus.Hadir -> PresenceStatus.HADIR
    AttendanceStatus.Izin -> PresenceStatus.IZIN
    AttendanceStatus.Sakit -> PresenceStatus.SAKIT
    AttendanceStatus.Alpa, AttendanceStatus.BelumAbsen -> null
}
