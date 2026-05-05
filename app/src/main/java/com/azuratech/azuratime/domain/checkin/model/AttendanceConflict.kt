package com.azuratech.azuratime.domain.checkin.model

/**
 * Domain Model for Attendance Conflict.
 * Represents a collision between local and cloud check-in records.
 */
data class AttendanceConflict(
    val conflictId: String,
    val local: CheckInRecord,
    val cloud: CheckInRecord
)
