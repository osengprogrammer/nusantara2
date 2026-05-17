package com.azuratech.azuratime.features.account.domain.model

import com.azuratech.azuratime.features.account.data.local.Membership

/**
 * 👤 USER PROFILE (v3.2.0-ai-native)
 * Domain model for account management.
 */
data class AccountProfile(
    val accountId: String,
    val email: String,
    val name: String,
    val photoUrl: String? = null,
    val role: String = "MEMBER",
    val activeSchoolId: String? = null,
    val activeClassId: String? = null,
    val memberships: Map<String, Membership> = emptyMap(),
)
