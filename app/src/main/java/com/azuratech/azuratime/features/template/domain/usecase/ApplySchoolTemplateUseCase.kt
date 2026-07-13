package com.azuratech.azuratime.features.template.domain.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.features.template.domain.model.SchoolTemplate
import com.azuratech.azuratime.features.template.domain.repository.TemplateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 🚀 APPLY SCHOOL TEMPLATE USE CASE (v1.0.0)
 * Orchestrates applying a selected global school template to the active school.
 * Delegates entity mapping and persistence to the repository to maintain VSA compliance.
 */
class ApplySchoolTemplateUseCase @Inject constructor(
    private val repository: TemplateRepository,
    private val syncManager: SyncManager,
) {
    suspend operator fun invoke(
        schoolId: String,
        ownerId: String,
        template: SchoolTemplate,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        repository.applyTemplate(schoolId, ownerId, template).flatMap {
            syncManager.enqueueSync()
            Result.Success(Unit)
        }
    }
}
