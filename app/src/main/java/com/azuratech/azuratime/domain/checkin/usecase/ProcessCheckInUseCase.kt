package com.azuratech.azuratime.domain.checkin.usecase

import com.azuratech.azuratime.domain.checkin.model.CheckInRecord
import com.azuratech.azuratime.domain.checkin.model.CheckInResult
import com.azuratech.azuratime.domain.checkin.model.CheckInStatus
import com.azuratech.azuratime.domain.checkin.repository.CheckInRepository
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * Parameters for processing a check-in.
 */
data class ProcessCheckInParams(
    val faceId: String,
    val studentName: String,
    val teacherEmail: String,
    val activeClassId: String?,
    val studentClassIds: List<String>
)

/**
 * UseCase to process a face scan and record a check-in.
 * 🔥 Clean Architecture compliant: No data layer or Android dependencies.
 */
class ProcessCheckInUseCase @Inject constructor(
    private val repository: CheckInRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(params: ProcessCheckInParams): Result<CheckInResult> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            if (schoolId.isBlank()) return@withContext Result.Failure(AppError.BusinessRule("No active school found"))

            val targetClassId: String
            val message: String

            if (params.activeClassId != null) {
                if (params.studentClassIds.contains(params.activeClassId)) {
                    targetClassId = params.activeClassId
                    message = "✅ Berhasil: ${params.studentName}"
                } else {
                    return@withContext Result.Success(
                        CheckInResult.Rejected(params.studentName, "❌ Ditolak: beda kelas!")
                    )
                }
            } else {
                targetClassId = params.studentClassIds.firstOrNull() ?: "UNASSIGNED"
                message = if (targetClassId == "UNASSIGNED") {
                    "⚠️ ${params.studentName} absen (Belum Masuk Kelas)"
                } else {
                    "✅ Gerbang: ${params.studentName} hadir."
                }
            }

            val record = CheckInRecord(
                recordId = UUID.randomUUID().toString(),
                studentId = params.faceId,
                studentName = params.studentName,
                teacherEmail = params.teacherEmail,
                classId = targetClassId,
                className = "Terekam",
                schoolId = schoolId,
                status = CheckInStatus.PRESENT,
                timestamp = System.currentTimeMillis(),
                isSynced = false
            )

            val saveResult = repository.saveRecord(record)
            if (saveResult is Result.Failure) return@withContext Result.Failure(saveResult.error)

            // Optional: Immediate sync attempt
            repository.syncRecord(record)

            Result.Success(CheckInResult.Success(params.studentName, message))

        } catch (e: Exception) {
            Result.Failure(AppError.BusinessRule(e.message))
        }
    }

    /**
     * Legacy adapter to save a domain record directly.
     */
    suspend operator fun invoke(record: CheckInRecord): Result<Unit> = withContext(Dispatchers.IO) {
        repository.saveRecord(record)
    }
}
