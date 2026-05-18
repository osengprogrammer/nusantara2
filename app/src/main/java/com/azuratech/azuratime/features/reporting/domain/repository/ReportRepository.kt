package com.azuratech.azuratime.features.reporting.domain.repository

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.reporting.domain.model.SystemAuditTrail
import java.time.LocalDate

interface ReportRepository {
    suspend fun getAuditLogs(startDate: LocalDate, endDate: LocalDate, schoolId: String): Result<List<SystemAuditTrail>>
}
