package com.azuratech.azuratime.features.session

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 🔥 AI Native UseCase: Fetches sessions for a specific day.
 * Standardizes mapping from DB models to UI-ready models.
 */
class GetSessionsByDayUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(schoolId: String, day: Int): Flow<Result<List<SessionWithDetails>>> {
        return sessionRepository.getSessionsByDayFlow(schoolId, day)
    }
}
