package com.azuratech.azuratime.features.attendance.domain.usecase

import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.attendance.domain.model.AttendanceRecord
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.core.domain.repository.SyncRepository
import com.azuratech.azuratime.core.domain.model.ClassModel
import com.azuratech.azuratime.core.result.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 🔒 OBSERVE ATTENDANCE DATA USE CASE
 * Combines classes observation, attendance records flow, and sync status
 * into a single reactive stream. Keeps the ViewModel free of direct repository dependencies.
 */
class ObserveAttendanceDataUseCase @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val schoolRepository: SchoolRepository,
    private val syncRepository: SyncRepository,
) {
    data class AttendanceData(
        val classes: List<ClassModel>,
        val recordsResult: Result<List<AttendanceRecord>>,
        val isSyncing: Boolean,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        schoolIdFlow: Flow<String?>,
        selectedClassIdFlow: Flow<String?>,
        searchQueryFlow: Flow<String>,
        refreshTriggerFlow: Flow<Int>,
    ): Flow<AttendanceData> {
        val classesFlow = schoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                schoolRepository.observeClassesFlow(schoolId)
                    .map { it.getOrNull() ?: emptyList() }
            }

        val recordsFlow = combine(
            schoolIdFlow.filterNotNull(),
            selectedClassIdFlow.distinctUntilChanged(),
            searchQueryFlow.distinctUntilChanged(),
            refreshTriggerFlow,
        ) { schoolId, classId, query, _ ->
            Triple(schoolId, classId, query)
        }.flatMapLatest { (schoolId, classId, query) ->
            attendanceRepository.getAttendanceRecordsFlow(
                name = query,
                startDate = null,
                endDate = null,
                accountId = null,
                classId = classId,
                assignedIds = emptyList(),
                schoolId = schoolId,
            )
        }

        val syncResultFlow = syncRepository.isSyncingFlow
            .map { it.getOrNull() ?: false }
            .distinctUntilChanged()

        return combine(classesFlow, recordsFlow, syncResultFlow) { classes, recordsResult, isSyncing ->
            AttendanceData(
                classes = classes,
                recordsResult = recordsResult,
                isSyncing = isSyncing,
            )
        }
    }
}
