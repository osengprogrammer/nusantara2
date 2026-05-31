package com.azuratech.azuratime.features.account.data.local

/**
 * 🏫 SCHOOL MEMBERSHIP (v3.2.1-ai-native)
 * Local entity for multi-tenant school access.
 */
data class SchoolMembership(
    val schoolName: String,
    val role: String, // "SUPER_ADMIN", "ADMIN", "SUPERVISOR", "USER"
    val status: String = "ACTIVE", // "ACTIVE", "PENDING", "REJECTED"
    val assignedClassIds: List<String> = emptyList(), // 🔥 Daftar kelas yang diamanahi
)

fun SchoolMembership.toDomain() = com.azuratech.azuratime.features.account.domain.model.SchoolMembership(
    schoolName = schoolName,
    role = role,
    status = status,
    assignedClassIds = assignedClassIds,
)
