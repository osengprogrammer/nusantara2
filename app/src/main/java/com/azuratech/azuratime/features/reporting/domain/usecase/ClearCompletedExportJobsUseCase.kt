package com.azuratech.azuratime.features.reporting.domain.usecase

import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.features.reporting.domain.repository.ExportRepository
import javax.inject.Inject

/**
 * 📊 CLEAR COMPLETED EXPORT JOBS USE CASE
 * Wraps ExportRepository.clearCompletedJobs() to keep the ViewModel free of direct repository dependencies.
 */
class ClearCompletedExportJobsUseCase @Inject constructor(
    private val exportRepository: ExportRepository,
) {
    suspend operator fun invoke(accountId: String): Result<Unit> =
        exportRepository.clearCompletedJobs(accountId)
}
