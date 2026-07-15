package com.azuratech.azuratime.features.template.domain.usecase

import com.azuratech.azuratime.features.template.domain.repository.TemplateRepository
import com.azuratech.azuratime.features.template.domain.model.SchoolTemplate
import com.azuratech.azuratime.core.result.Result
import javax.inject.Inject

/**
 * 🔒 FETCH SCHOOL TEMPLATES USE CASE
 * Retrieves all available school templates from the template repository.
 */
class FetchSchoolTemplatesUseCase @Inject constructor(
    private val templateRepository: TemplateRepository,
) {
    suspend operator fun invoke(): Result<List<SchoolTemplate>> =
        templateRepository.fetchSchoolTemplates()
}
