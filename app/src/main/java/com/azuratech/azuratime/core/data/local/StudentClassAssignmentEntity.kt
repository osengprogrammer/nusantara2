package com.azuratech.azuratime.core.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index



@Entity(
    tableName = "student_class_assignments",
    primaryKeys = ["studentId", "classId"],
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["classId"]),
        Index(value = ["schoolId"]),
    ],
    foreignKeys = [
        // 1. Link to the Student (Identity)
        ForeignKey(
            entity = com.azuratech.azuratime.features.student.data.local.StudentEntity::class,
            parentColumns = ["studentId"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.CASCADE,
        ),
        // 2. Link to the Class
        ForeignKey(
            entity = com.azuratech.azuratime.core.data.local.ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.NO_ACTION,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
)
data class StudentClassAssignmentEntity(
    val studentId: String,
    val classId: String,
    val schoolId: String = "",
    val isSynced: Boolean = false,
)
