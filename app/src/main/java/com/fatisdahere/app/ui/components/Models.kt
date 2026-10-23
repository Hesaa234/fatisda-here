package com.fatisdahere.app.ui.components

import androidx.compose.ui.graphics.Color
import com.fatisdahere.app.ui.theme.StatusDanger
import com.fatisdahere.app.ui.theme.StatusSuccess
import com.fatisdahere.app.ui.theme.StatusWarning
import com.fatisdahere.app.ui.theme.UnsNavy

enum class AttendanceStatus(val label: String, val color: Color) {
    Hadir("Hadir", StatusSuccess),
    Izin("Izin", StatusWarning),
    Sakit("Sakit", StatusWarning),
    Alpa("Alpa", StatusDanger),
    BelumAbsen("Belum Absen", UnsNavy)
}

data class SessionUi(
    val code: String,
    val name: String,
    val time: String,
    val room: String,
    val lecturer: String
) {
    val title get() = "($code) - $name"
}

data class AttendanceRecord(
    val meeting: Int,
    val status: AttendanceStatus,
    val room: String,
    val time: String
)

enum class ResultState { Verified, Rejected }
