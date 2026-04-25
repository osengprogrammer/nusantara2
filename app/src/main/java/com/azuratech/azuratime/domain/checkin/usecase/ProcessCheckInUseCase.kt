package com.azuratech.azuratime.domain.checkin.usecase

import com.azuratech.azuratime.data.local.CheckInLocalDataSource
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.data.remote.CheckInRemoteDataSource
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Result of a check-in operation.
 */
sealed class CheckInResult {
    data class Success(val message: String) : CheckInResult()
    data class Rejected(val message: String) : CheckInResult()
}

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
 */
class ProcessCheckInUseCase @Inject constructor(
    private val localDataSource: CheckInLocalDataSource,
    private val remoteDataSource: CheckInRemoteDataSource,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(params: ProcessCheckInParams): Result<CheckInResult> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            if (schoolId.isBlank()) return@withContext Result.Failure(AppError.BusinessRule("No active school found"))

            val targetClassId: String
            val message: String
            val isRejected: Boolean

            if (params.activeClassId != null) {
                if (params.studentClassIds.contains(params.activeClassId)) {
                    targetClassId = params.activeClassId
                    message = "✅ Berhasil: ${params.studentName}"
                    isRejected = false
                } else {
                    return@withContext Result.Success(CheckInResult.Rejected("❌ Ditolak: ${params.studentName} beda kelas!"))
                }
            } else {
                targetClassId = params.studentClassIds.firstOrNull() ?: "UNASSIGNED"
                message = if (targetClassId == "UNASSIGNED") {
                    "⚠️ ${params.studentName} absen (Belum Masuk Kelas)"
                } else {
                    "✅ Gerbang: ${params.studentName} hadir."
                }
                isRejected = false
            }

            val record = CheckInRecordEntity(
                faceId = params.faceId,
                name = params.studentName,
                userId = params.teacherEmail,
                classId = targetClassId,
                className = "Terekam",
                schoolId = schoolId,
                status = "H",
                attendanceDate = LocalDate.now(),
                checkInTime = LocalDateTime.now(),
                isSynced = false
            )

            localDataSource.insert(record)

            // Optional: Immediate sync if requested or required by legacy behavior
            try {
                val syncRes = remoteDataSource.syncRecord(record)
                if (syncRes is Result.Success) {
                    localDataSource.update(record.copy(isSynced = true))
                }
            } catch (e: Exception) {
                // Ignore sync errors here, they will be caught by background sync
            }

            Result.Success(CheckInResult.Success(message))

        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    /**
     * Legacy adapter to save a pre-constructed record entity.
     */
    suspend operator fun invoke(record: CheckInRecordEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            localDataSource.insert(record)
            if (record.schoolId.isNotBlank()) {
                val syncRes = remoteDataSource.syncRecord(record)
                if (syncRes is Result.Success) {
                    localDataSource.update(record.copy(isSynced = true))
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
