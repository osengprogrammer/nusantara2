package com.azuratech.azuratime.features.template.domain.usecase

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.template.domain.repository.TemplateRepository
import javax.inject.Inject

/**
 * 🚀 SYNC TEMPLATE USE CASE (v3.4.0)
 * Orchestrates the full pipeline: Fetch Classes -> Fetch Subjects -> Atomic Persist.
 * Ensures data integrity across Workspace boundaries.
 */
class SyncTemplateUseCase @Inject constructor(
    private val repository: TemplateRepository,
) {
    suspend operator fun invoke(
        schoolId: String,
        classIds: List<String>,
        subjectIds: List<String>,
    ): Result<Unit> {
        // 1. Fetch Classes in Batch
        val classesResult = repository.fetchClassesByIds(schoolId, classIds)

        return classesResult.flatMap { classes ->
            // 2. Fetch Subjects in Batch
            val subjectsResult = repository.fetchSubjectsByIds(schoolId, subjectIds)
            subjectsResult.flatMap { subjects ->
                // 3. Atomic Persist to Room
                repository.persistTemplateData(classes, subjects)
            }
        }
    }
}
