package com.azuratech.azuratime.features.session.domain.usecase

import com.azuratech.azuratime.features.session.domain.repository.SessionRepository

import com.azuratech.azuratime.core.result.Result
import javax.inject.Inject

/**
 * 🔥 AI Native UseCase: Validates if the current supervisor has permission
 * to record attendance for a specific session.
 */
class ValidateSessionAccessUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: String): Result<Boolean> {
        return sessionRepository.validateSessionAccess(sessionId)
    }
}
