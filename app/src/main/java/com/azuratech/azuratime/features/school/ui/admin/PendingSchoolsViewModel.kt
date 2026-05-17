package com.azuratech.azuratime.features.school.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.core.ui.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingSchoolsViewModel @Inject constructor(
    private val schoolRepository: SchoolRepository,
) : ViewModel() {

    private val _uiEventFlow = MutableSharedFlow<UiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    val pendingSchoolsFlow: StateFlow<List<School>> = schoolRepository.observeAllSchools()
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
            val result = schoolRepository.approveSchool(schoolId)
            if (result is Result.Success) {
                println("👑 SuperAdmin: Approved school $schoolId")
                _uiEventFlow.emit(UiEvent.ShowSnackbar("Sekolah berhasil disetujui!"))
            } else if (result is Result.Failure) {
                _uiEventFlow.emit(UiEvent.ShowSnackbar("Gagal menyetujui sekolah: ${result.error.message}"))
            }
        }
    }

    fun reject(schoolId: String, reason: String) {
        viewModelScope.launch {
            val result = schoolRepository.rejectSchool(schoolId, reason)
            if (result is Result.Success) {
                println("👑 SuperAdmin: Rejected school $schoolId")
                _uiEventFlow.emit(UiEvent.ShowSnackbar("Sekolah telah ditolak."))
            } else if (result is Result.Failure) {
                _uiEventFlow.emit(UiEvent.ShowSnackbar("Gagal menolak sekolah: ${result.error.message}"))
            }
        }
    }
}
