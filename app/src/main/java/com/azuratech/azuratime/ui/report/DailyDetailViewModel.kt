package com.azuratech.azuratime.ui.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.ClassEntity
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.data.repo.UserRepository
import com.azuratech.azuratime.domain.checkin.usecase.GetCheckInRecordsUseCase
import com.azuratech.azuratime.domain.checkin.usecase.UpdateCheckInRecordUseCase
import com.azuratech.azuratime.domain.checkin.usecase.DeleteCheckInRecordUseCase
import com.azuratech.azuratime.domain.checkin.usecase.CheckInFilters
import com.azuratech.azuratime.domain.classes.usecase.GetClassesUseCase
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
    private val getCheckInRecordsUseCase: GetCheckInRecordsUseCase,
    private val updateCheckInRecordUseCase: UpdateCheckInRecordUseCase,
    private val deleteCheckInRecordUseCase: DeleteCheckInRecordUseCase,
    private val getClassesUseCase: GetClassesUseCase,
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

    private val checkInRecords = getCheckInRecordsUseCase(
        CheckInFilters(
            startDate = date,
            endDate = date
        )
    ).map { it.getOrNull() ?: emptyList() }

    private val classes = getClassesUseCase().map { 
        if (it is Result.Success) it.data else emptyList() 
    }

    val uiState: StateFlow<DailyDetailUiState> = combine(
        checkInRecords,
        classes,
        assignedClassIds,
        currentUser
    ) { dailyLogs, globalClasses, assignedIds, user ->
        val filteredLogs = dailyLogs.filter { it.faceId == faceId }
            .sortedBy { it.checkInTime ?: it.createdAtDateTime }

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

    fun deleteRecord(record: CheckInRecordEntity) {
        viewModelScope.launch { deleteCheckInRecordUseCase(record.id) }
    }

    fun updateRecord(record: CheckInRecordEntity) {
        viewModelScope.launch { updateCheckInRecordUseCase(record) }
    }

    fun updateRecordClass(record: CheckInRecordEntity, selectedClass: ClassEntity) {
        viewModelScope.launch {
            updateCheckInRecordUseCase.updateClass(record.id, selectedClass.id, selectedClass.name)
        }
    }
}
