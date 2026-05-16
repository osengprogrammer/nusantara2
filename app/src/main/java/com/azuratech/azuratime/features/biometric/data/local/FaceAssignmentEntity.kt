package com.azuratech.azuratime.features.biometric.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.azuratech.azuratime.features.biometric.data.local.BiometricFaceEntity
import com.azuratech.azuratime.features.school.data.local.ClassEntity

@Entity(
    tableName = "face_assignments",
    primaryKeys = ["studentId", "classId"],
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["classId"]),
        Index(value = ["schoolId"])
    ],
    foreignKeys = [
        // 1. Link to the Face (Student)
        ForeignKey(
            entity = BiometricFaceEntity::class,
            parentColumns = ["studentId"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        // 2. Link to the Class 
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class FaceAssignmentEntity(
    val studentId: String,   
    val classId: String,  
    val schoolId: String = "",
    val isSynced: Boolean = false
)
