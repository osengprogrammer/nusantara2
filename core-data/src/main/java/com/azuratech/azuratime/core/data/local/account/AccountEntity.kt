package com.azuratech.azuratime.core.data.local.account

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val accountId: String,
    val email: String,
    val name: String,
    val role: String,
    val status: String,
    val syncStatus: String
)
