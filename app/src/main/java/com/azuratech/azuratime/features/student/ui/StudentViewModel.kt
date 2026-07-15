package com.azuratech.azuratime.features.student.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.result.onFailure
import com.azuratech.azuratime.core.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.ui.components.StudentDisplayItem
import com.azuratech.azuratime.features.student.domain.usecase.DeleteStudentProfileUseCase
import com.azuratech.azuratime.features.student.domain.usecase.GetAllStudentsUseCase
import com.azuratech.azuratime.features.student.domain.usecase.ObserveClassesForSchoolUseCase
import com.azuratech.azuratime.features.student.domain.usecase.ObserveStudentProfilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
 * 🎓 STUDENT VIEW MODEL (v3.2.0-ai-native)
 * Unified ViewModel for Student Management.
 */
@HiltViewModel
class StudentViewModel @Inject constructor(
    private val observeClassesForSchoolUseCase: ObserveClassesForSchoolUseCase,
    private val getAllStudentsUseCase: GetAllStudentsUseCase,
    private val observeStudentProfilesUseCase: ObserveStudentProfilesUseCase,
    private val deleteStudentProfileUseCase: DeleteStudentProfileUseCase,
    private val sessionManager: SessionManager,
    private val syncUseCase: com.azuratech.azuratime.features.student.domain.usecase.SyncPendingStudentDataUseCase,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(StudentUiState())
    val uiStateFlow: StateFlow<StudentUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<StudentUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    // Internal flows for reactive filtering
    private val _searchQueryFlow = MutableStateFlow("")
    private val _selectedClassIdFlow = MutableStateFlow<String?>(null)

    private val _allClassesFlow = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            observeClassesForSchoolUseCase(schoolId).map { result ->
                result.getOrNull() ?: emptyList()
            }
        }

    init {
        loadStudents()
        observeStudentsReactive()
    }

    fun onEvent(event: StudentUiEvent) {
        when (event) {
            is StudentUiEvent.LoadStudents -> loadStudents()
            is StudentUiEvent.SelectClass -> {
                _selectedClassIdFlow.value = event.classId
                _uiStateFlow.update { it.copy(selectedClassId = event.classId) }
            }
            is StudentUiEvent.UpdateSearch -> {
                _searchQueryFlow.value = event.query
                _uiStateFlow.update { it.copy(searchQuery = event.query) }
            }
            is StudentUiEvent.OpenAddDialog -> {
                _uiStateFlow.update { it.copy(isAddDialogVisible = true) }
            }
            is StudentUiEvent.OpenEditDialog -> {
                _uiStateFlow.update { it.copy(isEditDialogVisible = true, editingStudent = event.student) }
            }
            is StudentUiEvent.DeleteStudent -> deleteStudent(event.studentId)
            is StudentUiEvent.SyncStudents -> syncStudents()
            is StudentUiEvent.DismissDialog -> {
                _uiStateFlow.update { it.copy(isAddDialogVisible = false, isEditDialogVisible = false, editingStudent = null) }
            }
        }
    }

    private fun loadStudents() {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            getAllStudentsUseCase()
                .onSuccess {
                    _uiStateFlow.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(StudentUiEffect.ShowToast("Failed: ${error.message}"))
                }
        }
    }

    private fun observeStudentsReactive() {
        combine(
            observeStudentProfilesUseCase(),
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
                        assignedClassNames = assignedClassNames.ifEmpty { "No Class" },
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
            syncUseCase.pullAll(schoolId)
                .onSuccess { _uiStateFlow.update { it.copy(isLoading = false) } }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(StudentUiEffect.ShowToast("Sync failed: ${error.message}"))
                }
        }
    }

    private fun deleteStudent(studentId: String) {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            deleteStudentProfileUseCase(studentId)
                .onSuccess {
                    _uiStateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(StudentUiEffect.ShowToast("Student deleted"))
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(StudentUiEffect.ShowToast("Delete failed: ${error.message}"))
                }
        }
    }
}
