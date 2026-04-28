package com.azuratech.azuratime.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "school_class_assignments",
    primaryKeys = ["schoolId", "classId"],
    indices = [Index("schoolId"), Index("classId")],
    foreignKeys = [
        ForeignKey(
            entity = SchoolEntity::class,
            parentColumns = ["id"],
            childColumns = ["schoolId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SchoolClassAssignment(
    val schoolId: String,
    val classId: String,
    val assignedAt: Long = System.currentTimeMillis()
)
