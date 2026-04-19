package com.azuratech.azuratime.data.repository

import android.app.Application
import android.util.Log
import com.azuratech.azuratime.data.local.CheckInLocalDataSource
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.data.remote.CheckInRemoteDataSource
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.domain.checkin.usecase.*
import com.azuratech.azuratime.domain.result.AppError
import com.azuratech.azuratime.domain.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInRepository @Inject constructor(
    private val application: Application,
    private val localDataSource: CheckInLocalDataSource,
    private val remoteDataSource: CheckInRemoteDataSource,
    private val sessionManager: SessionManager,
    private val getCheckInRecordsUseCase: GetCheckInRecordsUseCase,
    private val processCheckInUseCase: ProcessCheckInUseCase,
    private val updateCheckInRecordUseCase: UpdateCheckInRecordUseCase,
    private val deleteCheckInRecordUseCase: DeleteCheckInRecordUseCase,
    private val syncCheckInRecordsUseCase: SyncCheckInRecordsUseCase
) {
    private val schoolId: String get() = sessionManager.getActiveSchoolId() ?: ""

    private var _activeClassId: String? = null

    @Deprecated("Route through UseCase layer")
    fun setActiveClass(classId: String?) {
        _activeClassId = classId
    }

    // =====================================================
    // 📖 READ (FLOWS)
    // =====================================================

    @Deprecated("Route through GetCheckInRecordsUseCase", ReplaceWith("getCheckInRecordsUseCase(filters)"))
    fun getFilteredRecords(
        nameFilter: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        userId: String?,
        classId: String?,
        assignedIds: List<String>,
        schoolId: String = this.schoolId
    ): Flow<Result<List<CheckInRecordEntity>>> {
        val filters = CheckInFilters(
            name = nameFilter,
            startDate = startDate,
            endDate = endDate,
            userId = userId,
            classId = classId,
            assignedIds = assignedIds
        )
        return getCheckInRecordsUseCase(filters)
    }

    // =====================================================
    // 📥 PULL & DELTA SYNC
    // =====================================================

    @Deprecated("Route through SyncCheckInRecordsUseCase", ReplaceWith("syncCheckInRecordsUseCase()"))
    suspend fun performRecordsDeltaSync(): Result<Unit> = syncCheckInRecordsUseCase()

    // =====================================================
    // ✍️ WRITE & SYNC
    // =====================================================

    @Deprecated("Route through ProcessCheckInUseCase")
    suspend fun saveRecord(record: CheckInRecordEntity): Result<Unit> = withContext(Dispatchers.IO) {
        // This is a legacy adapter for saveRecord. 
        // In the new architecture, we should use ProcessCheckInUseCase which handles validation.
        try {
            localDataSource.insert(record)
            if (record.schoolId.isNotBlank()) {
                remoteDataSource.syncRecord(record)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    @Deprecated("Route through UpdateCheckInRecordUseCase", ReplaceWith("updateCheckInRecordUseCase.updateClass(...)"))
    suspend fun updateRecordClass(recordId: String, classId: String, className: String): Result<Unit> = 
        updateCheckInRecordUseCase.updateClass(recordId, classId, className)

    @Deprecated("Route through UpdateCheckInRecordUseCase", ReplaceWith("updateCheckInRecordUseCase(record)"))
    suspend fun updateRecord(record: CheckInRecordEntity): Result<Unit> = 
        updateCheckInRecordUseCase(record)

    @Deprecated("Route through DeleteCheckInRecordUseCase", ReplaceWith("deleteCheckInRecordUseCase(recordId)"))
    suspend fun deleteRecord(record: CheckInRecordEntity): Result<Unit> = 
        deleteCheckInRecordUseCase(record.id)

    // =====================================================
    // ☁️ CLOUD OPERATIONS
    // =====================================================

    @Deprecated("Route through UseCase layer")
    suspend fun syncRecordToCloud(record: CheckInRecordEntity): Result<Unit> =
        remoteDataSource.syncRecord(record)

    @Deprecated("Route through UseCase layer")
    suspend fun deleteRecordFromCloud(schoolId: String, recordId: String): Result<Unit> =
        remoteDataSource.deleteRecord(schoolId, recordId)
}
