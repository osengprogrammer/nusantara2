package com.azuratech.azuratime.features.attendance.domain.model

/**
 * Domain Model for Attendance Conflict.
 * Represents a collision between local and cloud check-in records.
 */
data class AttendanceConflict(
    val conflictId: String,
    val local: AttendanceRecord,
    val cloud: AttendanceRecord,
)
