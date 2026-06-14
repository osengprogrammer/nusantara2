package com.azuratech.azuratime.features.session.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentSnapshot

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val subjectId: String,
    val name: String,
    val description: String? = null,
    val schoolId: String,
    val isSynced: Boolean = false,
) {
    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "subjectId" to subjectId,
            "name" to name,
            "description" to description,
            "schoolId" to schoolId,
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
            isSynced = true,
        )
    } catch (e: Exception) {
        null
    }
}
