package com.azuratech.azuratime.features.session.domain.usecase

import com.azuratech.azuratime.features.session.domain.repository.SessionRepository

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.data.local.SessionWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 🚀 GET ACTIVE SESSION USE CASE (v3.4.0-optimized)
 * Resolves the currently running session using database-level filtering.
 * Eliminates in-memory looping for maximum performance during attendance capture.
 */
class GetActiveSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    /**
     * @param schoolId The active workspace ID.
     * @param email The supervisor's identity.
     * @param dayOfWeek 1 (Mon) to 7 (Sun).
     * @param currentTime Format "HH:mm" (e.g., "08:30").
     */
    operator fun invoke(
        schoolId: String,
        email: String,
        dayOfWeek: Int,
        currentTime: String,
    ): Flow<Result<SessionWithDetails?>> = flow {
        emit(Result.Loading)
        val result = sessionRepository.getActiveSessionOptimized(
            schoolId = schoolId,
            email = email,
            day = dayOfWeek,
            currentTime = currentTime,
        )
        emit(result)
    }
}
