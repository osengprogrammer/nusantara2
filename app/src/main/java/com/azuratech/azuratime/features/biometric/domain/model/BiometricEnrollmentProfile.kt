package com.azuratech.azuratime.features.biometric.domain.model

import com.azuratech.azuratime.core.domain.model.SyncStatus

/**
 * 🧬 BIOMETRIC ENROLLMENT PROFILE - UI model for student enrollments
 */
data class BiometricEnrollmentProfile(
    val studentId: String,
    val studentName: String,
    val photoUri: String?,
    val enrollmentDate: Long,
    val syncStatus: SyncStatus,
)
