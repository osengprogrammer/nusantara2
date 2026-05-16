package com.azuratech.azuratime.features.account.data.local

/**
 * 🏫 Multi-Tenant Membership: Hak akses akun di dalam sekolahnya sendiri atau sekolah teman.
 */
data class Membership(
    val schoolName: String,
    val role: String, // "ADMIN", "TEACHER", "SUPER_USER"
    val status: String = "ACTIVE", // "ACTIVE", "PENDING", "REJECTED"
    val assignedClassIds: List<String> = emptyList() // 🔥 Daftar kelas yang diamanahi
)
