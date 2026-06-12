package com.azuratech.azuratime.features.attendance.data.repo

import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.asLocalResult
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.domain.sync.ExportUtils
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.features.attendance.data.local.AttendanceConflictEntity
import com.azuratech.azuratime.features.attendance.data.local.AttendanceLocalDataSource
import com.azuratech.azuratime.features.attendance.data.local.AttendanceRecordEntity
import com.azuratech.azuratime.features.attendance.data.remote.AttendanceRemoteDataSource
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceStatus
import com.azuratech.azuratime.features.session.domain.model.SessionType
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.attendance.domain.repository.ProcessAttendanceParams
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import com.azuratech.azuratime.features.reporting.domain.repository.AuditLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏛️ ATTENDANCE REPOSITORY IMPLEMENTATION (v3.7.0-base)
 * Unified SSOT for check-in records and export engines.
 * Supports tiered attendance resolution and reporting.
 */
@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val localDataSource: AttendanceLocalDataSource,
    private val remoteDataSource: AttendanceRemoteDataSource,
    private val syncManager: SyncManager,
    private val sessionManager: SessionManager,
    private val auditLogRepository: AuditLogRepository,
    private val exportUtils: ExportUtils,
) : AttendanceRepository {

    private val attendanceRecordDao = database.attendanceRecordDao()
    private val assignmentDao = database.studentClassAssignmentDao()
    private val biometricDao = database.biometricDao()
    private val conflictDao = database.attendanceConflictDao()

    override fun getAttendanceRecordsFlow(
        name: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        accountId: String?,
        classId: String?,
        assignedIds: List<String>,
        schoolId: String,
    ): Flow<Result<List<AttendanceRecordEntity>>> {
        return localDataSource.getFilteredRecords(
            name,
            startDate,
            endDate,
            accountId,
            classId,
            assignedIds,
            schoolId,
        ).asLocalResult()
    }

    override suspend fun saveRecord(record: AttendanceRecord, sessionId: String?): Result<Unit> {
        return try {
            val entity = AttendanceRecordEntity.fromDomain(record).let {
                if (sessionId != null) it.copy(sessionId = sessionId) else it
            }
            localDataSource.insert(entity)
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
                timestamp = System.currentTimeMillis(), // Update modification time
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
                isSynced = false,
            )
            attendanceRecordDao.update(updated)

            auditLogRepository.logAction(
                schoolId = schoolId,
                accountId = sessionManager.getAccountEmail(),
                action = "UPDATE_ATTENDANCE",
                details = "Changed status for ${record.name} to ${status.name}",
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

    override suspend fun getStudentHistory(studentId: String): Result<List<AttendanceRecord>> {
        return try {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            val records = attendanceRecordDao.getStudentHistory(studentId, schoolId)
            Result.Success(records.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override fun getTodayPresentCountFlow(date: LocalDate, schoolId: String): Flow<Result<Int>> {
        return attendanceRecordDao.getTodayPresentCount(date, schoolId)
            .asLocalResult()
    }

    override fun getUnassignedStudentCountFlow(schoolId: String): Flow<Result<Int>> {
        return assignmentDao.getUnassignedStudentCount(schoolId)
            .asLocalResult()
    }

    override fun getStudentsByClassFlow(classId: String, schoolId: String): Flow<Result<List<StudentBiometricEntity>>> {
        return assignmentDao.getStudentsByClass(classId, schoolId)
            .asLocalResult()
    }

    override fun getStudentCountInClassFlow(classId: String, schoolId: String): Flow<Result<Int>> {
        return assignmentDao.getStudentCountInClass(classId, schoolId)
            .asLocalResult()
    }

    override fun getClassIdsForStudentFlow(studentId: String, schoolId: String): Flow<Result<List<String>>> {
        return assignmentDao.getClassIdsForStudent(studentId, schoolId)
            .asLocalResult()
    }

    override suspend fun getStudentBiometricById(studentId: String, schoolId: String): Result<StudentBiometricEntity> {
        return try {
            val entity = biometricDao.getStudentBiometricById(studentId, schoolId)
            if (entity != null) {
                Result.Success(entity)
            } else {
                Result.Failure(AppError.BusinessRule("ENTITY_NOT_FOUND"))
            }
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override suspend fun getUnsyncedRecords(schoolId: String): Result<List<AttendanceRecord>> {
        return try {
            Result.Success(localDataSource.getUnsyncedRecords(schoolId).map { it.toDomain() })
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
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
        if (schoolId.isBlank()) {
            android.util.Log.w("AttendanceRepo", "⚠️ Aborting sync: No active school ID.")
            return@withContext Result.Success(Unit)
        }

        // 1. PUSH PHASE
        try {
            val unsyncedResult = getUnsyncedRecords(schoolId)
            if (unsyncedResult is Result.Success) {
                val records = unsyncedResult.data
                for (record in records) {
                    syncRecord(record)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AttendanceRepo", "❌ Push error: ${e.message}")
        }

        // 2. PULL PHASE
        val lastSync = sessionManager.getLastRecordsSyncTime()
        try {
            val syncResult = getRecordUpdates(schoolId, lastSync)
            if (syncResult is Result.Success) {
                val entities = syncResult.data.map { AttendanceRecordEntity.fromDomain(it) }
                attendanceRecordDao.insertAll(entities)
                sessionManager.saveLastRecordsSyncTime()
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
                val cloudEntity = conflict.cloud.copy(isSynced = true)
                attendanceRecordDao.insert(cloudEntity)
                remoteDataSource.syncRecord(cloudEntity)
            } else {
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

            val recordTimestamp = params.timestamp ?: System.currentTimeMillis()
            val logicalDate = java.time.Instant.ofEpochMilli(recordTimestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()

            // 1. Duplicate Check (10m window)
            val latest = localDataSource.getLatestRecordForStudent(
                studentId = params.studentId,
                classId = params.activeClassId ?: "",
                date = logicalDate,
                schoolId = schoolId,
            )

            if (latest != null) {
                val timeDiff = recordTimestamp - latest.timestamp
                if (timeDiff >= 0 && timeDiff < 600_000L) {
                    return@withContext Result.Success(com.azuratech.azuratime.features.attendance.domain.model.AttendanceResult.AlreadyCheckedIn(params.studentName))
                }
            }

            // 2. Class Resolution
            val selectedClassId = params.activeClassId
            val (resolvedClassId, resolvedClassName) = if (selectedClassId != null) {
                val className = database.classDao().getClassById(selectedClassId)?.name ?: "Kelas"
                selectedClassId to className
            } else {
                "UNASSIGNED" to "Scan Bebas"
            }

            // 3. Session Tier Resolution
            val sessionType = params.sessionId?.let { sid ->
                database.sessionDao().getSessionById(sid)?.sessionType
            } ?: SessionType.ACADEMIC

            val record = AttendanceRecord(
                recordId = "rec_${System.currentTimeMillis()}",
                studentId = params.studentId,
                studentName = params.studentName,
                schoolId = schoolId,
                classId = resolvedClassId,
                className = resolvedClassName,
                sessionId = params.sessionId,
                sessionType = sessionType,
                timestamp = recordTimestamp,
                status = params.status,
                accountEmail = params.accountEmail,
            )

            saveRecord(record, params.sessionId)

            auditLogRepository.logAction(
                schoolId = schoolId,
                accountId = params.accountEmail,
                action = "CHECK_IN",
                details = "Student: ${params.studentName}",
            )

            Result.Success(com.azuratech.azuratime.features.attendance.domain.model.AttendanceResult.Success(params.studentName, "Berhasil Absen"))
        } catch (e: Exception) {
            Result.Failure(AppError.LocalDB(e.message))
        }
    }

    override fun getAttendanceByTierFlow(schoolId: String, sessionType: SessionType): Flow<Result<List<AttendanceRecordEntity>>> {
        return attendanceRecordDao.getRecordsByTier(schoolId, sessionType.name).asLocalResult()
    }

    override fun getTierSummaryCountFlow(schoolId: String, sessionType: SessionType, date: LocalDate): Flow<Result<Int>> {
        return attendanceRecordDao.getTierSummaryCount(schoolId, sessionType.name, date).asLocalResult()
    }

    override suspend fun exportLogs(records: List<AttendanceRecord>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val path = exportUtils.exportRawLogsToCsv(records)
            if (path != null) {
                Result.Success(path)
            } else {
                Result.Failure(AppError.BusinessRule("Gagal mengekspor log ke CSV."))
            }
        } catch (e: Exception) {
            Result.Failure(AppError.BusinessRule(e.message))
        }
    }
}
