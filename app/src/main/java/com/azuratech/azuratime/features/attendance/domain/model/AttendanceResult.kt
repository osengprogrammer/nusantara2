package com.azuratech.azuratime.features.attendance.domain.model

/**
 * Unified Result for Check-In operations.
 * Used across Scanner and Manual check-in flows.
 */
sealed class AttendanceResult {
    data class Success(val name: String, val message: String) : AttendanceResult()
    data class AlreadyCheckedIn(val name: String) : AttendanceResult()
    data class Rejected(val name: String, val reason: String) : AttendanceResult()
    object Unregistered : AttendanceResult()
}
