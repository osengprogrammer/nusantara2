package com.azuratech.azuratime.features.reporting.data.repo

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏰 DATA INTEGRITY REPOSITORY
 * Monitors total system health: sync debt, structural gaps, relational breakages.
 * Strictly scoped to the active multi-tenant workspace (schoolIdFlow).
 * Reacts in real-time via Room Flows — no manual refresh needed.
 */
@Singleton
class DataIntegrityRepository @Inject constructor(
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
) {
    private val biometricDao = database.biometricDao()
    private val recordDao = database.attendanceRecordDao()
    private val assignmentDao = database.studentClassAssignmentDao()
    private val conflictDao = database.attendanceConflictDao()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val schoolIdFlow = sessionManager.activeSchoolIdFlow.map { it ?: "" }

    // =====================================================
    // 📊 VOLUME — How big is the system?
    // =====================================================

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val totalStudentsFlow: Flow<Int> = schoolIdFlow.flatMapLatest { id ->
        biometricDao.getTotalStudentsCountFlow(id)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val totalRecordsFlow: Flow<Int> = schoolIdFlow.flatMapLatest { id ->
        recordDao.getTotalCountFlow(id)
    }

    // =====================================================
    // 🛡️ STRUCTURAL INTEGRITY — Missing data checks
    // =====================================================

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val missingAssignmentFlow: Flow<Int> = schoolIdFlow.flatMapLatest { id ->
        assignmentDao.getUnassignedStudentCount(id)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val brokenAssignmentsFlow: Flow<Int> = schoolIdFlow.flatMapLatest { id ->
        assignmentDao.getBrokenAssignmentsCount(id)
    }

    // =====================================================
    // ☁️ GLOBAL SYNC HEALTH — Total cloud debt across tables
    // =====================================================

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val globalUnsyncedCountFlow: Flow<Int> = schoolIdFlow.flatMapLatest { id ->
        combine(
            biometricDao.getUnsyncedStudentsCountFlow(id),
            recordDao.getUnsyncedRecordsCountFlow(id),
            assignmentDao.getUnsyncedAssignmentsCountFlow(id),
        ) { biometric, record, assignment ->
            biometric + record + assignment
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val conflictsFlow: Flow<List<com.azuratech.azuratime.features.attendance.data.local.AttendanceConflictEntity>> =
        schoolIdFlow.flatMapLatest { id ->
            conflictDao.observeConflictsBySchool(id)
        }

    // =====================================================
    // 🔧 CORRECTION MODE — Return the specific people who need fixing
    // =====================================================

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getIncompleteProfiles(type: String): Flow<List<StudentBiometricEntity>> = schoolIdFlow.flatMapLatest { id ->
        when (type) {
            "CLASS" -> biometricDao.getStudentsMissingAssignment(id)
            else -> flowOf(emptyList())
        }
    }
}
