package com.azuratech.azuratime.features.reporting.data.repo

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.asLocalResult
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.data.local.AttendanceConflictEntity
import com.azuratech.azuratime.core.data.local.StudentBiometricEntity
import com.azuratech.azuratime.features.reporting.domain.repository.DataIntegrityRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataIntegrityRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
) : DataIntegrityRepository {

    private val biometricDao = database.biometricDao()
    private val assignmentDao = database.studentClassAssignmentDao()
    private val recordDao = database.attendanceRecordDao()
    private val conflictDao = database.attendanceConflictDao()

    private val schoolIdFlow = sessionManager.activeSchoolIdFlow.map { it ?: "" }

    override val totalStudentsFlow: Flow<Result<Int>> = schoolIdFlow.flatMapLatest { id ->
        biometricDao.getTotalStudentsCountFlow(id).asLocalResult()
    }

    override val totalRecordsFlow: Flow<Result<Int>> = schoolIdFlow.flatMapLatest { id ->
        recordDao.getTotalCountFlow(id).asLocalResult()
    }

    override val missingAssignmentFlow: Flow<Result<Int>> = schoolIdFlow.flatMapLatest { id ->
        assignmentDao.getUnassignedStudentCount(id).asLocalResult()
    }

    override val brokenAssignmentsFlow: Flow<Result<Int>> = schoolIdFlow.flatMapLatest { id ->
        assignmentDao.getBrokenAssignmentsCount(id).asLocalResult()
    }

    override val globalUnsyncedCountFlow: Flow<Result<Int>> = schoolIdFlow.flatMapLatest { id ->
        combine(
            biometricDao.getUnsyncedStudentsCountFlow(id),
            recordDao.getUnsyncedRecordsCountFlow(id),
            assignmentDao.getUnsyncedAssignmentsCountFlow(id),
        ) { biometric, record, assignment ->
            biometric + record + assignment
        }.asLocalResult()
    }

    override val conflictsFlow: Flow<Result<List<AttendanceConflictEntity>>> =
        schoolIdFlow.flatMapLatest { id ->
            conflictDao.observeConflictsBySchool(id).asLocalResult()
        }

    override fun getIncompleteProfiles(type: String): Flow<Result<List<StudentBiometricEntity>>> = schoolIdFlow.flatMapLatest { id ->
        when (type) {
            "CLASS" -> biometricDao.getStudentsMissingAssignment(id).asLocalResult()
            else -> flowOf(Result.Success<List<StudentBiometricEntity>>(emptyList()))
        }
    }
}
