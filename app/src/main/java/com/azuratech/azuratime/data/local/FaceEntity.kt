package com.azuratech.azuratime.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "faces",
    indices = [
        Index(value = ["schoolId"]),
        Index(value = ["studentId"])
    ]
)
data class FaceEntity(
    @PrimaryKey val faceId: String = UUID.randomUUID().toString(),
    val studentId: String? = null,
    val schoolId: String = "",
    val name: String,
    val photoUrl: String? = null,
    val embedding: FloatArray? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String? = "Admin",
    val lastUpdated: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false
)