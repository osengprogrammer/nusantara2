package com.azuratech.azuratime.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentSnapshot

@Entity(
    tableName = "subjects",
    indices = [
        Index(value = ["schoolId", "name"], unique = true),
    ],
)
data class SubjectEntity(
    @PrimaryKey val subjectId: String,
    val name: String,
    val description: String? = null,
    val schoolId: String,
    val isActive: Boolean = true, // 🔥 Soft Delete Support
    val isSynced: Boolean = false,
    val isFromTemplate: Boolean = false, // 👈 New Property
) {
    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "subjectId" to subjectId,
            "name" to name,
            "description" to description,
            "schoolId" to schoolId,
            "isActive" to isActive,
            "isFromTemplate" to isFromTemplate,
        )
    }
}

fun DocumentSnapshot.toSubjectEntity(schoolId: String): SubjectEntity? {
    return try {
        SubjectEntity(
            subjectId = id,
            name = getString("name") ?: "",
            description = getString("description"),
            schoolId = getString("schoolId") ?: schoolId,
            isActive = getBoolean("isActive") ?: true,
            isSynced = true,
            isFromTemplate = getBoolean("isFromTemplate") ?: false,
        )
    } catch (e: Exception) {
        null
    }
}
