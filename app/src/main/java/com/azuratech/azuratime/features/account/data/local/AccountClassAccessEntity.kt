package com.azuratech.azuratime.features.account.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_class_access",
    primaryKeys = ["accountId", "classId", "subjectId"], // 🔥 Matrix Key: Account + Class + Subject
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["classId"]),
        Index(value = ["subjectId"]),
        Index(value = ["schoolId"]),
    ],
)
data class AccountClassAccessEntity(
    val accountId: String, // ID Guru / Admin (Account)
    val classId: String, // ID Kelas
    val subjectId: String = "", // 🔥 Empty string means Homeroom/All Subjects
    val schoolId: String = "",
    val assignedAt: Long = System.currentTimeMillis(), // Kapan akses ini diberikan
)
