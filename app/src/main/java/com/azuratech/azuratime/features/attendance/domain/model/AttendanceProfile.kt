package com.azuratech.azuratime.features.attendance.domain.model

import com.azuratech.azuratime.core.domain.model.SyncStatus
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 📊 Attendance Profile Domain Model
 * Represents a single attendance entry for a student on a specific date.
 * Following SSOT: Used by UI to render attendance matrix and lists.
 */
data class AttendanceProfile(
    val studentId: String,
    val studentName: String,
    val schoolId: String,
    val classId: String,
    val className: String,
    val date: LocalDate,
    val checkInTime: LocalDateTime?,
    val status: String,
    val syncStatus: SyncStatus,
    val timestamp: Long
)
