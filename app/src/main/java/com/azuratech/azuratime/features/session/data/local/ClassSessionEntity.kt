package com.azuratech.azuratime.features.session.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.azuratech.azuratime.features.session.domain.model.SessionType
import com.google.firebase.firestore.DocumentSnapshot

@Entity(
    tableName = "class_sessions",
    indices = [
        Index(value = ["schoolId"]),
        Index(value = ["classId"]),
        Index(value = ["subjectId"]),
        Index(value = ["lookupKey"], unique = true),
    ],
)
data class ClassSessionEntity(
    @PrimaryKey val sessionId: String,
    val classId: String?, // ✅ Nullable for GLOBAL
    val subjectId: String?, // ✅ Nullable for CLASS_WIDE & GLOBAL
    val sessionType: SessionType = SessionType.ACADEMIC, // ✅ New Tiering Field
    val supervisorEmail: String, // 🔥 Unified Identity
    val dayOfWeek: Int, // 1 (Mon) - 7 (Sun)
    val startTime: String, // "08:00"
    val endTime: String, // "09:30"
    val schoolId: String,
    val lookupKey: String, // 🔥 Production Index: TYPE_classId_subjectId_day_time
    val isActive: Boolean = true, // 🔥 Soft Delete Support
    val isSynced: Boolean = false,
) {
    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "sessionId" to sessionId,
            "classId" to classId,
            "subjectId" to subjectId,
            "sessionType" to sessionType.name,
            "supervisorEmail" to supervisorEmail,
            "dayOfWeek" to dayOfWeek,
            "startTime" to startTime,
            "endTime" to endTime,
            "schoolId" to schoolId,
            "lookupKey" to lookupKey,
            "isActive" to isActive,
        )
    }
}

fun DocumentSnapshot.toClassSessionEntity(schoolId: String): ClassSessionEntity? {
    return try {
        val sessionTypeStr = getString("sessionType")
        val type = if (sessionTypeStr != null) SessionType.valueOf(sessionTypeStr) else SessionType.ACADEMIC

        ClassSessionEntity(
            sessionId = id,
            classId = getString("classId"),
            subjectId = getString("subjectId"),
            sessionType = type,
            supervisorEmail = getString("supervisorEmail") ?: "",
            dayOfWeek = getLong("dayOfWeek")?.toInt() ?: 1,
            startTime = getString("startTime") ?: "00:00",
            endTime = getString("endTime") ?: "00:00",
            schoolId = getString("schoolId") ?: schoolId,
            lookupKey = getString("lookupKey") ?: "",
            isActive = getBoolean("isActive") ?: true,
            isSynced = true,
        )
    } catch (e: Exception) {
        null
    }
}
