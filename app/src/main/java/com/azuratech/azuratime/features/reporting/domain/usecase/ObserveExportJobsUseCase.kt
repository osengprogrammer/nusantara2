package com.azuratech.azuratime.features.reporting.domain.usecase

import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.features.reporting.domain.model.ExportJobProfile
import com.azuratech.azuratime.features.reporting.domain.repository.ExportRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 📊 OBSERVE EXPORT JOBS USE CASE
 * Wraps ExportRepository.observeExportJobs() to keep the ViewModel free of direct repository dependencies.
 */
class ObserveExportJobsUseCase @Inject constructor(
    private val exportRepository: ExportRepository,
) {
    operator fun invoke(accountId: String): Flow<Result<List<ExportJobProfile>>> =
        exportRepository.observeExportJobs(accountId)
}
