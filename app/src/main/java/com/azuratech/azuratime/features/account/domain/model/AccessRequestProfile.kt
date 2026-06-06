package com.azuratech.azuratime.features.account.domain.model

import com.azuratech.azuratime.core.domain.model.SyncStatus

/**
 * 🎫 Access Request Profile (Domain Model)
 * Represents a request made by an account to join or leave a school.
 */
data class AccessRequestProfile(
    val requestId: String,
    val accountId: String,
    val schoolId: String,
    val schoolName: String,
    val status: AccessRequestStatus,
    val assignedRole: String = "USER",
    val syncStatus: SyncStatus,
    val createdAt: Long,
    val updatedAt: Long,
)
