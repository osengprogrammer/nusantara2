package com.azuratech.azuratime.core.util

import com.azuratech.azuratime.core.domain.model.AccountRole
import com.azuratech.azuratime.features.account.domain.model.Account

/**
 * 🔐 PERMISSION UTILS (v3.2.1-ai-native)
 * Centralized authorization logic for Account and School resources.
 */

/**
 * Returns true if the account has Admin or Super Admin role in the given school.
 */
fun Account?.isAdmin(schoolId: String): Boolean {
    if (this == null) return false
    val role = memberships[schoolId]?.role ?: role.name
    return role == AccountRole.ADMIN.name || role == AccountRole.SUPER_ADMIN.name
}

/**
 * Returns true if the account is a Supervisor of the specific class in the given school.
 */
fun Account?.isSupervisorOf(schoolId: String, classId: String): Boolean {
    if (this == null) return false
    val membership = memberships[schoolId] ?: return false
    val roleStr = membership.role
    val isSupervisorRole = roleStr == AccountRole.SUPERVISOR.name || roleStr == "TEACHER" // Backwards compat
    return isSupervisorRole && membership.assignedClassIds.contains(classId)
}

/**
 * Returns true if the account can access the class (Admin OR Supervisor of that class).
 */
fun Account?.canAccessClass(schoolId: String, classId: String): Boolean {
    if (this == null) return false
    if (isAdmin(schoolId)) return true
    return isSupervisorOf(schoolId, classId)
}

/**
 * Extension-style helpers for cleaner usage.
 */
fun Account?.isSuperAdmin(): Boolean {
    return this?.role == AccountRole.SUPER_ADMIN
}

/**
 * Returns true if the account can access a specific feature (Admin or Supervisor).
 */
fun Account?.canAccessFeature(schoolId: String): Boolean {
    if (this == null) return false
    val roleStr = memberships[schoolId]?.role ?: role.name
    return roleStr == AccountRole.ADMIN.name ||
        roleStr == AccountRole.SUPER_ADMIN.name ||
        roleStr == AccountRole.SUPERVISOR.name ||
        roleStr == "TEACHER"
}
