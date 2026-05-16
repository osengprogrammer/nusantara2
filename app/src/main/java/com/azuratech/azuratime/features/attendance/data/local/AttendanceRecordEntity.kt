package com.azuratech.azuratime.features.attendance.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Entity(
    tableName = "check_in_records",
    indices = [Index(value = ["schoolId"])]
)
data class AttendanceRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val schoolId: String = "",
    @ColumnInfo(name = "faceId") val studentId: String,
    val name: String,
    val userId: String, 
    val status: String, 
    val attendanceDate: LocalDate,
    val attendanceTime: LocalDateTime? = null,
    val classId: String? = null,
    val className: String? = null,
    val isSynced: Boolean = false,
    val timestamp: Long = System.currentTimeMillis() 
) {
    // 1. FOR EXPORT & MATH: Converts the Long back to LocalDateTime
    val createdAtDateTime: LocalDateTime
        get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())

    // 2. FOR UI: A ready-to-use formatted string
    val displayCreatedAt: String
        get() {
            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
            return createdAtDateTime.format(formatter)
        }

    fun toDomain(): AttendanceRecord {
        return AttendanceRecord(
            recordId = id,
            studentId = studentId,
            studentName = name,
            classId = classId ?: "",
            className = className ?: "",
            schoolId = schoolId,
            timestamp = timestamp,
            status = AttendanceStatus.fromCode(status),
            isSynced = isSynced,
            teacherEmail = userId
        )
    }

    fun toProfile() = toDomain()

    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "schoolId" to schoolId,
            "faceId" to studentId,
            "name" to name,
            "teacherEmail" to userId,
            "status" to status,
            "attendanceDate" to attendanceDate.toString(),
            "attendanceTime" to attendanceTime?.toString(),
            "classId" to classId,
            "className" to className,
            "timestamp" to FieldValue.serverTimestamp(), 
            "createdAt" to timestamp,
            "isSynced" to true
        )
    }

    companion object {
        fun fromDomain(domain: AttendanceRecord): AttendanceRecordEntity {
            val dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(domain.timestamp),
                ZoneId.systemDefault()
            )
            return AttendanceRecordEntity(
                id = domain.recordId,
                schoolId = domain.schoolId,
                studentId = domain.studentId,
                name = domain.studentName,
                userId = domain.teacherEmail,
                status = domain.status.toCode(),
                attendanceDate = dateTime.toLocalDate(),
                attendanceTime = dateTime,
                classId = domain.classId,
                className = domain.className,
                isSynced = domain.isSynced,
                timestamp = domain.timestamp
            )
        }
    }
}

/**
 * 🔥 EXTENSION: DARI CLOUD KE LOKAL (SNAPSHOT PARSER)
 * Diletakkan di luar class agar bisa diakses langsung oleh FirestoreManager.
 */
fun com.google.firebase.firestore.DocumentSnapshot.toAttendanceRecordEntity(schoolId: String): AttendanceRecordEntity? {
    return try {
        val dateStr = getString("attendanceDate") ?: java.time.LocalDate.now().toString()
        val timeStr = getString("attendanceTime") ?: getString("checkInTime")
        
        AttendanceRecordEntity(
            id = id, 
            schoolId = schoolId,
            studentId = getString("faceId") ?: "",
            name = getString("name") ?: "Siswa",
            userId = getString("teacherEmail") ?: "",
            status = getString("status") ?: "Hadir",
            attendanceDate = java.time.LocalDate.parse(dateStr),
            attendanceTime = timeStr?.let { java.time.LocalDateTime.parse(it) },
            classId = getString("classId"),
            className = getString("className"),
            isSynced = true,
            timestamp = getTimestamp("createdAt")?.toDate()?.time ?: getLong("createdAt") ?: System.currentTimeMillis()
        )
    } catch (e: Exception) {
        null 
    }
}
