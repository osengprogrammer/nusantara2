package com.azuratech.azuratime.features.account.domain.model

import com.azuratech.azuratime.core.domain.model.SyncStatus

/**
 * 🎫 Access Request Profile (Domain Model)
 * Represents a request made by a user to join or leave a school.
 */
data class AccessRequestProfile(
    val requestId: String,
    val requesterId: String,
    val schoolId: String,
    val schoolName: String,
    val status: AccessRequestStatus,
    val syncStatus: SyncStatus,
    val createdAt: Long,
    val updatedAt: Long
)
