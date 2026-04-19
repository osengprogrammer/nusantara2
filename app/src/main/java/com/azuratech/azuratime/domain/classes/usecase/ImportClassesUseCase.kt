package com.azuratech.azuratime.domain.classes.usecase

import android.net.Uri
import com.azuratech.azuratime.data.repository.RegistrationRepository
import com.azuratech.azuratime.domain.result.AppError
import com.azuratech.azuratime.domain.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 🏰 IMPORT CLASSES USE CASE
 * Consumes the RegistrationRepository's CSV Flow and returns a final Result.
 */
class ImportClassesUseCase @Inject constructor(
    private val registrationRepository: RegistrationRepository
) {
    suspend operator fun invoke(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Collecting the flow to completion. Progress is handled by Repo stubs internally if needed,
            // but for this UseCase we just wait for success/fail.
            registrationRepository.processCsvFile(uri, "ASSIGNMENT").collect()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.BusinessRule(e.message ?: "Gagal memproses file CSV"))
        }
    }
}
