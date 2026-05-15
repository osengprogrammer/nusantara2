package com.azuratech.azuratime.features.staff.data.local

/**
 * 🏫 Multi-Tenant Membership: Hak akses user di dalam candinya sendiri atau candi teman.
 */
data class Membership(
    val schoolName: String,
    val role: String, // "ADMIN", "TEACHER", "SUPER_USER"
    val assignedClassIds: List<String> = emptyList() // 🔥 Daftar kelas yang diamanahi
)
