package com.azuratech.azuratime.features.student.ui.roster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import com.azuratech.azuratime.core.ui.components.StudentRosterItem
import com.azuratech.azuratime.core.data.local.StudentWalletDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.collect

/**
 * 🎓 STUDENT ROSTER VIEW MODEL (v3.2.0-ai-native)
 * Optimized with Effect-Driven MVI pattern.
 */
@HiltViewModel
class StudentRosterViewModel @Inject constructor(
    private val studentRepository: StudentRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionManager: SessionManager,
    private val walletDao: StudentWalletDao,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(StudentRosterUiState())
    val uiStateFlow: StateFlow<StudentRosterUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<StudentRosterUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    // Internal flows for reactive filtering
    private val _searchQueryFlow = MutableStateFlow("")
    private val _selectedClassIdFlow = MutableStateFlow<String?>(null)

    private val _allClassesFlow = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            schoolRepository.observeClassesFlow(schoolId).map { result ->
                result.getOrNull() ?: emptyList()
            }
        }

    // Flow of wallets for the active school
    private val _walletsFlow = sessionManager.activeSchoolIdFlow
        .filterNotNull()
        .flatMapLatest { schoolId ->
            walletDao.getAllWalletsBySchool(schoolId)
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
            is StudentRosterUiEvent.ClearError -> { /* Handled via Effects in UI */ }
            is StudentRosterUiEvent.SyncStudents -> syncStudents()
            is StudentRosterUiEvent.NavigateToDetail -> {
                viewModelScope.launch { _uiEffectFlow.emit(StudentRosterUiEffect.NavigateToDetail(event.studentId)) }
            }
        }
    }

    private fun loadRoster() {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            studentRepository.getAll()
                .onSuccess {
                    // Profiles will also be updated via observeRosterReactive flow
                    _uiStateFlow.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(StudentRosterUiEffect.ShowToast("Failed to load data: ${error.message}"))
                }
        }
    }

    private fun observeRosterReactive() {
        // Combine student profiles, class list, search/filter state, and wallet balances.
        viewModelScope.launch {
            combine(
                studentRepository.getStudentProfilesFlow(),
                _allClassesFlow,
                _searchQueryFlow,
                _selectedClassIdFlow,
                _walletsFlow,
            ) { profilesResult, classes, query, classId, wallets ->
                val profiles = profilesResult.getOrNull() ?: emptyList()
                val classMap = classes.associateBy { it.id }

                // Build UI items that include wallet balance
                val rosterItems = profiles
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
                            .ifEmpty { "No Class" }

                        // Find wallet for this student (default 0)
                        val wallet = wallets.find { it.studentId == profile.studentId }
                        val currentBalance = wallet?.currentBalance ?: 0.0

                        StudentRosterItem(
                            studentId = profile.studentId,
                            displayName = profile.name,
                            studentCode = profile.studentCode,
                            assignedClassNames = assignedClassNames,
                            isBiometricReady = profile.embedding != null,
                            currentBalance = currentBalance,
                        )
                    }
                rosterItems to classes
            }.collect { (rosterItems, classes) ->
                _uiStateFlow.update { it.copy(students = rosterItems, allClasses = classes) }
            }
        }
    }

    private fun syncStudents() {
        viewModelScope.launch {
            // TODO: Implement actual sync logic
        }
    }

    private fun deleteStudent(@Suppress("UNUSED_PARAMETER") studentId: String) {
        viewModelScope.launch {
            // TODO: Implement actual delete logic
        }
    }
}
