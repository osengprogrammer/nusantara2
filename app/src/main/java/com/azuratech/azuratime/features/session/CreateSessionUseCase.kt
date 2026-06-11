package com.azuratech.azuratime.features.session

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import java.util.UUID
import javax.inject.Inject

/**
 * 🚀 CREATE SESSION USE CASE (v3.3.2-final)
 * Centralizes sessionId and lookupKey generation for production consistency.
 */
class CreateSessionUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    suspend operator fun invoke(
        classId: String,
        subjectId: String,
        supervisorEmail: String,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        schoolId: String,
    ): Result<Unit> {
        // 🔥 AI Native: lookupKey generation classId_subjectId_day_time
        val lookupKey = "${classId}_${subjectId}_${dayOfWeek}_${startTime.replace(":", "")}"

        val session = ClassSessionEntity(
            sessionId = UUID.randomUUID().toString(),
            classId = classId,
            subjectId = subjectId,
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
