package com.azuratech.azuratime.features.biometric.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "student_biometrics",
    indices = [Index(value = ["schoolId"])]
)
data class StudentBiometricEntity(
    @PrimaryKey val studentId: String, // 🔥 Unified Identity: matches StudentEntity.id
    val schoolId: String = "",
    val name: String = "",
    val photoUrl: String? = null,
    val embedding: FloatArray? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String? = "Admin",
    val lastUpdated: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)
