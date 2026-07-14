package com.azuratech.azuratime.core.data.local

import androidx.room.Ignore
import com.azuratech.azuratime.features.session.domain.model.SessionType

/**
 * 🔥 POJO for JOIN results between ClassSessionEntity and SubjectEntity.
 * All columns from class_sessions table must be present (SELECT s.*).
 */
data class SessionWithDetails(
    val sessionId: String,
    val schoolId: String,
    val classId: String?,
    val subjectId: String?,
    val sessionType: SessionType = SessionType.ACADEMIC,
    val supervisorEmail: String = "",
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val lookupKey: String = "",
    val isActive: Boolean,
    val isSynced: Boolean = false,
    val subjectName: String? = null,
) {
    @Ignore var className: String? = null
}
