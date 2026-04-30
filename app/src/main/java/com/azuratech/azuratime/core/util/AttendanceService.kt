package com.azuratech.azuratime.core.util

import com.azuratech.azuratime.domain.checkin.model.CheckInRecord
import com.azuratech.azuratime.domain.checkin.model.CheckInStatus
import com.azuratech.azuratime.data.local.FaceEntity
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * AZURA ATTENDANCE SERVICE - THE LOGIC ENGINE
 * Menangani pembuatan record absensi dengan standar "Strategy 3".
 * Memastikan integritas data antara Personil, Guru, dan Sesi Kelas.
 */
object AttendanceService {

    /**
     * Membuat CheckInRecord yang valid dan siap lapor.
     */
    fun createRecord(
        face: FaceEntity,
        teacherEmail: String = "",
        activeClassId: String? = null,
        activeClassName: String? = null,
        status: String = "H",
        attendanceDate: LocalDate = LocalDate.now(),
        checkInTime: LocalDateTime? = LocalDateTime.now()
    ): CheckInRecord {
        val dateTime = checkInTime ?: LocalDateTime.now()
        val timestamp = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return CheckInRecord(
            recordId = java.util.UUID.randomUUID().toString(),
            studentId = face.faceId, 
            studentName = face.name,
            teacherEmail = teacherEmail,
            classId = activeClassId ?: "",
            className = activeClassName ?: "",
            schoolId = face.schoolId,
            status = CheckInStatus.fromCode(status),
            timestamp = timestamp,
            isSynced = false
        )
    }

    /**
     * Logika untuk menentukan apakah personil terlambat.
     * Menggunakan threshold (batas waktu) yang bisa dikonfigurasi.
     */
    fun isLate(
        checkInTime: LocalDateTime?,
        thresholdHour: Int = 7,
        thresholdMinute: Int = 30
    ): Boolean {
        if (checkInTime == null) return false

        val scanTime = checkInTime.toLocalTime()
        val limitTime = LocalTime.of(thresholdHour, thresholdMinute)

        return scanTime.isAfter(limitTime)
    }

    /**
     * Kalkulasi durasi kerja (untuk mode Pabrik/Garmen).
     * Menghitung selisih antara Scan Masuk dan Scan Keluar.
     */
    fun calculateWorkDuration(checkIn: LocalDateTime, checkOut: LocalDateTime): Long {
        return Duration.between(checkIn, checkOut).toMinutes()
    }
}