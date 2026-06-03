package com.azuratech.azuratime.features.school.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.azuratech.azuraengine.model.ClassModel
import java.util.UUID

@Entity(
    tableName = "classes",
    indices = [Index(value = ["schoolId"])],
)
data class ClassEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ownerAccountId: String,
    val schoolId: String? = null,
    val name: String,
    val grade: String = "",
    val accountId: String? = null,
    val studentCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val displayOrder: Int = 0,
    val isSynced: Boolean = false,
) {
    fun toDomain(): ClassModel = ClassModel(
        id = id,
        schoolId = schoolId,
        name = name,
        grade = grade,
        accountId = accountId,
        studentCount = studentCount,
        studentIds = emptyList(),
        createdAt = createdAt,
    )
}
