package com.azuratech.azuratime.core.api.account.model

/**
 * Account Entity - Domain Model for Authentication
 * 
 * This is a pure Kotlin data class that represents an account.
 * It is used across the :core-auth-api and can be mapped to/from Room entities.
 * 
 * NOTE: This was moved from :app to :core-api to support modular architecture.
 * If you need Room-specific annotations, map this to a separate Room entity.
 */
data class AccountEntity(
    val accountId: String,
    val email: String,
    val name: String,
    val photoUrl: String? = null,
    val status: String = "PENDING",
    val role: String = "USER",
    val activeSchoolId: String? = null,
    val activeClassId: String? = null,
    val schoolName: String? = null,
    val memberships: Map<String, SchoolMembership> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED",
)

data class SchoolMembership(
    val schoolId: String,
    val schoolName: String,
    val role: String,
    val status: String,
    val joinedAt: Long = System.currentTimeMillis(),
)

// Mapping extensions (if you have a separate Account domain model)
// fun AccountEntity.toDomain() = Account(...)
// fun Account.toEntity() = AccountEntity(...)