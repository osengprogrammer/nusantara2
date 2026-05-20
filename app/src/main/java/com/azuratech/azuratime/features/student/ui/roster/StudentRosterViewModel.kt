package com.azuratech.azuratime.features.student.ui.roster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import com.azuratech.azuratime.features.student.ui.components.StudentDisplayItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🎓 STUDENT ROSTER VIEW MODEL (v3.2.0-ai-native)
 * Refactored to Strict MVI & SSOT.
 */
@HiltViewModel
class StudentRosterViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(StudentRosterUiState())
    val uiStateFlow: StateFlow<StudentRosterUiState> = _uiStateFlow.asStateFlow()

    // Internal flows for reactive filtering
    private val _searchQueryFlow = MutableStateFlow("")
    private val _selectedClassIdFlow = MutableStateFlow<String?>(null)

    private val _allClassesFlow = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            schoolRepository.observeClasses(schoolId).map { result ->
                result.getOrNull() ?: emptyList()
            }
        }

    init {
        loadRoster()
        observeRosterReactive()
    }

    fun onEvent(event: StudentRosterUiEvent) {
        when (event) {
            is StudentRosterUiEvent.LoadRoster -> loadRoster()
            is StudentRosterUiEvent.SelectClass -> {
                _selectedClassIdFlow.value = event.classId
                _uiStateFlow.update { it.copy(selectedClassId = event.classId) }
            }
            is StudentRosterUiEvent.UpdateSearch -> {
                _searchQueryFlow.value = event.query
                _uiStateFlow.update { it.copy(searchQuery = event.query) }
            }
            is StudentRosterUiEvent.RequestDelete -> {
                _uiStateFlow.update { it.copy(isDeleteDialogVisible = true, targetStudentId = event.studentId) }
            }
            is StudentRosterUiEvent.CancelDelete -> {
                _uiStateFlow.update { it.copy(isDeleteDialogVisible = false, targetStudentId = null) }
            }
            is StudentRosterUiEvent.ConfirmDelete -> deleteStudent(event.studentId)
            is StudentRosterUiEvent.RetryDelete -> {
                _uiStateFlow.value.targetStudentId?.let { deleteStudent(it) }
            }
            is StudentRosterUiEvent.ClearError -> {
                _uiStateFlow.update { it.copy(error = null) }
            }
            is StudentRosterUiEvent.SyncStudents -> syncStudents()
            is StudentRosterUiEvent.NavigateToDetail -> {
                // Handled via Screen navigation callback
            }
        }
    }

    private fun loadRoster() {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true, error = null) }
            studentRepository.getAll()
                .onSuccess { profiles ->
                    // Profiles will also be updated via observeRosterReactive flow
                    _uiStateFlow.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun observeRosterReactive() {
        combine(
            studentRepository.getStudentProfiles(),
            _allClassesFlow,
            _searchQueryFlow,
            _selectedClassIdFlow,
        ) { profilesResult, classes, query, classId ->
            val profiles = profilesResult.getOrNull() ?: emptyList()
            val classMap = classes.associateBy { it.id }

            val displayItems = profiles
                .filter { profile ->
                    val matchesQuery = profile.name.contains(query, ignoreCase = true) ||
                        (profile.studentCode?.contains(query, ignoreCase = true) ?: false)
                    val matchesClass = classId == null || profile.classIds.contains(classId)
                    matchesQuery && matchesClass
                }
                .map { profile ->
                    val assignedClassNames = profile.classIds
                        .mapNotNull { id -> classMap[id]?.name }
                        .joinToString(", ")

                    StudentDisplayItem(
                        profile = profile,
                        assignedClassNames = assignedClassNames.ifEmpty { "Tanpa Kelas" },
                        isBiometricReady = profile.biometricExists,
                    )
                }

            _uiStateFlow.update {
                it.copy(
                    students = displayItems,
                    allClasses = classes,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    }

    private fun syncStudents() {
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId() ?: return@launch
            _uiStateFlow.update { it.copy(isLoading = true) }
            studentRepository.pullStudents(schoolId)
                .onSuccess { _uiStateFlow.update { it.copy(isLoading = false) } }
                .onFailure { error -> _uiStateFlow.update { it.copy(isLoading = false, error = error.message) } }
        }
    }

    private fun deleteStudent(studentId: String) {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true, isDeleteDialogVisible = false) }
            studentRepository.deleteProfile(studentId)
                .onSuccess { _uiStateFlow.update { it.copy(isLoading = false, targetStudentId = null) } }
                .onFailure { error -> _uiStateFlow.update { it.copy(isLoading = false, error = error.message) } }
        }
    }
}
