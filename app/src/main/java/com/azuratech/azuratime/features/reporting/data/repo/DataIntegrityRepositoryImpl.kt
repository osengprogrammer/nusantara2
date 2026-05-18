package com.azuratech.azuratime.features.reporting.data.repo

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import com.azuratech.azuratime.features.attendance.data.local.AttendanceConflictEntity
import com.azuratech.azuratime.features.reporting.domain.repository.DataIntegrityRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏰 DATA INTEGRITY REPOSITORY IMPLEMENTATION
 */
@Singleton
class DataIntegrityRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
) : DataIntegrityRepository {
    private val biometricDao = database.biometricDao()
    private val recordDao = database.attendanceRecordDao()
    private val assignmentDao = database.studentClassAssignmentDao()
    private val conflictDao = database.attendanceConflictDao()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val schoolIdFlow = sessionManager.activeSchoolIdFlow.map { it ?: "" }

    override val totalStudentsFlow: Flow<Result<Int>> = schoolIdFlow.flatMapLatest { id ->
        biometricDao.getTotalStudentsCountFlow(id).map { Result.Success(it) as Result<Int> }
    }.catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }

    override val totalRecordsFlow: Flow<Result<Int>> = schoolIdFlow.flatMapLatest { id ->
        recordDao.getTotalCountFlow(id).map { Result.Success(it) as Result<Int> }
    }.catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }

    override val missingAssignmentFlow: Flow<Result<Int>> = schoolIdFlow.flatMapLatest { id ->
        assignmentDao.getUnassignedStudentCount(id).map { Result.Success(it) as Result<Int> }
    }.catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }

    override val brokenAssignmentsFlow: Flow<Result<Int>> = schoolIdFlow.flatMapLatest { id ->
        assignmentDao.getBrokenAssignmentsCount(id).map { Result.Success(it) as Result<Int> }
    }.catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }

    override val globalUnsyncedCountFlow: Flow<Result<Int>> = schoolIdFlow.flatMapLatest { id ->
        combine(
            biometricDao.getUnsyncedStudentsCountFlow(id),
            recordDao.getUnsyncedRecordsCountFlow(id),
            assignmentDao.getUnsyncedAssignmentsCountFlow(id),
        ) { biometric, record, assignment ->
            Result.Success(biometric + record + assignment) as Result<Int>
        }
    }.catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }

    override val conflictsFlow: Flow<Result<List<AttendanceConflictEntity>>> =
        schoolIdFlow.flatMapLatest { id ->
            conflictDao.observeConflictsBySchool(id).map { Result.Success(it) as Result<List<AttendanceConflictEntity>> }
        }.catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }

    override fun getIncompleteProfiles(type: String): Flow<Result<List<StudentBiometricEntity>>> = schoolIdFlow.flatMapLatest { id ->
        when (type) {
            "CLASS" -> biometricDao.getStudentsMissingAssignment(id).map { Result.Success(it) as Result<List<StudentBiometricEntity>> }
            else -> flowOf(Result.Success(emptyList()))
        }
    }.catch { e -> emit(Result.Failure(AppError.LocalDB(e.message))) }
}
