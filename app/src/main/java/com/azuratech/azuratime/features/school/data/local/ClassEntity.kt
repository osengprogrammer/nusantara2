package com.azuratech.azuratime.features.school.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.azuratech.azuraengine.model.ClassModel
import java.util.UUID

@Entity(
    tableName = "classes",
    indices = [
        Index(value = ["schoolId"]),
        Index(value = ["schoolId", "name"], unique = true),
    ],
)
data class ClassEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ownerAccountId: String,
    val schoolId: String? = null,
    val name: String,
    val grade: String = "",
    val accountId: String? = null,
    val studentCount: Int = 0,
    val subjectIds: List<String> = emptyList(), // 🔥 Curriculum Inheritance
    val createdAt: Long = System.currentTimeMillis(),
    val displayOrder: Int = 0,
    val isSynced: Boolean = false,
    val isFromTemplate: Boolean = false, // 👈 New Property
) {
    fun toDomain(): ClassModel = ClassModel(
        id = id,
        schoolId = schoolId,
        name = name,
        grade = grade,
        accountId = accountId,
        studentCount = studentCount,
        studentIds = emptyList(),
        subjectIds = subjectIds,
        createdAt = createdAt,
    )
}

fun com.google.firebase.firestore.DocumentSnapshot.toClassEntity(schoolId: String? = null): ClassEntity? {
    return try {
        ClassEntity(
            id = id,
            ownerAccountId = getString("ownerAccountId") ?: "",
            schoolId = getString("schoolId") ?: schoolId,
            name = getString("name") ?: "",
            grade = getString("grade") ?: "",
            accountId = getString("accountId"),
            studentCount = getLong("studentCount")?.toInt() ?: 0,
            subjectIds = (get("subjectIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            createdAt = getLong("createdAt") ?: System.currentTimeMillis(),
            isSynced = true,
            isFromTemplate = getBoolean("isFromTemplate") ?: false,
        )
    } catch (e: Exception) {
        null
    }
}
