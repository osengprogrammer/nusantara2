package com.azuratech.azuratime.features.session.domain.usecase

import com.azuratech.azuratime.features.session.domain.repository.SessionRepository
import com.azuratech.azuratime.core.data.local.SubjectEntity
import com.azuratech.azuratime.core.result.Result
import javax.inject.Inject

/**
 * 🔒 SAVE SUBJECT USE CASE
 * Persists a subject entity via the session repository.
 */
class SaveSubjectUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(subject: SubjectEntity): Result<Unit> =
        sessionRepository.saveSubject(subject)
}
