package com.azuratech.azuratime.core.util

import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.data.local.FaceEntity
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * AZURA ATTENDANCE SERVICE - THE LOGIC ENGINE
 * Menangani pembuatan record absensi dengan standar "Strategy 3".
 * Memastikan integritas data antara Personil, Guru, dan Sesi Kelas.
 */
object AttendanceService {

    /**
     * Membuat CheckInRecordEntity yang valid dan siap lapor.
     * * @param face Data personil (siswa/karyawan) yang terdeteksi.
     * @param teacherEmail Email admin/guru yang bertanggung jawab saat scan.
     * @param activeClassId UUID Kelas atau Sesi yang sedang aktif.
     * @param activeClassName Nama tampilan kelas (misal: "Garmen Shift A").
     * @param status Status kehadiran (H=Hadir, S=Sakit, I=Izin, A=Alpa).
     * @param attendanceDate Tanggal absensi (LocalDate).
     * @param checkInTime Waktu scan (LocalDateTime).
     */
    fun createRecord(
        face: FaceEntity,
        teacherEmail: String = "",
        activeClassId: String? = null,
        activeClassName: String? = null,
        status: String = "H",
        attendanceDate: LocalDate = LocalDate.now(),
        checkInTime: LocalDateTime? = LocalDateTime.now()
    ): CheckInRecordEntity {
        return CheckInRecordEntity(
            faceId = face.faceId, // UUID String dari FaceEntity
            name = face.name,

            // 🔥 Hubungkan ke konteks pengelola (Strategy 3)
            userId = teacherEmail,
            classId = activeClassId,
            className = activeClassName,

            // 🔥 Timing & Status dengan Null Safety
            attendanceDate = attendanceDate,
            checkInTime = checkInTime ?: LocalDateTime.now(),
            // 🔥 FIX: createdAt dihapus dari parameter instansiasi
            status = status
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