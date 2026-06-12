package com.azuratech.azuratime.features.session

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.features.session.domain.model.SessionType
import java.util.UUID
import javax.inject.Inject

/**
 * 🚀 CREATE SESSION USE CASE (v3.7.0-base)
 * Centralizes sessionId and lookupKey generation for production consistency.
 * Supports Academic, Class-Wide, and Global tiers.
 */
class CreateSessionUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    suspend operator fun invoke(
        classId: String?,
        subjectId: String?,
        sessionType: SessionType = SessionType.ACADEMIC,
        supervisorEmail: String,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        schoolId: String,
    ): Result<Unit> {
        // 🔥 AI Native: Prefix-based lookupKey to prevent cross-tier collisions
        val timeKey = startTime.replace(":", "")
        val lookupKey = when (sessionType) {
            SessionType.ACADEMIC -> "ACADEMIC_${classId}_${subjectId}_${dayOfWeek}_$timeKey"
            SessionType.CLASS_WIDE -> "CLASS_${classId}_ALL_${dayOfWeek}_$timeKey"
            SessionType.GLOBAL -> "GLOBAL_${schoolId}_ALL_${dayOfWeek}_$timeKey"
        }

        val session = ClassSessionEntity(
            sessionId = UUID.randomUUID().toString(),
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

        return repository.saveSession(session)
    }
}
