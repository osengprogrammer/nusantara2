package com.azuratech.azuratime.core.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_class_access",
    primaryKeys = ["accountId", "classId"], // 🔥 Composite Key: 1 Akun cuma bisa di-assign 1 kali ke kelas yang sama
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["classId"]),
        Index(value = ["schoolId"]),
    ],
)
data class AccountClassAccessEntity(
    val accountId: String, // ID Guru / Admin (Account)
    val classId: String, // ID Kelas
    val schoolId: String = "",
    val assignedAt: Long = System.currentTimeMillis(), // Kapan akses ini diberikan
)
