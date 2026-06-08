package com.azuratech.azuratime.core.util

import com.azuratech.azuratime.core.domain.model.AccountRole
import com.azuratech.azuratime.features.account.domain.model.Account

/**
 * 🔐 PERMISSION UTILS (v3.2.2-ai-native)
 * Centralized authorization logic for Account and School resources.
 * Enforces strict type-safety against AccountRole enums.
 */

/**
 * Safely parses a role string to AccountRole, returning null if invalid or unparseable.
 */
private fun String?.toSafeAccountRole(): AccountRole? {
    if (this == null) return null
    return try {
        AccountRole.valueOf(this.trim().uppercase())
    } catch (e: IllegalArgumentException) {
        null
    }
}

/**
 * Returns true if the account has Admin or Super Admin role in the given school.
 */
fun Account?.isAdmin(schoolId: String): Boolean {
    if (this == null) return false
    val roleStr = memberships[schoolId]?.role ?: role.name
    val parsedRole = roleStr.toSafeAccountRole() ?: return false
    return parsedRole == AccountRole.ADMIN || parsedRole == AccountRole.SUPER_ADMIN
}

/**
 * Returns true if the account is a Supervisor of the specific class in the given school.
 */
fun Account?.isSupervisorOf(schoolId: String, classId: String): Boolean {
    if (this == null) return false
    val membership = memberships[schoolId] ?: return false
    val roleStr = membership.role
    val parsedRole = roleStr.toSafeAccountRole() ?: return false
    val isSupervisorRole = parsedRole == AccountRole.SUPERVISOR
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
    val parsedRole = roleStr.toSafeAccountRole() ?: return false
    return parsedRole == AccountRole.ADMIN ||
        parsedRole == AccountRole.SUPER_ADMIN ||
        parsedRole == AccountRole.SUPERVISOR
}
