package com.azuratech.azuratime.features.school.ui.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.model.School
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.ui.UiEvent
import com.azuratech.azuratime.features.school.data.repo.SchoolRepository
import com.azuratech.azuratime.features.account.data.repo.SchoolWorkspaceRepository
import com.azuratech.azuratime.features.account.data.repo.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SchoolViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionManager: SessionManager,
    private val schoolRepository: SchoolRepository,
    private val workspaceRepository: SchoolWorkspaceRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _uiState = MutableStateFlow(SchoolUiState())
    val uiState: StateFlow<SchoolUiState> = _uiState.asStateFlow()

    init {
        val initialAccountId = savedStateHandle.get<String>("accountId") ?: sessionManager.getCurrentUserId() ?: ""
        if (initialAccountId.isNotEmpty()) {
            onEvent(SchoolUiEvent.LoadSchools(initialAccountId))
        }

        // Keep activeSchoolId in sync with SessionManager
        viewModelScope.launch {
            sessionManager.activeSchoolIdFlow.collect { id ->
                _uiState.update { it.copy(activeSchoolId = id) }
            }
        }
    }

    fun onEvent(event: SchoolUiEvent) {
        when (event) {
            is SchoolUiEvent.LoadSchools -> loadSchools(event.accountId)
            is SchoolUiEvent.SelectSchool -> selectSchool(event.school)
            is SchoolUiEvent.CreateSchool -> createSchool(event.name, event.timezone, event.selectedClassIds)
            is SchoolUiEvent.DeleteSchool -> deleteSchool(event.id)
            SchoolUiEvent.Retry -> _uiState.value.accountId.takeIf { it.isNotEmpty() }?.let { loadSchools(it) }
        }
    }

    private fun loadSchools(accountId: String) {
        if (accountId.isBlank()) return
        _uiState.update { it.copy(isLoading = true, error = null, accountId = accountId) }

        // 🔥 REACTIVE SSOT: Observe account memberships and schools in a unified flow
        viewModelScope.launch {
            accountRepository.observeAccountEntity(accountId)
                .filterNotNull()
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
                        schoolRepository.observeSchoolsByIds(schoolIds).map { result ->
                            result.getOrNull() ?: emptyList()
                        }
                    }
                }
                .onEach { schools ->
                    if (schools.isNotEmpty() && sessionManager.getActiveSchoolId().isNullOrBlank()) {
                        selectSchool(schools.first())
                    }
                    _uiState.update { it.copy(isLoading = false, schools = schools) }
                }
                .launchIn(this)
        }

        // Separate classes observation
        viewModelScope.launch {
            schoolRepository.observeAllClassesForAccount(accountId).collect { result ->
                result.onSuccess { classes ->
                    _uiState.update { it.copy(availableClasses = classes) }
                }
            }
        }
    }

    private fun selectSchool(school: School) {
        viewModelScope.launch {
            val currentAccountId = _uiState.value.accountId
            if (currentAccountId.isEmpty()) return@launch

            sessionManager.saveActiveSchoolId(school.id)
            // Error handling handled downstream or ignored if it's just a local switch preference
            runCatching { workspaceRepository.switchWorkspace(currentAccountId, school.id) }
        }
    }

    private fun createSchool(name: String, timezone: String, selectedClassIds: List<String>) {
        val currentAccountId = _uiState.value.accountId
        if (currentAccountId.isEmpty()) return

        viewModelScope.launch {
            val account = accountRepository.getAccountById(currentAccountId)
            val role = account?.role ?: "USER"

            if (role != "SUPER_ADMIN" && _uiState.value.schools.isNotEmpty()) {
                _uiEvent.emit(UiEvent.ShowSnackbar("❌ Gagal: Hanya Super Admin yang dapat membuat lebih dari satu sekolah."))
                return@launch
            }

            schoolRepository.createSchool(currentAccountId, name, timezone)
                .onSuccess { newSchoolId ->
                    selectedClassIds.forEach { classId ->
                        schoolRepository.assignClassToSchool(newSchoolId, classId)
                    }

                    val newSchool = schoolRepository.getSchoolById(newSchoolId)
                    val status = newSchool?.status ?: "PENDING"
                    if (status == "ACTIVE") {
                        _uiEvent.emit(UiEvent.ShowSnackbar("🎉 Sekolah aktif! Anda adalah Admin."))
                        if (sessionManager.getActiveSchoolId() == null) {
                            newSchool?.let { selectSchool(it) }
                        }
                    } else {
                        _uiEvent.emit(UiEvent.ShowSnackbar("⏳ Menunggu verifikasi Super Admin."))
                    }
                }
                .onFailure { error ->
                    _uiEvent.emit(UiEvent.ShowSnackbar("❌ Gagal: ${error.message}"))
                }
        }
    }

    private fun deleteSchool(id: String) {
        val currentAccountId = _uiState.value.accountId
        if (currentAccountId.isEmpty()) return

        viewModelScope.launch {
            schoolRepository.deleteSchool(id, currentAccountId)
        }
    }
}
