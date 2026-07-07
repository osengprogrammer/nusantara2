package com.azuratech.azuratime.features.payment.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val schoolId: String,
    val amount: Double,
    val type: String, // e.g., "TOP_UP", "DEDUCTION"
    val timestamp: Long,
    val performedByAccountId: String = "",
    val performedByAccountName: String = "",
    val isSynced: Boolean = false
)
