package com.azuratech.azuratime.features.school.ui.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.features.account.domain.repository.SchoolWorkspaceRepository
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.core.domain.model.AccountRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🚀 SCHOOL VIEW MODEL (v3.2.0-ai-native)
 * Manages school list and workspace switching.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SchoolViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionManager: SessionManager,
    private val schoolRepository: SchoolRepository,
    private val workspaceRepository: SchoolWorkspaceRepository,
    private val accountRepository: AccountRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val _uiEffectFlow = MutableSharedFlow<SchoolUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    private val _uiStateFlow = MutableStateFlow(SchoolUiState())
    val uiStateFlow: StateFlow<SchoolUiState> = _uiStateFlow.asStateFlow()

    init {
        val initialAccountId = savedStateHandle.get<String>("accountId") ?: sessionManager.getCurrentAccountId() ?: ""
        if (initialAccountId.isNotEmpty()) {
            onEvent(SchoolUiEvent.LoadSchools(initialAccountId))
        }

        loadOrphanedClasses() // 🔥 AI Native: Pre-load classes for new school creation

        // Keep activeSchoolId in sync with SessionManager
        viewModelScope.launch {
            sessionManager.activeSchoolIdFlow.collect { id ->
                _uiStateFlow.update { it.copy(activeSchoolId = id) }
            }
        }
    }

    private fun loadOrphanedClasses() {
        viewModelScope.launch {
            schoolRepository.getOrphanedClasses().onSuccess { classes ->
                _uiStateFlow.update { it.copy(availableClasses = classes) }
            }
        }
    }

    fun onEvent(event: SchoolUiEvent) {
        when (event) {
            is SchoolUiEvent.LoadSchools -> loadSchools(event.accountId)
            is SchoolUiEvent.SelectSchool -> selectSchool(event.school)
            is SchoolUiEvent.CreateSchool -> createSchool(event.name, event.timezone, event.selectedClassIds)
            is SchoolUiEvent.DeleteSchool -> deleteSchool(event.id)
            is SchoolUiEvent.UpdateSchoolName -> updateSchoolName(event.schoolId, event.newName)
            SchoolUiEvent.Retry -> _uiStateFlow.value.accountId.takeIf { it.isNotEmpty() }?.let { loadSchools(it) }
        }
    }

    private fun loadSchools(accountId: String) {
        if (accountId.isBlank()) return
        _uiStateFlow.update { it.copy(isLoading = true, error = null, accountId = accountId) }

        // 🔥 REACTIVE SSOT: Observe account memberships and schools in a unified flow
        viewModelScope.launch {
            accountRepository.observeAccountEntityFlow(accountId)
                .map { it.getOrNull() }
                .filterNotNull()
                .onEach { account ->
                    _uiStateFlow.update { it.copy(currentAccountRole = AccountRole.fromString(account.role)) }
                }
                .distinctUntilChangedBy { it.memberships.keys }
                .flatMapLatest { account ->
                    val schoolIds = account.memberships.keys.toList()
                    if (schoolIds.isEmpty()) {
                        flowOf(emptyList<School>())
                    } else {
                        // Trigger background sync for missing schools
                        viewModelScope.launch {
                            schoolRepository.syncSchools(schoolIds)
                        }
                        // Observe the schools from Room
                        schoolRepository.observeSchoolsByIdsFlow(schoolIds).map { result ->
                            result.getOrNull() ?: emptyList()
                        }
                    }
                }
                .onEach { schools ->
                    if (schools.isNotEmpty() && sessionManager.getActiveSchoolId().isNullOrBlank()) {
                        selectSchool(schools.first())
                    }
                    _uiStateFlow.update { it.copy(isLoading = false, schools = schools) }
                }
                .launchIn(this)
        }

        // Separate classes observation
        viewModelScope.launch {
            schoolRepository.observeAllClassesForAccountFlow(accountId).collect { result ->
                result.onSuccess { classes ->
                    _uiStateFlow.update { it.copy(availableClasses = classes) }
                }
            }
        }
    }

    private fun selectSchool(school: School) {
        viewModelScope.launch {
            val currentAccountId = _uiStateFlow.value.accountId
            if (currentAccountId.isEmpty()) return@launch

            sessionManager.saveActiveSchoolId(school.id)
            syncManager.enqueueSync() // 🔥 Trigger immediate sync for history & biometrics
            // Error handling handled downstream or ignored if it's just a local switch preference
            runCatching { workspaceRepository.switchWorkspace(currentAccountId, school.id) }
        }
    }

    private fun createSchool(name: String, timezone: String, selectedClassIds: List<String>) {
        val currentAccountId = _uiStateFlow.value.accountId
        if (currentAccountId.isEmpty()) return

        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            schoolRepository.createSchool(currentAccountId, name, timezone)
                .onSuccess { newSchoolId ->
                    // Push the account update immediately (containing the new membership)
                    accountRepository.pushAccount(currentAccountId)

                    selectedClassIds.forEach { classId ->
                        schoolRepository.assignClassToSchool(newSchoolId, classId)
                    }

                    schoolRepository.getSchoolById(newSchoolId).onSuccess { newSchool ->
                        val status = newSchool.status
                        if (status == "ACTIVE") {
                            _uiEffectFlow.emit(SchoolUiEffect.ShowSnackbar("🎉 School active! You are an Admin."))
                            selectSchool(newSchool)
                        } else {
                            _uiEffectFlow.emit(SchoolUiEffect.ShowSnackbar("⏳ Waiting for Super Admin verification."))
                        }
                    }
                    loadOrphanedClasses() // 🔥 Refresh available classes
                    _uiStateFlow.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    _uiEffectFlow.emit(SchoolUiEffect.ShowSnackbar("❌ Failed: ${error.message}"))
                    _uiStateFlow.update { it.copy(isLoading = false) }
                }
        }
    }

    private fun deleteSchool(id: String) {
        val currentAccountId = _uiStateFlow.value.accountId
        if (currentAccountId.isEmpty()) return

        viewModelScope.launch {
            schoolRepository.deleteSchool(id, currentAccountId)
        }
    }

    private fun updateSchoolName(schoolId: String, newName: String) {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            schoolRepository.updateSchoolDetails(schoolId, newName, null)
                .onSuccess {
                    _uiStateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(SchoolUiEffect.ShowSnackbar("School name updated successfully!"))
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}
