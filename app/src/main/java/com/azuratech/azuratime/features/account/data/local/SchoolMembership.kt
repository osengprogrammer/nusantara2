package com.azuratech.azuratime.features.account.data.local
import com.azuratech.azuratime.core.domain.model.TeacherAssignment

/**
 * 🏫 SCHOOL MEMBERSHIP (v3.2.1-ai-native)
 * Local entity for multi-tenant school access.
 */
data class SchoolMembership(
    val schoolName: String,
    val role: String, // "SUPER_ADMIN", "ADMIN", "SUPERVISOR", "USER"
    val status: String = "ACTIVE", // "ACTIVE", "PENDING", "REJECTED"
    val assignments: List<com.azuratech.azuratime.core.domain.model.TeacherAssignment> = emptyList(), // 🔥 Matrix Assignment
)

fun SchoolMembership.toDomain() = com.azuratech.azuratime.features.account.domain.model.SchoolMembership(
    schoolName = schoolName,
    role = role,
    status = status,
    assignments = assignments,
)
