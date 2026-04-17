package com.azuratech.azuratime.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
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
data class CheckInRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val schoolId: String = "",
    val faceId: String, 
    val name: String,
    val userId: String, 
    val status: String, 
    val attendanceDate: LocalDate,
    val checkInTime: LocalDateTime? = null,
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

    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "schoolId" to schoolId,
            "faceId" to faceId,
            "name" to name,
            "teacherEmail" to userId,
            "status" to status,
            "attendanceDate" to attendanceDate.toString(),
            "checkInTime" to checkInTime?.toString(),
            "classId" to classId,
            "className" to className,
            "timestamp" to FieldValue.serverTimestamp(), 
            "createdAt" to timestamp,
            "isSynced" to true
        )
    }
}

/**
 * 🔥 EXTENSION: DARI CLOUD KE LOKAL (SNAPSHOT PARSER)
 * Diletakkan di luar class agar bisa diakses langsung oleh FirestoreManager.
 */
fun com.google.firebase.firestore.DocumentSnapshot.toCheckInRecordEntity(schoolId: String): CheckInRecordEntity? {
    return try {
        val dateStr = getString("attendanceDate") ?: java.time.LocalDate.now().toString()
        val timeStr = getString("checkInTime")
        
        CheckInRecordEntity(
            id = id, 
            schoolId = schoolId,
            faceId = getString("faceId") ?: "",
            name = getString("name") ?: "Siswa",
            userId = getString("teacherEmail") ?: "",
            status = getString("status") ?: "Hadir",
            attendanceDate = java.time.LocalDate.parse(dateStr),
            checkInTime = timeStr?.let { java.time.LocalDateTime.parse(it) },
            classId = getString("classId"),
            className = getString("className"),
            isSynced = true,
            timestamp = getLong("createdAt") ?: System.currentTimeMillis()
        )
    } catch (e: Exception) {
        null 
    }
}