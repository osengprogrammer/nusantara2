package com.azuratech.azuratime.features.reporting.domain.usecase

import com.azuratech.azuratime.core.result.Result
import com.azuratech.azuratime.features.reporting.domain.model.SystemAuditTrail
import com.azuratech.azuratime.features.reporting.domain.repository.ReportRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * 📊 GET AUDIT LOGS USE CASE
 * Wraps ReportRepository.getAuditLogs() to keep the ViewModel free of direct repository dependencies.
 */
class GetAuditLogsUseCase @Inject constructor(
    private val reportRepository: ReportRepository,
) {
    suspend operator fun invoke(
        startDate: LocalDate,
        endDate: LocalDate,
        schoolId: String,
    ): Result<List<SystemAuditTrail>> =
        reportRepository.getAuditLogs(startDate, endDate, schoolId)
}
