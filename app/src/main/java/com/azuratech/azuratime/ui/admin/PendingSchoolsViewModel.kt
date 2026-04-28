package com.azuratech.azuratime.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.data.repo.SchoolRepository
import com.azuratech.azuratime.domain.school.usecase.SuperAdminApprovalUseCase
import com.azuratech.azuratime.ui.core.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingSchoolsViewModel @Inject constructor(
    private val schoolRepository: SchoolRepository,
    private val approvalUseCase: SuperAdminApprovalUseCase
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val pendingSchools: StateFlow<List<School>> = schoolRepository.observeAllSchools()
        .map { result ->
            if (result is Result.Success) {
                result.data.filter { it.status == "PENDING" }
            } else {
                emptyList()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun approve(schoolId: String) {
        viewModelScope.launch {
            val result = approvalUseCase.approveSchool(schoolId)
            if (result is Result.Success) {
                println("👑 SuperAdmin: Approved school $schoolId")
                _uiEvent.emit(UiEvent.ShowSnackbar("Sekolah berhasil disetujui!"))
            } else if (result is Result.Failure) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Gagal menyetujui sekolah: ${result.error.message}"))
            }
        }
    }

    fun reject(schoolId: String, reason: String) {
        viewModelScope.launch {
            val result = approvalUseCase.rejectSchool(schoolId, reason)
            if (result is Result.Success) {
                println("👑 SuperAdmin: Rejected school $schoolId")
                _uiEvent.emit(UiEvent.ShowSnackbar("Sekolah telah ditolak."))
            } else if (result is Result.Failure) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Gagal menolak sekolah: ${result.error.message}"))
            }
        }
    }
}
