package com.azuratech.azuratime.features.biometric.ui.enroll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.toProfile
import com.azuratech.azuratime.features.biometric.domain.repository.BiometricFaceRepository
import com.azuratech.azuratime.domain.model.BiometricEnrollmentProfile
import com.azuratech.azuratime.ui.core.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🛠️ BIOMETRIC VIEW MODEL - Phase 7.9 SSOT Migration
 * Observes BiometricEnrollmentProfile stream directly from Room.
 */
@HiltViewModel
class BiometricViewModel @Inject constructor(
    private val faceRepository: BiometricFaceRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val enrollmentList: StateFlow<List<BiometricEnrollmentProfile>> = 
        sessionManager.activeSchoolIdFlow.filterNotNull()
            .flatMapLatest { schoolId -> 
                faceRepository.observeEnrollmentsBySchool(schoolId)
                    .map { entities -> entities.map { it.toProfile() } }
            }
            .combine(_searchQuery.debounce(300)) { list, query ->
                if (query.isBlank()) list
                else list.filter { it.studentName.contains(query, ignoreCase = true) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun enrollFace(studentId: String, photoUri: String) {
        viewModelScope.launch {
            val result = faceRepository.submitEnrollment(studentId, photoUri)
            if (result is Result.Success) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Enrollment queued"))
            } else if (result is Result.Failure) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Gagal pendaftaran: ${result.error.message}"))
            }
        }
    }
    
    fun deleteEnrollment(faceId: String) {
        viewModelScope.launch {
            val result = faceRepository.deleteEnrollment(faceId)
            if (result is Result.Success) {
                _uiEvent.emit(UiEvent.ShowSnackbar("Deleted"))
            }
        }
    }
}
