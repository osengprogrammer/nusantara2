package com.azuratech.azuratime.domain.model

/**
 * 🧬 FACE ENROLLMENT PROFILE - UI-focused biometric model
 * Used for listing and managing biometric enrollments.
 */
data class FaceEnrollmentProfile(
    val faceId: String,
    val studentId: String?,
    val studentName: String,
    val photoUri: String?,
    val enrollmentDate: Long,
    val syncStatus: SyncStatus
)
