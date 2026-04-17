package com.azuratech.azuratime.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "user_class_access",
    primaryKeys = ["userId", "classId"], // 🔥 Composite Key: 1 Guru cuma bisa di-assign 1 kali ke kelas yang sama
    indices = [
        Index(value = ["userId"]),
        Index(value = ["classId"]),
        Index(value = ["schoolId"])
    ] // Optimasi pencarian agar query ngebut
)
data class UserClassAccessEntity(
    val userId: String,   // ID Guru / Admin
    val classId: String,  // ID Kelas
    val schoolId: String = "",
    val assignedAt: Long = System.currentTimeMillis() // Kapan akses ini diberikan
)