package com.azuratech.azuratime.ui.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.ClassEntity
import com.azuratech.azuratime.data.local.CheckInRecordEntity
import com.azuratech.azuratime.data.repo.ReportRepository
import com.azuratech.azuratime.ui.checkin.CheckInViewModel
import com.azuratech.azuratime.ui.classes.ClassViewModel
import com.azuratech.azuratime.ui.user.UserManagementViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DailyDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val checkInViewModel: CheckInViewModel,
    private val userViewModel: UserManagementViewModel,
    private val classViewModel: ClassViewModel
) : ViewModel() {

    private val faceId: String = savedStateHandle["faceId"] ?: ""
    private val dateString: String = savedStateHandle["date"] ?: ""
    private val date: LocalDate = try { LocalDate.parse(dateString) } catch (e: Exception) { LocalDate.now() }

    init {
        checkInViewModel.updateFilters(
            name = "",
            start = date,
            end = date
        )
    }

    val uiState: StateFlow<DailyDetailUiState> = combine(
        checkInViewModel.checkInRecords,
        classViewModel.classes,
        userViewModel.assignedClassIds,
        userViewModel.currentUser
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
        checkInViewModel.deleteRecord(record)
    }

    fun updateRecord(record: CheckInRecordEntity) {
        checkInViewModel.updateRecord(record)
    }

    fun updateRecordClass(record: CheckInRecordEntity, selectedClass: ClassEntity) {
        checkInViewModel.updateRecordClass(record, selectedClass)
    }
}
