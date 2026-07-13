package com.azuratech.azuratime.features.session.domain.usecase

import com.azuratech.azuratime.features.session.domain.repository.SessionRepository

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.features.session.domain.model.SessionType
import javax.inject.Inject

/**
 * 🚀 UPDATE SESSION USE CASE
 * Centralizes lookupKey generation and session updates for consistency.
 */
class UpdateSessionUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    suspend operator fun invoke(
        sessionId: String,
        classId: String?,
        subjectId: String?,
        sessionType: SessionType = SessionType.ACADEMIC,
        supervisorEmail: String,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        schoolId: String,
    ): Result<Unit> {
        val timeKey = startTime.replace(":", "")
        val lookupKey = when (sessionType) {
            SessionType.ACADEMIC -> "ACADEMIC_${classId}_${subjectId}_${dayOfWeek}_$timeKey"
            SessionType.CLASS_WIDE -> "CLASS_${classId}_ALL_${dayOfWeek}_$timeKey"
            SessionType.GLOBAL -> "GLOBAL_${schoolId}_ALL_${dayOfWeek}_$timeKey"
        }

        val session = ClassSessionEntity(
            sessionId = sessionId,
            classId = classId,
            subjectId = subjectId,
            sessionType = sessionType,
            supervisorEmail = supervisorEmail,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime,
            schoolId = schoolId,
            lookupKey = lookupKey,
            isActive = true,
            isSynced = false,
        )

        return repository.updateSession(session)
    }
}
