package com.azuratech.azuratime.features.account.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.azuratech.azuratime.features.account.domain.model.AccessRequestStatus
import com.azuratech.azuratime.core.domain.model.SyncStatus
import com.azuratech.azuratime.features.account.domain.model.AccessRequestProfile

/**
 * 🎫 Access Request Entity
 * Represents a request made by an account to join or leave a school.
 * Follows SSOT: Local-first source for access management.
 */
@Entity(tableName = "access_requests")
data class AccessRequestEntity(
    @PrimaryKey
    val requestId: String,
    val accountId: String,
    val schoolId: String,
    val schoolName: String,
    val status: AccessRequestStatus,
    val assignedRole: String = "USER",
    val syncStatus: SyncStatus,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * Domain projection for Access Request
 */
fun AccessRequestEntity.toProfile() = AccessRequestProfile(
    requestId = requestId,
    accountId = accountId,
    schoolId = schoolId,
    schoolName = schoolName,
    status = status,
    assignedRole = assignedRole,
    syncStatus = syncStatus,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun AccessRequestProfile.toEntity() = AccessRequestEntity(
    requestId = requestId,
    accountId = accountId,
    schoolId = schoolId,
    schoolName = schoolName,
    status = status,
    assignedRole = assignedRole,
    syncStatus = syncStatus,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
