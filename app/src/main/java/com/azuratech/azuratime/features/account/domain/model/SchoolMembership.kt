package com.azuratech.azuratime.features.account.domain.model

/**
 * 🏫 SCHOOL MEMBERSHIP (v3.2.1-ai-native)
 * Domain model representing an account's membership and role within a school.
 */
data class SchoolMembership(
    val schoolName: String,
    val role: String, // "SUPER_ADMIN", "ADMIN", "SUPERVISOR", "USER"
    val status: String = "ACTIVE",
    val assignedClassIds: List<String> = emptyList(),
)
