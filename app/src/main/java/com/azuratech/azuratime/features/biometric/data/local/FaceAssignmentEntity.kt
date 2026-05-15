package com.azuratech.azuratime.features.biometric.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.azuratech.azuratime.features.biometric.data.local.BiometricFaceEntity
import com.azuratech.azuratime.features.school.data.local.ClassEntity

@Entity(
    tableName = "face_assignments",
    primaryKeys = ["faceId", "classId"],
    indices = [
        Index(value = ["faceId"]),
        Index(value = ["classId"]),
        Index(value = ["schoolId"]) // 🔥 Index for fast tenant filtering
    ],
    foreignKeys = [
        // 1. Link to the Face (Student)
        ForeignKey(
            entity = BiometricFaceEntity::class,
            parentColumns = ["faceId"],
            childColumns = ["faceId"],
            onDelete = ForeignKey.CASCADE, // Auto-delete if Face is deleted
            onUpdate = ForeignKey.CASCADE
        ),
        // 2. Link to the Class 
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE, // Auto-delete if Class is deleted
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class FaceAssignmentEntity(
    val faceId: String,   
    val classId: String,  
    val schoolId: String = "", // 🔥 Added tenant scoping to isolate data
    val isSynced: Boolean = false
)