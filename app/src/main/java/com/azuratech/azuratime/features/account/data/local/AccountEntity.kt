package com.azuratech.azuratime.features.account.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.azuratech.azuratime.core.data.local.Converters

@Entity(tableName = "accounts")
@TypeConverters(Converters::class)
data class AccountEntity(
    @PrimaryKey val accountId: String, // 🔥 Unified Identity: Account ID
    val email: String,
    val name: String,
    val photoUrl: String? = null,
    val status: String = "PENDING",
    val role: String = "MEMBER",
    val activeSchoolId: String? = null,
    val activeClassId: String? = null,
    val schoolName: String? = null,
    val memberships: Map<String, Membership> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED",
)

fun AccountEntity.toProfile() = com.azuratech.azuratime.features.account.domain.model.AccountProfile(
    accountId = accountId,
    email = email,
    name = name,
    photoUrl = photoUrl,
    role = role,
    activeSchoolId = activeSchoolId,
    activeClassId = activeClassId,
    memberships = memberships,
)
