package com.azuratech.azuratime.features.school.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.result.onFailure
import com.azuratech.azuratime.core.result.onSuccess
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 👑 PENDING SCHOOLS VIEW MODEL (v3.2.0-ai-native)
 * Strict MVI implementation for school approval management.
 */
@HiltViewModel
class PendingSchoolsViewModel @Inject constructor(
    private val schoolRepository: SchoolRepository,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(PendingSchoolsUiState())
    val uiStateFlow: StateFlow<PendingSchoolsUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<PendingSchoolsUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    init {
        loadPendingSchools()
    }

    fun onEvent(event: PendingSchoolsUiEvent) {
        when (event) {
            is PendingSchoolsUiEvent.LoadPending -> loadPendingSchools()
            is PendingSchoolsUiEvent.ApproveSchool -> handleApprove(event.schoolId)
            is PendingSchoolsUiEvent.RejectSchool -> handleReject(event.schoolId, event.reason)
            is PendingSchoolsUiEvent.ClearError -> _uiStateFlow.update { it.copy(error = null) }
        }
    }

    private fun loadPendingSchools() {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            schoolRepository.observeAllSchoolsFlow().collectLatest { result ->
                result.onSuccess { schools ->
                    _uiStateFlow.update {
                        it.copy(
                            pendingSchools = schools.filter { it.status == "PENDING" },
                            isLoading = false,
                        )
                    }
                }.onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
                }
            }
        }
    }

    private fun handleApprove(schoolId: String) {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            schoolRepository.approveSchool(schoolId)
                .onSuccess {
                    _uiStateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(PendingSchoolsUiEffect.ShowSnackbar("School approved successfully!"))
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
                    _uiEffectFlow.emit(PendingSchoolsUiEffect.ShowSnackbar("Failed to approve school: ${error.message}"))
                }
        }
    }

    private fun handleReject(schoolId: String, reason: String) {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            schoolRepository.rejectSchool(schoolId, reason)
                .onSuccess {
                    _uiStateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(PendingSchoolsUiEffect.ShowSnackbar("School has been rejected."))
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
                    _uiEffectFlow.emit(PendingSchoolsUiEffect.ShowSnackbar("Failed to reject school: ${error.message}"))
                }
        }
    }
}
