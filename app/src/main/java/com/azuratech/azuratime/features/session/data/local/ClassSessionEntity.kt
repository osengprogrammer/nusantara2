package com.azuratech.azuratime.features.session.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val classId: String,
    val subjectId: String,
    val supervisorEmail: String, // 🔥 Unified Identity
    val dayOfWeek: Int, // 1 (Mon) - 7 (Sun)
    val startTime: String, // "08:00"
    val endTime: String, // "09:30"
    val schoolId: String,
    val lookupKey: String, // 🔥 Production Index: classId_subjectId_day_time
    val isActive: Boolean = true, // 🔥 Soft Delete Support
    val isSynced: Boolean = false,
)
