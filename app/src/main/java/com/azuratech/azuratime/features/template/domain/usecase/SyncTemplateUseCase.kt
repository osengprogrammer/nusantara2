package com.azuratech.azuratime.features.template.domain.usecase

import com.azuratech.azuratime.core.result.Result
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
        if (classesResult !is Result.Success) return classesResult.typedUnit()

        // 2. Fetch Subjects in Batch
        val subjectsResult = repository.fetchSubjectsByIds(schoolId, subjectIds)
        if (subjectsResult !is Result.Success) return subjectsResult.typedUnit()

        // 3. Atomic Persist to Room
        return repository.persistTemplateData(classesResult.data, subjectsResult.data)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> Result<T>.typedUnit(): Result<Unit> = this as Result<Unit>
}
