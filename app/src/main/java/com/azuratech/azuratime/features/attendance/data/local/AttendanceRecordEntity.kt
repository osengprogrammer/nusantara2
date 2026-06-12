package com.azuratech.azuratime.features.attendance.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus
import com.azuratech.azuratime.features.session.domain.model.SessionType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Entity(
    tableName = "check_in_records",
    indices = [Index(value = ["schoolId"])],
)
data class AttendanceRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val schoolId: String = "",
    @ColumnInfo(name = "studentId") val studentId: String,
    val name: String,
    val accountEmail: String, // 🔥 Unified Identity: Email of the account that recorded this
    val status: String,
    val attendanceDate: LocalDate,
    val attendanceTime: LocalDateTime? = null,
    val classId: String? = null,
    val className: String? = null,
    @ColumnInfo(name = "sessionId", defaultValue = "") val sessionId: String = "",
    val sessionType: SessionType = SessionType.ACADEMIC, // ✅ Denormalized Tier
    val isSynced: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
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
            sessionId = sessionId,
            sessionType = sessionType,
            isSynced = isSynced,
            accountEmail = accountEmail,
        )
    }

    fun toProfile() = toDomain()

    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "schoolId" to schoolId,
            "studentId" to studentId, // 🔥 Unified Identity
            "name" to name,
            "accountEmail" to accountEmail,
            "status" to status,
            "attendanceDate" to attendanceDate.toString(),
            "attendanceTime" to attendanceTime?.toString(),
            "classId" to classId,
            "className" to className,
            "sessionId" to sessionId,
            "sessionType" to sessionType.name,
            "timestamp" to FieldValue.serverTimestamp(),
            "createdAt" to timestamp,
            "isSynced" to true,
        )
    }

    companion object {
        fun fromDomain(domain: AttendanceRecord): AttendanceRecordEntity {
            val dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(domain.timestamp),
                ZoneId.systemDefault(),
            )
            return AttendanceRecordEntity(
                id = domain.recordId,
                schoolId = domain.schoolId,
                studentId = domain.studentId,
                name = domain.studentName,
                accountEmail = domain.accountEmail,
                status = domain.status.toCode(),
                attendanceDate = dateTime.toLocalDate(), // The exact day the epoch represents in local timezone
                attendanceTime = dateTime,
                classId = domain.classId,
                className = domain.className,
                sessionId = domain.sessionId ?: "",
                sessionType = domain.sessionType,
                isSynced = domain.isSynced,
                timestamp = domain.timestamp,
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
        // 1. Identify primary timestamp
        // 🔥 AI Native: Prioritize 'createdAt' (actual attendance time) over 'timestamp' (sync time)
        val longTimestamp = getLong("createdAt")
        val rawTimestamp = getTimestamp("timestamp")

        val finalTimestamp = longTimestamp
            ?: rawTimestamp?.toDate()?.time
            ?: System.currentTimeMillis()

        // 2. Parse Date & Time (Resilient)
        val dateStr = getString("attendanceDate") ?: getString("date")
        val timeStr = getString("attendanceTime") ?: getString("checkInTime") ?: getString("time")

        val parsedDate = try {
            if (dateStr != null) {
                LocalDate.parse(dateStr)
            } else {
                val instant = Instant.ofEpochMilli(finalTimestamp)
                LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate()
            }
        } catch (e: Exception) {
            LocalDate.now()
        }

        val parsedTime = try {
            if (timeStr != null) {
                LocalDateTime.parse(timeStr)
            } else {
                val instant = Instant.ofEpochMilli(finalTimestamp)
                LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            }
        } catch (e: Exception) {
            null
        }

        val sessionTypeStr = getString("sessionType")
        val parsedSessionType = try {
            if (sessionTypeStr != null) SessionType.valueOf(sessionTypeStr) else SessionType.ACADEMIC
        } catch (e: Exception) {
            SessionType.ACADEMIC
        }

        AttendanceRecordEntity(
            id = id,
            schoolId = getString("schoolId") ?: schoolId,
            studentId = getString("studentId") ?: getString("faceId") ?: "",
            name = getString("name") ?: getString("studentName") ?: "Siswa",
            accountEmail = getString("accountEmail") ?: getString("accountId") ?: "unknown",
            status = getString("status") ?: "H",
            attendanceDate = parsedDate,
            attendanceTime = parsedTime,
            classId = getString("classId"),
            className = getString("className"),
            sessionId = getString("sessionId") ?: "",
            sessionType = parsedSessionType,
            isSynced = true,
            timestamp = finalTimestamp,
        )
    } catch (e: Exception) {
        android.util.Log.e("AttendanceEntity", "❌ Error parsing snapshot: ${e.message}")
        null
    }
}
