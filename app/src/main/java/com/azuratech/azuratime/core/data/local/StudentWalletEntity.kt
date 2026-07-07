package com.azuratech.azuratime.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_wallets")
data class StudentWalletEntity(
    @PrimaryKey val studentId: String,
    val schoolId: String,
    val currentBalance: Double = 0.0,
    val isSynced: Boolean = false
)
