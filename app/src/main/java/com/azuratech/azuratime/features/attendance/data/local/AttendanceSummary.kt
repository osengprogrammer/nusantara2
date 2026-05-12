package com.azuratech.azuratime.features.attendance.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.azuratech.azuratime.domain.model.AttendanceProfile
import com.azuratech.azuratime.domain.model.SyncStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity(
    tableName = "attendance",
    indices = [Index(value = ["schoolId"])]
)
data class AttendanceSummary(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val schoolId: String,
    val studentId: String,
    val name: String,
    val status: String,
    val attendanceDate: LocalDate,
    val checkInTime: LocalDateTime? = null,
    val classId: String? = null,
    val className: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val timestamp: Long = System.currentTimeMillis()
)

fun AttendanceSummary.toProfile() = AttendanceProfile(
    studentId = studentId,
    studentName = name,
    schoolId = schoolId,
    classId = classId ?: "",
    className = className ?: "",
    date = attendanceDate,
    checkInTime = checkInTime,
    status = status,
    syncStatus = syncStatus,
    timestamp = timestamp
)

typealias AttendanceEntity = AttendanceSummary
