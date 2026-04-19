package com.azuratech.azuratime.domain.report.usecase

import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

/**
 * UseCase to handle complex report data fetching and filtering.
 */
class GetReportDataUseCase @Inject constructor(
    private val database: AppDatabase,
    private val sessionManager: SessionManager
) {
    private val checkInRecordDao = database.checkInRecordDao()
    private val faceDao = database.faceDao()
    private val classDao = database.classDao()
    private val faceAssignmentDao = database.faceAssignmentDao()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getAvailableClasses(role: String, assignedIds: List<String>): Flow<List<ClassEntity>> =
        sessionManager.activeSchoolIdFlow.filterNotNull().flatMapLatest { schoolId ->
            if (role == "ADMIN" || role == "SUPER_USER") {
                classDao.observeClassesBySchool(schoolId)
            } else {
                if (assignedIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    classDao.getClassesByIdsFlow(schoolId, assignedIds)
                }
            }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getStudentsInReport(role: String, selectedClassId: String?, assignedIds: List<String>): Flow<List<FaceEntity>> =
        sessionManager.activeSchoolIdFlow.filterNotNull().flatMapLatest { schoolId ->
            val isAdmin = role == "ADMIN" || role == "SUPER_USER"
            if (isAdmin) {
                if (selectedClassId == "ALL" || selectedClassId == null) {
                    faceDao.getAllFacesFlow(schoolId)
                } else {
                    faceAssignmentDao.getFacesByClass(selectedClassId, schoolId)
                }
            } else {
                if (assignedIds.isEmpty()) return@flatMapLatest flowOf(emptyList())
                if (selectedClassId == "ALL" || selectedClassId == null) {
                    faceAssignmentDao.getFacesByMultipleClasses(assignedIds, schoolId)
                } else {
                    if (assignedIds.contains(selectedClassId)) {
                        faceAssignmentDao.getFacesByClass(selectedClassId, schoolId)
                    } else {
                        flowOf(emptyList())
                    }
                }
            }
        }

    fun getAttendanceHistory(schoolId: String, role: String, start: LocalDate, end: LocalDate, classId: String?, assignedIds: List<String>): Flow<List<CheckInRecordEntity>> {
        val isAdmin = role == "ADMIN" || role == "SUPER_USER"
        val targetClassId = if (classId == "ALL") null else classId

        return if (isAdmin) {
            checkInRecordDao.getFilteredRecords(
                schoolId = schoolId,
                startDate = start,
                endDate = end,
                classId = targetClassId
            )
        } else {
            if (assignedIds.isEmpty()) return flowOf(emptyList())
            checkInRecordDao.getFilteredRecords(
                schoolId = schoolId,
                startDate = start,
                endDate = end,
                classId = targetClassId,
                assignedIds = assignedIds
            )
        }
    }
}
