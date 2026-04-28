package com.azuratech.azuratime.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 🏫 Multi-Tenant Membership: Hak akses user di dalam candinya sendiri atau candi teman.
 */
data class Membership(
    val schoolName: String,
    val role: String, // "ADMIN", "TEACHER", "SUPER_USER"
    val assignedClassIds: List<String> = emptyList() // 🔥 Daftar kelas yang diamanahi
)

/**
 * 🤝 SEDULURAN (FRIENDSHIP): Menyimpan status koneksi antar Guru.
 */
data class FriendConnection(
    val friendName: String,
    val friendEmail: String,
    val status: String // "REQUEST_SENT", "PENDING_APPROVAL", "FRIENDS"
)

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey
    val userId: String,

    val email: String,
    val name: String,

    // 🔥 MULTI-TENANT: Map of schoolId → Membership (stored as JSON via Converter)
    val memberships: Map<String, Membership> = emptyMap(),

    // 🤝 JARINGAN PERTEMANAN: Map of friendUserId → FriendConnection
    val friends: Map<String, FriendConnection> = emptyMap(),

    // The school workspace currently active on this device
    val activeSchoolId: String? = null,

    val status: String = "PENDING", // ACTIVE, PENDING
    val isActive: Boolean = true,

    // Class currently selected for scanning (UUID String)
    val activeClassId: String? = null,

    val role: String = "USER", // 🔥 GLOBAL ROLE: SUPER_ADMIN, ADMIN, USER

    val deviceId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    // =====================================================
    // 🔑 COMPUTED HELPERS
    // =====================================================

    /** The role in the currently active school workspace, or null if no active school. */
    val membershipRole: String? get() = memberships[activeSchoolId]?.role

    /** The name of the currently active school workspace, or null if no active school. */
    val schoolName: String? get() = memberships[activeSchoolId]?.schoolName

    /** Cek status pertemanan dengan guru lain */
    fun getFriendStatus(targetUserId: String): String? = friends[targetUserId]?.status
}