package com.azuratech.azuratime.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.azuratech.azuratime.domain.model.AccessRequestStatus
import com.azuratech.azuratime.domain.model.SyncStatus

/**
 * 🎫 Access Request Entity
 * Represents a request made by a user to join or leave a school.
 * Follows SSOT: Local-first source for access management.
 */
@Entity(tableName = "access_requests")
data class AccessRequestEntity(
    @PrimaryKey
    val requestId: String,
    val requesterId: String,
    val schoolId: String,
    val schoolName: String,
    val status: AccessRequestStatus,
    val syncStatus: SyncStatus,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Domain projection for Access Request
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

fun AccessRequestEntity.toProfile() = AccessRequestProfile(
    requestId = requestId,
    requesterId = requesterId,
    schoolId = schoolId,
    schoolName = schoolName,
    status = status,
    syncStatus = syncStatus,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun AccessRequestProfile.toEntity() = AccessRequestEntity(
    requestId = requestId,
    requesterId = requesterId,
    schoolId = schoolId,
    schoolName = schoolName,
    status = status,
    syncStatus = syncStatus,
    createdAt = createdAt,
    updatedAt = updatedAt
)
