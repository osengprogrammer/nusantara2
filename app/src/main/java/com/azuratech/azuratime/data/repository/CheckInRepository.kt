package com.azuratech.azuratime.data.repository

import android.app.Application
import android.util.Log
import com.azuratech.azuratime.data.local.CheckInLocalDataSource
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.data.remote.CheckInRemoteDataSource
import com.azuratech.azuratime.core.session.SessionManager
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
    private val sessionManager: SessionManager
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

    @Deprecated("Route through UseCase layer")
    fun getFilteredRecords(
        nameFilter: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        userId: String?,
        classId: String?,
        assignedIds: List<String>,
        schoolId: String = this.schoolId
    ): Flow<Result<List<CheckInRecordEntity>>> = localDataSource.getFilteredRecords(
            nameFilter = nameFilter,
            startDate = startDate,
            endDate = endDate,
            userId = userId,
            classId = classId,
            assignedIds = assignedIds,
            schoolId = schoolId
        ).map { Result.Success(it) as Result<List<CheckInRecordEntity>> }
         .catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }

    // =====================================================
    // 📥 PULL & DELTA SYNC
    // =====================================================

    @Deprecated("Route through UseCase layer")
    suspend fun performRecordsDeltaSync(): Result<Unit> = withContext(Dispatchers.IO) {
        if (schoolId.isBlank()) return@withContext Result.Success(Unit)

        val lastSync = sessionManager.getLastRecordsSyncTime()
        try {
            val syncResult = remoteDataSource.getRecordUpdates(schoolId, lastSync)
            if (syncResult is Result.Success) {
                val records = syncResult.data
                if (records.isNotEmpty()) {
                    records.forEach { record ->
                        localDataSource.insert(record)
                    }
                    sessionManager.saveLastRecordsSyncTime()
                    Log.i("CheckInRepository", "✅ Delta Sync: Pulled ${records.size} records")
                }
                Result.Success(Unit)
            } else {
                syncResult as Result.Failure
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    // =====================================================
    // ✍️ WRITE & SYNC
    // =====================================================

    @Deprecated("Route through UseCase layer")
    suspend fun saveRecord(record: CheckInRecordEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            localDataSource.insert(record)

            if (record.schoolId.isNotBlank()) {
                val syncRes = remoteDataSource.syncRecord(record)
                if (syncRes is Result.Failure) throw Exception("Sync failed")
                localDataSource.update(record.copy(isSynced = true))
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    @Deprecated("Route through UseCase layer")
    suspend fun updateRecordClass(recordId: String, classId: String, className: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val record = localDataSource.getRecordById(recordId, schoolId)
            record?.let {
                val updatedRecord = it.copy(
                    classId = classId,
                    className = className,
                    isSynced = false
                )
                localDataSource.update(updatedRecord)

                if (updatedRecord.schoolId.isNotBlank()) {
                    val syncRes = remoteDataSource.syncRecord(updatedRecord)
                    if (syncRes is Result.Failure) throw Exception("Sync failed")
                    localDataSource.update(updatedRecord.copy(isSynced = true))
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    @Deprecated("Route through UseCase layer")
    suspend fun updateRecord(record: CheckInRecordEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val recordToUpdate = record.copy(isSynced = false)
            localDataSource.update(recordToUpdate)

            if (recordToUpdate.schoolId.isNotBlank()) {
                val syncRes = remoteDataSource.syncRecord(recordToUpdate)
                if (syncRes is Result.Failure) throw Exception("Sync failed")
                localDataSource.update(recordToUpdate.copy(isSynced = true))
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    @Deprecated("Route through UseCase layer")
    suspend fun deleteRecord(record: CheckInRecordEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            localDataSource.delete(record)

            if (record.schoolId.isNotBlank()) {
                val deleteRes = remoteDataSource.deleteRecord(schoolId, record.id)
                if (deleteRes is Result.Failure) throw Exception("Delete failed")
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

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
