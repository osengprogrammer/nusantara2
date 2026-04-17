package com.azuratech.azuratime.data.local

/**
 * Wadah untuk membandingkan tabrakan data antara lokal (HP) dan cloud (Firestore)
 */
data class AttendanceConflict(
    val local: CheckInRecordEntity,
    val cloud: CheckInRecordEntity
)
