package com.azuratech.azuratime.data.repo

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.domain.checkin.model.AttendanceConflict
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏰 DATA INTEGRITY REPOSITORY
 * Monitors total system health: sync debt, structural gaps, relational breakages.
 * Strictly scoped to the active multi-tenant workspace (schoolId).
 * Reacts in real-time via Room Flows — no manual refresh needed.
 */
@Singleton
class DataIntegrityRepository @Inject constructor(
    private val database: AppDatabase,
    private val sessionManager: SessionManager
) {
    private val faceDao = database.faceDao()
    private val recordDao = database.checkInRecordDao()
    private val assignmentDao = database.faceAssignmentDao()
    private val conflictDao = database.attendanceConflictDao()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val schoolIdFlow = sessionManager.activeSchoolIdFlow.map { it ?: "" }

    // =====================================================
    // 📊 VOLUME — How big is the system?
    // =====================================================

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val totalFaces: Flow<Int> = schoolIdFlow.flatMapLatest { id ->
        faceDao.getTotalFacesFlow(id)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val totalRecords: Flow<Int> = schoolIdFlow.flatMapLatest { id ->
        recordDao.getTotalCountFlow(id)
    }

    // =====================================================
    // 🛡️ STRUCTURAL INTEGRITY — Missing data checks
    // =====================================================

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val missingAssignment: Flow<Int> = schoolIdFlow.flatMapLatest { id ->
        assignmentDao.getUnassignedStudentCount(id)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val brokenAssignments: Flow<Int> = schoolIdFlow.flatMapLatest { id ->
        assignmentDao.getBrokenAssignmentsCount(id)
    }

    // =====================================================
    // ☁️ GLOBAL SYNC HEALTH — Total cloud debt across tables
    // =====================================================

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val globalUnsyncedCount: Flow<Int> = schoolIdFlow.flatMapLatest { id ->
        combine(
            faceDao.getUnsyncedFacesCountFlow(id),
            recordDao.getUnsyncedRecordsCountFlow(id),
            assignmentDao.getUnsyncedAssignmentsCountFlow(id)
        ) { face, record, assignment ->
            face + record + assignment
        }
    }

    val conflicts: Flow<List<AttendanceConflict>> = conflictDao.getAllConflicts().map { entities: List<com.azuratech.azuratime.data.local.AttendanceConflictEntity> ->
        entities.map { it.toDomain() }
    }

    // =====================================================
    // 🔧 CORRECTION MODE — Return the specific people who need fixing
    // =====================================================

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getIncompleteProfiles(type: String): Flow<List<FaceEntity>> = schoolIdFlow.flatMapLatest { id ->
        when (type) {
            "CLASS"  -> faceDao.getFacesMissingAssignment(id)
            else     -> flowOf(emptyList())
        }
    }
}