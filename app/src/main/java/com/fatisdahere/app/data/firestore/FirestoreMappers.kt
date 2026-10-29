package com.fatisdahere.app.data.firestore

import com.fatisdahere.app.data.AppUser
import com.fatisdahere.app.data.Attempt
import com.fatisdahere.app.data.Attendance
import com.fatisdahere.app.data.AttendanceMethod
import com.fatisdahere.app.data.AttendanceSession
import com.fatisdahere.app.data.Course
import com.fatisdahere.app.data.PresenceStatus
import com.fatisdahere.app.data.Role
import com.fatisdahere.app.data.Room
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/*
 * Pengubah dokumen Firestore <-> model data. Nama field mengikuti docs/ERD.md.
 * Dokumen yang field wajibnya kosong/salah tipe dilewati (hasilnya null), supaya
 * satu data rusak di Console tidak membuat aplikasi crash.
 */

private fun DocumentSnapshot.ms(field: String): Long? = getTimestamp(field)?.toDate()?.time

internal fun ts(ms: Long): Timestamp = Timestamp(Date(ms))

fun DocumentSnapshot.toAppUser(): AppUser? {
    if (!exists()) return null
    return AppUser(
        uid = id,
        name = getString("name") ?: getString("email") ?: id,
        email = getString("email") ?: "",
        role = Role.from(getString("role")) ?: return null,
        nim = getString("nim"),
        nip = getString("nip"),
    )
}

internal fun AppUser.toMap(): Map<String, Any> = buildMap {
    put("name", name)
    put("email", email)
    put("role", role.value)
    nim?.let { put("nim", it) }
    nip?.let { put("nip", it) }
}

internal fun DocumentSnapshot.toRoom(): Room? {
    return Room(
        id = id,
        name = getString("name") ?: id,
        building = getString("building") ?: "",
        floor = getLong("floor")?.toInt() ?: 0,
        mainApPrefix = getString("mainApPrefix") ?: "",
    )
}

internal fun DocumentSnapshot.toCourse(): Course? {
    return Course(
        id = id,
        code = getString("code") ?: return null,
        name = getString("name") ?: return null,
        lecturerId = getString("lecturerId") ?: return null,
        roomId = getString("roomId") ?: return null,
        studentIds = (get("studentIds") as? List<*>)?.filterIsInstance<String>().orEmpty(),
    )
}

internal fun Course.toMap(): Map<String, Any> = mapOf(
    "code" to code,
    "name" to name,
    "lecturerId" to lecturerId,
    "roomId" to roomId,
    "studentIds" to studentIds,
)

internal fun DocumentSnapshot.toSession(): AttendanceSession? {
    return AttendanceSession(
        id = id,
        courseId = getString("courseId") ?: return null,
        lecturerId = getString("lecturerId") ?: return null,
        roomId = getString("roomId") ?: return null,
        startAtMs = ms("startAt") ?: return null,
        endAtMs = ms("endAt") ?: return null,
        closedAtMs = ms("closedAt"),
    )
}

internal fun AttendanceSession.toMap(): Map<String, Any> = buildMap {
    put("courseId", courseId)
    put("lecturerId", lecturerId)
    put("roomId", roomId)
    put("startAt", ts(startAtMs))
    put("endAt", ts(endAtMs))
    closedAtMs?.let { put("closedAt", ts(it)) }
}

internal fun DocumentSnapshot.toAttendance(): Attendance? {
    return Attendance(
        sessionId = getString("sessionId") ?: return null,
        courseId = getString("courseId") ?: return null,
        studentId = getString("studentId") ?: return null,
        status = PresenceStatus.from(getString("status")) ?: return null,
        method = AttendanceMethod.from(getString("method")) ?: AttendanceMethod.AUTO,
        detectedRoomId = getString("detectedRoomId"),
        knnDistance = getDouble("knnDistance"),
        validScans = getLong("validScans")?.toInt(),
        displacementM = getDouble("displacementM"),
        fingerprintVersion = getString("fingerprintVersion"),
        deviceModel = getString("deviceModel"),
        createdAtMs = ms("createdAt") ?: 0L,
    )
}

/** Field kosong tidak ditulis. */
internal fun Attendance.toMap(): Map<String, Any> = buildMap {
    put("sessionId", sessionId)
    put("courseId", courseId)
    put("studentId", studentId)
    put("status", status.value)
    put("method", method.value)
    detectedRoomId?.let { put("detectedRoomId", it) }
    knnDistance?.let { put("knnDistance", it) }
    validScans?.let { put("validScans", it) }
    displacementM?.let { put("displacementM", it) }
    fingerprintVersion?.let { put("fingerprintVersion", it) }
    deviceModel?.let { put("deviceModel", it) }
    put("createdAt", ts(createdAtMs))
}

internal fun Attempt.toMap(): Map<String, Any> = buildMap {
    put("sessionId", sessionId)
    put("studentId", studentId)
    put("accepted", accepted)
    reason?.let { put("reason", it) }
    knnDistance?.let { put("knnDistance", it) }
    validScans?.let { put("validScans", it) }
    displacementM?.let { put("displacementM", it) }
    put("createdAt", ts(createdAtMs))
}
