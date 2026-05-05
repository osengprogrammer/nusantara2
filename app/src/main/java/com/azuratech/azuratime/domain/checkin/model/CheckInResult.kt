package com.azuratech.azuratime.domain.checkin.model

/**
 * Unified Result for Check-In operations.
 * Used across Scanner and Manual check-in flows.
 */
sealed class CheckInResult {
    data class Success(val name: String, val message: String) : CheckInResult()
    data class AlreadyCheckedIn(val name: String) : CheckInResult()
    data class Rejected(val name: String, val reason: String) : CheckInResult()
    object Unregistered : CheckInResult()
}
