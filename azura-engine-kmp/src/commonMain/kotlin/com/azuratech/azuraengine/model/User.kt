package com.azuratech.azuraengine.model

import kotlinx.serialization.Serializable

/**
 * 🏫 Membership domain model for school context.
 */
@Serializable
data class Membership(
    val schoolName: String,
    val role: String,
    val assignedClassIds: List<String> = emptyList()
)

/**
 * 🤝 Friend connection domain model.
 */
@Serializable
data class FriendConnection(
    val friendName: String,
    val friendEmail: String,
    val status: String
)

/**
 * 👤 Global User domain model.
 */
@Serializable
data class User(
    val userId: String,
    val email: String,
    val name: String,
    val memberships: Map<String, Membership> = emptyMap(),
    val friends: Map<String, FriendConnection> = emptyMap(),
    val activeSchoolId: String? = null,
    val status: String = "PENDING",
    val isActive: Boolean = true,
    val activeClassId: String? = null,
    val role: String = "USER",
    val deviceId: String? = null,
    val createdAt: Long = 0L
) {
    /** The role in the currently active school workspace, or null if no active school. */
    val membershipRole: String? get() = memberships[activeSchoolId]?.role

    /** The name of the currently active school workspace, or null if no active school. */
    val schoolName: String? get() = memberships[activeSchoolId]?.schoolName

    /** Cek status pertemanan dengan guru lain */
    fun getFriendStatus(targetUserId: String): String? = friends[targetUserId]?.status

    /** Permission check for managing students (ADMIN or SUPER_ADMIN) */
    fun canManageStudents(): Boolean = role == "SUPER_ADMIN" || memberships[activeSchoolId]?.role == "ADMIN"
}
