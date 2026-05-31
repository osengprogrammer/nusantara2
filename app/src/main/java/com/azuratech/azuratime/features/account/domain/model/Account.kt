package com.azuratech.azuratime.features.account.domain.model

import com.azuratech.azuratime.core.domain.model.AccountRole

/**
 * 👤 ACCOUNT DOMAIN MODEL (v3.2.0-ai-native)
 * Unified identity model for the application.
 */
data class Account(
    val accountId: String,
    val email: String,
    val name: String,
    val photoUrl: String? = null,
    val role: AccountRole = AccountRole.USER,
    val status: String = "PENDING",
    val activeSchoolId: String? = null,
    val activeClassId: String? = null,
    val memberships: Map<String, SchoolMembership> = emptyMap(),
    val syncStatus: String = "SYNCED",
)

fun Account.toProfileCompat() = AccountProfile(
    accountId = accountId,
    email = email,
    name = name,
    photoUrl = photoUrl,
    role = role.name,
    activeSchoolId = activeSchoolId,
    activeClassId = activeClassId,
    memberships = memberships,
)

fun AccountProfile.toDomain() = Account(
    accountId = accountId,
    email = email,
    name = name,
    photoUrl = photoUrl,
    role = AccountRole.fromString(role),
    activeSchoolId = activeSchoolId,
    activeClassId = activeClassId,
    memberships = memberships,
)
