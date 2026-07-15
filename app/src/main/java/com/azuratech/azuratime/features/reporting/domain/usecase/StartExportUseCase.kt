package com.azuratech.azuratime.features.reporting.domain.usecase

import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.features.reporting.domain.repository.ExportRepository
import javax.inject.Inject

/**
 * 📊 START EXPORT USE CASE
 * Wraps ExportRepository.startExport() to keep the ViewModel free of direct repository dependencies.
 */
class StartExportUseCase @Inject constructor(
    private val exportRepository: ExportRepository,
) {
    suspend operator fun invoke(
        format: String,
        accountId: String,
        schoolId: String,
    ): Result<String> =
        exportRepository.startExport(format, accountId, schoolId)
}
