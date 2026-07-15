package com.azuratech.azuratime.features.template.domain.usecase

import com.azuratech.azuratime.features.template.domain.repository.TemplateRepository
import com.azuratech.azuratime.features.template.domain.model.ClassTemplate
import com.azuratech.azuratime.core.result.Result
import javax.inject.Inject

/**
 * 🔒 FETCH GLOBAL CLASSES BY IDS USE CASE
 * Retrieves global class templates by their IDs.
 */
class FetchGlobalClassesByIdsUseCase @Inject constructor(
    private val templateRepository: TemplateRepository,
) {
    suspend operator fun invoke(classIds: List<String>): Result<List<ClassTemplate>> =
        templateRepository.fetchGlobalClassesByIds(classIds)
}
