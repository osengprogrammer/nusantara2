package com.azuratech.azuratime.domain.classes.usecase

import com.azuratech.azuratime.domain.sync.usecase.ProcessCsvUseCase
import com.azuratech.azuratime.domain.result.AppError
import com.azuratech.azuratime.domain.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 🏰 IMPORT CLASSES USE CASE
 * Consumes the ProcessCsvUseCase's CSV Flow and returns a final Result.
 */
class ImportClassesUseCase @Inject constructor(
    private val processCsvUseCase: ProcessCsvUseCase
) {
    suspend operator fun invoke(uriString: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Collecting the flow to completion. Progress is handled by UseCase internally if needed,
            // but for this UseCase we just wait for success/fail.
            processCsvUseCase(uriString, "ASSIGNMENT").collect()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.BusinessRule(e.message ?: "Gagal memproses file CSV"))
        }
    }
}
