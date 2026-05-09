package com.azuratech.azuratime.ui.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.repo.UserRepository
import com.azuratech.azuratime.domain.checkin.model.CheckInRecord
import com.azuratech.azuratime.domain.user.usecase.ObserveUserUseCase
import com.azuratech.azuraengine.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DailyDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: com.azuratech.azuratime.domain.checkin.repository.CheckInRepository,
    private val schoolRepository: com.azuratech.azuratime.data.repo.SchoolRepository,
    private val observeUserUseCase: ObserveUserUseCase,
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val faceId: String = savedStateHandle["faceId"] ?: ""
    private val dateString: String = savedStateHandle["date"] ?: ""
    private val date: LocalDate = try { LocalDate.parse(dateString) } catch (e: Exception) { LocalDate.now() }

    private val currentUser = sessionManager.currentUserIdFlow
        .filterNotNull()
        .flatMapLatest { uid -> observeUserUseCase(uid) }

    private val assignedClassIds = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .combine(sessionManager.currentUserIdFlow.filterNotNull()) { schoolId, userId -> schoolId to userId }
        .flatMapLatest { (schoolId, userId) -> userRepository.getUserClassAccessDao().observeClassIdsForUser(userId, schoolId) }

    private val checkInRecords = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            repository.getCheckInRecords(
                name = "",
                startDate = date,
                endDate = date,
                userId = null,
                classId = null,
                assignedIds = emptyList(),
                schoolId = schoolId
            ).map { entities -> entities.map { it.toDomain() } }
        }

    private val classes = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            schoolRepository.observeClasses(schoolId).map { 
                if (it is Result.Success) it.data else emptyList() 
            }
        }

    val uiState: StateFlow<DailyDetailUiState> = combine(
        checkInRecords,
        classes,
        assignedClassIds,
        currentUser
    ) { dailyLogs, globalClasses, assignedIds, user ->
        val filteredLogs = dailyLogs.filter { it.studentId == faceId }
            .sortedBy { it.timestamp }

        val activeSchoolId = user?.activeSchoolId
        val isAdmin = activeSchoolId != null && user?.memberships?.get(activeSchoolId)?.role == "ADMIN"

        DailyDetailUiState.Success(
            DailyDetailData(
                filteredLogs = filteredLogs,
                globalClasses = globalClasses,
                assignedIds = assignedIds,
                isAdmin = isAdmin
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailyDetailUiState.Loading
    )

    fun deleteRecord(record: CheckInRecord) {
        viewModelScope.launch { 
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            repository.deleteRecord(record.recordId, schoolId) 
        }
    }

    fun updateRecord(record: CheckInRecord) {
        viewModelScope.launch { 
            repository.updateRecord(record.recordId, record.classId, record.className)
        }
    }

    fun updateRecordClass(record: CheckInRecord, selectedClass: com.azuratech.azuraengine.model.ClassModel) {
        viewModelScope.launch {
            repository.updateRecord(record.recordId, selectedClass.id, selectedClass.name)
        }
    }
}
