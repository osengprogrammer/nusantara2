package com.azuratech.azuratime.features.attendance.data.repo

import com.azuratech.azuratime.features.attendance.data.local.AttendanceLocalDataSource
import com.azuratech.azuratime.features.attendance.data.local.AttendanceRecordEntity
import com.azuratech.azuratime.features.attendance.data.local.AttendanceConflictEntity
import com.azuratech.azuratime.features.attendance.data.remote.AttendanceRemoteDataSource
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.attendance.domain.repository.ProcessAttendanceParams
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val database: com.azuratech.azuratime.core.data.local.AppDatabase,
    private val localDataSource: AttendanceLocalDataSource,
    private val remoteDataSource: AttendanceRemoteDataSource,
    private val syncManager: com.azuratech.azuratime.core.sync.SyncManager,
    private val sessionManager: com.azuratech.azuratime.core.session.SessionManager,
    private val auditLogRepository: com.azuratech.azuratime.features.reporting.data.repo.AuditLogRepository
) : AttendanceRepository {

    private val attendanceRecordDao = database.attendanceRecordDao()
    private val assignmentDao = database.studentClassAssignmentDao()
    private val biometricDao = database.biometricDao()
    private val conflictDao = database.attendanceConflictDao()

    override fun getAttendanceRecords(
        name: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        accountId: String?,
        classId: String?,
        assignedIds: List<String>,
        schoolId: String
    ): Flow<List<AttendanceRecordEntity>> {
        return localDataSource.getFilteredRecords(
            name, startDate, endDate, accountId, classId, assignedIds, schoolId
        )
    }

    override suspend fun saveRecord(record: AttendanceRecord): Result<Unit> {
        return try {
            localDataSource.insert(AttendanceRecordEntity.fromDomain(record))
            syncManager.enqueueSync() // Background sync
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun updateRecord(recordId: String, classId: String, className: String): Result<Unit> {
        return try {
            val record = attendanceRecordDao.getRecordByIdNoSchool(recordId) 
                ?: return Result.Failure(AppError.BusinessRule("Record not found"))

            val updated = record.copy(
                classId = classId,
                className = className,
                isSynced = false,
                timestamp = System.currentTimeMillis() // Update modification time
            )
            attendanceRecordDao.update(updated)
            syncManager.enqueueSync() 
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    /**
     * 🔥 AI Native: Update Status with Audit Logging
     */
    override suspend fun updateRecordStatus(recordId: String, status: AttendanceStatus, schoolId: String): Result<Unit> {
        return try {
            val record = attendanceRecordDao.getRecordById(recordId, schoolId)
                ?: return Result.Failure(AppError.BusinessRule("Record not found"))

            val updated = record.copy(
                status = status.toCode(),
                isSynced = false
            )
            attendanceRecordDao.update(updated)
            
            auditLogRepository.logAction(
                schoolId = schoolId,
                userId = sessionManager.getUserEmail(), // This might need renaming to getAccountEmail() later
                action = "UPDATE_ATTENDANCE",
                details = "Changed status for ${record.name} to ${status.name}"
            )
            
            syncManager.enqueueSync()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun syncRecord(record: AttendanceRecord): Result<Unit> {
        return try {
            val entity = AttendanceRecordEntity.fromDomain(record)
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
        return attendanceRecordDao.getTodayPresentCount(date, schoolId)
    }

    override fun getUnassignedStudentCount(schoolId: String): Flow<Int> {
        return assignmentDao.getUnassignedStudentCount(schoolId)
    }

    override fun getStudentsByClass(classId: String, schoolId: String): Flow<List<StudentBiometricEntity>> {
        return assignmentDao.getStudentsByClass(classId, schoolId)
    }

    override fun getStudentCountInClass(classId: String, schoolId: String): Flow<Int> {
        return assignmentDao.getStudentCountInClass(classId, schoolId)
    }

    override fun getClassIdsForStudent(studentId: String, schoolId: String): Flow<List<String>> {
        return assignmentDao.getClassIdsForStudent(studentId, schoolId)
    }

    override suspend fun getStudentBiometricById(studentId: String, schoolId: String): StudentBiometricEntity? {
        return biometricDao.getStudentBiometricById(studentId, schoolId)
    }

    override suspend fun getUnsyncedRecords(schoolId: String): List<AttendanceRecord> {
        return localDataSource.getUnsyncedRecords(schoolId).map { it.toDomain() }
    }

    override suspend fun getRecordUpdates(schoolId: String, lastSync: Long): Result<List<AttendanceRecord>> {
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
            println("ERROR: [AttendanceRepository] Error during push phase: ${e.message}")
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
                    println("[AttendanceRepository]  Delta Sync: Pulled ${records.size} records")
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
                attendanceRecordDao.insert(cloudEntity)
                remoteDataSource.syncRecord(cloudEntity)
            } else {
                // If local version is selected, just trigger a sync to cloud
                val localEntity = conflict.local.copy(isSynced = false)
                attendanceRecordDao.update(localEntity)
                val syncResult = remoteDataSource.syncRecord(localEntity)
                if (syncResult is Result.Success) {
                    attendanceRecordDao.update(localEntity.copy(isSynced = true))
                }
            }

            conflictDao.deleteById(conflictId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun processAttendance(params: ProcessAttendanceParams): Result<com.azuratech.azuratime.features.attendance.domain.model.AttendanceResult> = withContext(Dispatchers.IO) {
        try {
            val schoolId = sessionManager.getActiveSchoolId() ?: return@withContext Result.Failure(AppError.BusinessRule("School not selected"))
            
            // 1. 🔥 AI Native: Time-based Duplicate Check
            // We allow re-recording after 10 minutes (600,000 ms)
            val today = LocalDate.now()
            val latest = localDataSource.getLatestRecordForStudent(
                studentId = params.studentId,
                classId = params.activeClassId ?: "",
                date = today,
                schoolId = schoolId
            )

            if (latest != null) {
                val timeDiff = System.currentTimeMillis() - latest.timestamp
                if (timeDiff < 600_000L) { // 10 minute lockout window
                    return@withContext Result.Success(com.azuratech.azuratime.features.attendance.domain.model.AttendanceResult.AlreadyCheckedIn(params.studentName))
                }
            }

            // 2. Class validation
            if (params.activeClassId != null && params.activeClassId !in params.studentClassIds) {
                return@withContext Result.Success(com.azuratech.azuratime.features.attendance.domain.model.AttendanceResult.Rejected(params.studentName, "Bukan kelas ini"))
            }

            // 3. Save Record
            val record = AttendanceRecord(
                recordId = "rec_${System.currentTimeMillis()}",
                studentId = params.studentId,
                studentName = params.studentName,
                schoolId = schoolId,
                classId = params.activeClassId ?: params.studentClassIds.firstOrNull() ?: "UNASSIGNED",
                className = "Auto", 
                timestamp = System.currentTimeMillis(),
                status = AttendanceStatus.PRESENT,
                accountEmail = params.accountEmail
            )
            
            saveRecord(record)
            
            // 🔥 Log to Audit Trail
            auditLogRepository.logAction(
                schoolId = schoolId,
                userId = params.accountEmail,
                action = "CHECK_IN",
                details = "Student: ${params.studentName} (${params.studentId})"
            )
            
            Result.Success(com.azuratech.azuratime.features.attendance.domain.model.AttendanceResult.Success(params.studentName, "Berhasil Absen"))
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }
}
