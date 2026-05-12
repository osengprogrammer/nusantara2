package com.azuratech.azuratime.features.attendance.data.repo

import com.azuratech.azuratime.features.attendance.data.local.CheckInLocalDataSource
import com.azuratech.azuratime.features.attendance.data.local.CheckInRecordEntity
import com.azuratech.azuratime.data.local.AttendanceConflictEntity
import com.azuratech.azuratime.features.attendance.data.remote.CheckInRemoteDataSource
import com.azuratech.azuratime.features.attendance.domain.model.CheckInRecord
import com.azuratech.azuratime.features.attendance.domain.model.CheckInStatus
import com.azuratech.azuratime.features.attendance.domain.repository.CheckInRepository
import com.azuratech.azuratime.features.attendance.domain.repository.ProcessCheckInParams
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
    private val remoteDataSource: CheckInRemoteDataSource,
    private val syncManager: com.azuratech.azuratime.core.sync.SyncManager,
    private val sessionManager: com.azuratech.azuratime.core.session.SessionManager
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
    ): Flow<List<CheckInRecordEntity>> {
        return localDataSource.getFilteredRecords(
            name, startDate, endDate, userId, classId, assignedIds, schoolId
        )
    }

    override suspend fun saveRecord(record: CheckInRecord): Result<Unit> {
        return try {
            localDataSource.insert(CheckInRecordEntity.fromDomain(record))
            syncManager.enqueueSync() // Background sync
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun updateRecord(recordId: String, classId: String, className: String): Result<Unit> {
        return try {
            val record = checkInRecordDao.getRecordByIdNoSchool(recordId) 
                ?: return Result.Failure(AppError.BusinessRule("Record not found"))

            val updated = record.copy(
                classId = classId,
                className = className,
                isSynced = false
            )
            checkInRecordDao.update(updated)
            syncManager.enqueueSync() // Background sync
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
                syncManager.enqueueSync() // Background sync (will handle remote delete via worker)
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

    override fun getFacesByClass(classId: String, schoolId: String): Flow<List<com.azuratech.azuratime.data.local.BiometricFaceEntity>> {
        return faceAssignmentDao.getFacesByClass(classId, schoolId)
    }

    override fun getStudentCountInClass(classId: String, schoolId: String): Flow<Int> {
        return faceAssignmentDao.getStudentCountInClass(classId, schoolId)
    }

    override fun getClassIdsForFace(faceId: String, schoolId: String): Flow<List<String>> {
        return faceAssignmentDao.getClassIdsForFace(faceId, schoolId)
    }

    override suspend fun getFaceById(faceId: String, schoolId: String): com.azuratech.azuratime.data.local.BiometricFaceEntity? {
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

    override suspend fun syncRecords(): Result<Unit> = withContext(Dispatchers.IO) {
        val schoolId = sessionManager.getActiveSchoolId() ?: ""
        if (schoolId.isBlank()) return@withContext Result.Success(Unit)

        // 1. PUSH PHASE: Upload local changes to cloud
        try {
            val unsyncedRecords = getUnsyncedRecords(schoolId)
            for (record in unsyncedRecords) {
                val syncRes = syncRecord(record)
                if (syncRes is Result.Failure) {
                    if (syncRes.error is AppError.Network) {
                        return@withContext Result.Failure(syncRes.error)
                    }
                }
            }
        } catch (e: Exception) {
            println("ERROR: [CheckInRepository] Error during push phase: ${e.message}")
        }

        // 2. PULL PHASE: Delta sync from cloud to local
        val lastSync = sessionManager.getLastRecordsSyncTime()
        try {
            val syncResult = getRecordUpdates(schoolId, lastSync)
            if (syncResult is Result.Success) {
                val records = syncResult.data
                if (records.isNotEmpty()) {
                    records.forEach { record ->
                        saveRecord(record)
                    }
                    sessionManager.saveLastRecordsSyncTime()
                    println("[CheckInRepository] ✅ Delta Sync: Pulled ${records.size} records")
                }
                Result.Success(Unit)
            } else {
                syncResult as Result.Failure
            }
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun resolveConflict(conflictId: String, useCloud: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val conflict: AttendanceConflictEntity = conflictDao.getConflictById(conflictId)
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

    override suspend fun processCheckIn(params: ProcessCheckInParams): Result<com.azuratech.azuratime.features.attendance.domain.model.CheckInResult> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId() ?: return@withContext Result.Failure(AppError.BusinessRule("School not selected"))
            
            // 1. Check duplicate check-in today
            val today = LocalDate.now()
            val existing = localDataSource.getRecordByFaceAndDate(params.faceId, today, schoolId)
            if (existing != null) {
                return@withContext Result.Success(com.azuratech.azuratime.features.attendance.domain.model.CheckInResult.AlreadyCheckedIn(params.studentName))
            }

            // 2. Class validation
            if (params.activeClassId != null && params.activeClassId !in params.studentClassIds) {
                return@withContext Result.Success(com.azuratech.azuratime.features.attendance.domain.model.CheckInResult.Rejected(params.studentName, "Bukan kelas ini"))
            }

            // 3. Save Record
            val record = CheckInRecord(
                recordId = "rec_${System.currentTimeMillis()}",
                studentId = params.faceId,
                studentName = params.studentName,
                schoolId = schoolId,
                classId = params.activeClassId ?: params.studentClassIds.firstOrNull() ?: "UNASSIGNED",
                className = "Auto", 
                timestamp = System.currentTimeMillis(),
                status = CheckInStatus.PRESENT,
                teacherEmail = params.teacherEmail
            )
            
            saveRecord(record)
            Result.Success(com.azuratech.azuratime.features.attendance.domain.model.CheckInResult.Success(params.studentName, "Berhasil Absen"))
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
