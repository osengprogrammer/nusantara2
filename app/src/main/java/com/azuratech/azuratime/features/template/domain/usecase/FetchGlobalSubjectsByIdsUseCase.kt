package com.azuratech.azuratime.features.template.domain.usecase

import com.azuratech.azuratime.features.template.domain.repository.TemplateRepository
import com.azuratech.azuratime.core.domain.model.SubjectTemplate
import com.azuratech.azuratime.core.result.Result
import javax.inject.Inject

/**
 * 🔒 FETCH GLOBAL SUBJECTS BY IDS USE CASE
 * Retrieves global subject templates by their IDs.
 */
class FetchGlobalSubjectsByIdsUseCase @Inject constructor(
    private val templateRepository: TemplateRepository,
) {
    suspend operator fun invoke(subjectIds: List<String>): Result<List<SubjectTemplate>> =
        templateRepository.fetchGlobalSubjectsByIds(subjectIds)
}
