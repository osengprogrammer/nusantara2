package com.azuratech.azuratime.core.util

import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
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
     * Membuat AttendanceRecord yang valid dan siap lapor.
     */
    fun createRecord(
        biometric: StudentBiometricEntity,
        accountEmail: String = "",
        activeClassId: String? = null,
        activeClassName: String? = null,
        status: String = "H",
        attendanceTime: LocalDateTime? = LocalDateTime.now()
    ): AttendanceRecord {
        val dateTime = attendanceTime ?: LocalDateTime.now()
        val timestamp = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return AttendanceRecord(
            recordId = java.util.UUID.randomUUID().toString(),
            studentId = biometric.studentId, 
            studentName = biometric.name,
            accountEmail = accountEmail,
            classId = activeClassId ?: "",
            className = activeClassName ?: "",
            schoolId = biometric.schoolId,
            status = AttendanceStatus.fromCode(status),
            timestamp = timestamp,
            isSynced = false
        )
    }

    /**
     * Logika untuk menentukan apakah personil terlambat.
     * Menggunakan threshold (batas waktu) yang bisa dikonfigurasi.
     */
    fun isLate(
        attendanceTime: LocalDateTime?,
        thresholdHour: Int = 7,
        thresholdMinute: Int = 30
    ): Boolean {
        if (attendanceTime == null) return false

        val scanTime = attendanceTime.toLocalTime()
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
