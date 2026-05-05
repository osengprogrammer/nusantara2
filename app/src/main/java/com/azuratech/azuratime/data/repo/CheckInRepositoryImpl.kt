package com.azuratech.azuratime.data.repo

import com.azuratech.azuratime.data.local.CheckInLocalDataSource
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.data.remote.CheckInRemoteDataSource
import com.azuratech.azuratime.domain.checkin.model.CheckInRecord
import com.azuratech.azuratime.domain.checkin.repository.CheckInRepository
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.AppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInRepositoryImpl @Inject constructor(
    private val database: com.azuratech.azuratime.data.local.AppDatabase,
    private val localDataSource: CheckInLocalDataSource,
    private val remoteDataSource: CheckInRemoteDataSource
) : CheckInRepository {

    private val checkInRecordDao = database.checkInRecordDao()
    private val faceAssignmentDao = database.faceAssignmentDao()
    private val faceDao = database.faceDao()
    private val conflictDao = database.attendanceConflictDao()

    override fun getCheckInRecords(
        name: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        userId: String?,
        classId: String?,
        assignedIds: List<String>,
        schoolId: String
    ): Flow<List<CheckInRecord>> {
        return localDataSource.getFilteredRecords(
            name, startDate, endDate, userId, classId, assignedIds, schoolId
        ).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveRecord(record: CheckInRecord): Result<Unit> {
        return try {
            localDataSource.insert(CheckInRecordEntity.fromDomain(record))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun updateRecord(record: CheckInRecord): Result<Unit> {
        return try {
            localDataSource.update(CheckInRecordEntity.fromDomain(record))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun syncRecord(record: CheckInRecord): Result<Unit> {
        return try {
            val entity = CheckInRecordEntity.fromDomain(record)
            val result = remoteDataSource.syncRecord(entity)
            if (result is Result.Success) {
                localDataSource.update(entity.copy(isSynced = true))
            }
            result
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun deleteRecord(recordId: String, schoolId: String): Result<Unit> {
        return try {
            val entity = localDataSource.getRecordById(recordId, schoolId)
            if (entity != null) {
                localDataSource.delete(entity)
                remoteDataSource.deleteRecord(schoolId, recordId)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override fun getTodayPresentCount(date: LocalDate, schoolId: String): Flow<Int> {
        return checkInRecordDao.getTodayPresentCount(date, schoolId)
    }

    override fun getUnassignedStudentCount(schoolId: String): Flow<Int> {
        return faceAssignmentDao.getUnassignedStudentCount(schoolId)
    }

    override fun getFacesByClass(classId: String, schoolId: String): Flow<List<com.azuratech.azuratime.data.local.FaceEntity>> {
        return faceAssignmentDao.getFacesByClass(classId, schoolId)
    }

    override fun getStudentCountInClass(classId: String, schoolId: String): Flow<Int> {
        return faceAssignmentDao.getStudentCountInClass(classId, schoolId)
    }

    override fun getClassIdsForFace(faceId: String, schoolId: String): Flow<List<String>> {
        return faceAssignmentDao.getClassIdsForFace(faceId, schoolId)
    }

    override suspend fun getFaceById(faceId: String, schoolId: String): com.azuratech.azuratime.data.local.FaceEntity? {
        return faceDao.getFaceById(faceId, schoolId)
    }

    override suspend fun getUnsyncedRecords(schoolId: String): List<CheckInRecord> {
        return localDataSource.getUnsyncedRecords(schoolId).map { it.toDomain() }
    }

    override suspend fun getRecordUpdates(schoolId: String, lastSync: Long): Result<List<CheckInRecord>> {
        val result = remoteDataSource.getRecordUpdates(schoolId, lastSync)
        return when (result) {
            is Result.Success -> Result.Success(result.data.map { it.toDomain() })
            is Result.Failure -> Result.Failure(result.error)
            is Result.Loading -> Result.Loading
        }
    }

    override suspend fun resolveConflict(conflictId: String, useCloud: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val conflict = conflictDao.getConflictById(conflictId)
                ?: return@withContext Result.Failure(AppError.BusinessRule("Conflict not found"))

            if (useCloud) {
                // If cloud version is selected, overwrite local record and sync
                val cloudEntity = conflict.cloud.copy(isSynced = true)
                checkInRecordDao.insert(cloudEntity)
                remoteDataSource.syncRecord(cloudEntity)
            } else {
                // If local version is selected, just trigger a sync to cloud
                val localEntity = conflict.local.copy(isSynced = false)
                checkInRecordDao.update(localEntity)
                val syncResult = remoteDataSource.syncRecord(localEntity)
                if (syncResult is Result.Success) {
                    checkInRecordDao.update(localEntity.copy(isSynced = true))
                }
            }

            conflictDao.deleteById(conflictId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
