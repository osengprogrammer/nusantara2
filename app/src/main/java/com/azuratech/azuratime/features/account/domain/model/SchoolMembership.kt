package com.azuratech.azuratime.features.account.domain.model

/**
 * 🏫 ASSIGNED SCHOOL MEMBERSHIP (v3.2.1-ai-native)
 * Domain model representing an account's assignment to a specific school.
 * Defines the relationship: [Account] + [School] = [Role].
 */
data class SchoolMembership(
    val schoolName: String,
    val role: String, // 🔥 Role IN THIS SPECIFIC SCHOOL (ADMIN, SUPERVISOR, USER)
    val status: String = "ACTIVE",
    val assignments: List<TeacherAssignment> = emptyList(),
)
