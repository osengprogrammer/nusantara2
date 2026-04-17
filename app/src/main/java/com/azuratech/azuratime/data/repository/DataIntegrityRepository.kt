package com.azuratech.azuratime.data.repository

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.FaceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
    private val schoolId: String get() = sessionManager.getActiveSchoolId() ?: ""

    private val faceDao = database.faceDao()
    private val recordDao = database.checkInRecordDao()
    private val assignmentDao = database.faceAssignmentDao()

    // =====================================================
    // 📊 VOLUME — How big is the system?
    // =====================================================

    val totalFaces: Flow<Int>
        get() = faceDao.getTotalFacesFlow(schoolId)

    val totalRecords: Flow<Int>
        get() = recordDao.getTotalCountFlow(schoolId)

    // =====================================================
    // 🛡️ STRUCTURAL INTEGRITY — Missing data checks
    // =====================================================

    val missingAssignment: Flow<Int>
        get() = assignmentDao.getUnassignedStudentCount(schoolId)

    val brokenAssignments: Flow<Int>
        get() = assignmentDao.getBrokenAssignmentsCount(schoolId)

    // =====================================================
    // ☁️ GLOBAL SYNC HEALTH — Total cloud debt across tables
    // =====================================================

    val globalUnsyncedCount: Flow<Int>
        get() = combine(
            faceDao.getUnsyncedFacesCountFlow(schoolId),
            recordDao.getUnsyncedRecordsCountFlow(schoolId),
            assignmentDao.getUnsyncedAssignmentsCountFlow(schoolId)
        ) { face, record, assignment ->
            face + record + assignment
        }

    // =====================================================
    // 🔧 CORRECTION MODE — Return the specific people who need fixing
    // =====================================================

    fun getIncompleteProfiles(type: String): Flow<List<FaceEntity>> = when (type) {
        "CLASS"  -> faceDao.getFacesMissingAssignment(schoolId)
        else     -> flowOf(emptyList())
    }
}