package com.azuratech.azuratime.features.reporting.domain.repository

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.reporting.domain.model.ExportJobProfile
import kotlinx.coroutines.flow.Flow

interface ExportRepository {
    fun observeExportJobs(accountId: String): Flow<Result<List<ExportJobProfile>>>
    suspend fun startExport(format: String, accountId: String, schoolId: String): Result<String>
    suspend fun clearCompletedJobs(accountId: String): Result<Unit>
}
