package com.azuratech.azuratime.features.session.domain.usecase

import com.azuratech.azuratime.features.session.domain.repository.SessionRepository
import com.azuratech.azuratime.features.session.data.local.ClassSessionEntity
import com.azuratech.azuratime.core.result.Result
import javax.inject.Inject

/**
 * 🔒 DELETE SESSION USE CASE
 * Removes a session entity via the session repository.
 */
class DeleteSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(session: ClassSessionEntity): Result<Unit> =
        sessionRepository.deleteSession(session)
}
