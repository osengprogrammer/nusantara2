package com.azuratech.azuratime.features.bankforwarder.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_notifications")
data class BankNotificationEntity(
    @PrimaryKey val id: String,
    val bankName: String,
    val title: String,
    val body: String,
    val amount: Double,
    val studentId: String? = null,
    val timestamp: Long,
    val isProcessed: Boolean = false,
    val isSynced: Boolean = false,
)
