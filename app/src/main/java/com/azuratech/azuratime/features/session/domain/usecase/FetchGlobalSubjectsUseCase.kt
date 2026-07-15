package com.azuratech.azuratime.features.session.domain.usecase

import com.azuratech.azuratime.features.template.domain.repository.TemplateRepository
import com.azuratech.azuratime.core.domain.model.SubjectTemplate
import com.azuratech.azuratime.core.result.Result
import javax.inject.Inject

/**
 * 🔒 FETCH GLOBAL SUBJECTS USE CASE
 * Retrieves all global subject templates from the template repository.
 */
class FetchGlobalSubjectsUseCase @Inject constructor(
    private val templateRepository: TemplateRepository,
) {
    suspend operator fun invoke(): Result<List<SubjectTemplate>> =
        templateRepository.fetchAllGlobalSubjects()
}
