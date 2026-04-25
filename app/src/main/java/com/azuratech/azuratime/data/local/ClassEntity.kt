package com.azuratech.azuratime.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.azuratech.azuraengine.model.ClassModel
import java.util.UUID

@Entity(
    tableName = "classes",
    indices = [Index(value = ["schoolId"])],
    foreignKeys = [
        ForeignKey(
            entity = SchoolEntity::class,
            parentColumns = ["id"],
            childColumns = ["schoolId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ClassEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val schoolId: String,
    val name: String,
    val grade: String = "",
    val teacherId: String? = null,
    val studentCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val displayOrder: Int = 0,
    val isSynced: Boolean = false
) {
    fun toDomain(): ClassModel = ClassModel(
        id = id,
        schoolId = schoolId,
        name = name,
        grade = grade,
        teacherId = teacherId,
        studentCount = studentCount,
        createdAt = createdAt
    )
}
