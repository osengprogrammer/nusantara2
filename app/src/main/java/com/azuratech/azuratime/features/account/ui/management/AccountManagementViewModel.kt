package com.azuratech.azuratime.features.account.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.ui.UiEvent
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.data.local.toProfile
import com.azuratech.azuratime.features.account.data.repo.AccountRepository
import com.azuratech.azuratime.features.school.data.repo.SchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 👤 ACCOUNT MANAGEMENT VIEW MODEL (v3.2.0-ai-native)
 * Unified ViewModel for account profile, school memberships, and network.
 */
@HiltViewModel
class AccountManagementViewModel @Inject constructor(
    private val repository: AccountRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionManager: SessionManager,
    private val database: AppDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        observeData()
        onEvent(AccountUiEvent.LoadProfile)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeData() {
        // 1. Current User Profile & Active Class
        sessionManager.currentUserIdFlow
            .filterNotNull()
            .flatMapLatest { uid -> repository.observeAccountEntity(uid) }
            .onEach { entity ->
                _uiState.update { it.copy(userProfile = entity?.toProfile(), activeClassId = entity?.activeClassId) }
            }
            .launchIn(viewModelScope)

        // 2. Assigned Class IDs for current user
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .combine(sessionManager.currentUserIdFlow.filterNotNull()) { schoolId, accountId -> schoolId to accountId }
            .flatMapLatest { (schoolId, accountId) -> database.accountClassAccessDao().getAssignedClassIds(accountId, schoolId) }
            .onEach { classIds ->
                _uiState.update { it.copy(assignedClassIds = classIds) }
            }
            .launchIn(viewModelScope)

        // 3. Available Classes in current school
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId -> schoolRepository.observeClasses(schoolId) }
            .onEach { result ->
                result.onSuccess { classes ->
                    _uiState.update { it.copy(availableClasses = classes) }
                }
            }
            .launchIn(viewModelScope)

        // 4. All users in same school (Network)
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                repository.getAccountDao().observeAllAccounts().map { accounts ->
                    accounts.filter { it.memberships.containsKey(schoolId) }
                }
            }
            .onEach { accounts ->
                _uiState.update { it.copy(allUsersInSameSchool = accounts) }
            }
            .launchIn(viewModelScope)

        // 5. Target User Assigned Class IDs
        _uiState.map { it.selectedTargetUser }
            .distinctUntilChanged()
            .combine(sessionManager.activeSchoolIdFlow.filterNotNull()) { target, schoolId ->
                if (target != null) target.accountId to schoolId else null
            }
            .filterNotNull()
            .flatMapLatest { (targetId, schoolId) -> database.accountClassAccessDao().getAssignedClassIds(targetId, schoolId) }
            .onEach { classIds ->
                _uiState.update { it.copy(targetAssignedClassIds = classIds) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: AccountUiEvent) {
        when (event) {
            is AccountUiEvent.LoadProfile -> loadProfile()
            is AccountUiEvent.UpdateDisplayName -> updateDisplayName(event.newName)
            is AccountUiEvent.SelectActiveClass -> selectActiveClass(event.classId, event.targetAccountId)
            is AccountUiEvent.UpdatePhoto -> updatePhoto(event.uri)
            is AccountUiEvent.AssignClassToUser -> assignClassToUser(event.classId, event.targetAccountId)
            is AccountUiEvent.RemoveClassAccess -> removeClassAccess(event.classId, event.targetAccountId)
            is AccountUiEvent.ClearPhoto -> clearPhoto()
            is AccountUiEvent.Logout -> logout()
            is AccountUiEvent.ClearError -> _uiState.update { it.copy(error = null) }
            is AccountUiEvent.NavigateBack -> { /* Handled in Screen */ }
        }
    }

    private fun loadProfile() {
        val uid = sessionManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getProfile(uid)
                .onSuccess { profile ->
                    _uiState.update { it.copy(isLoading = false, userProfile = profile) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun updateDisplayName(newName: String) {
        val uid = sessionManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.updateDisplayName(uid, newName)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _uiEvent.emit(UiEvent.ShowSnackbar("Nama berhasil diperbarui"))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun selectActiveClass(classId: String?, targetAccountId: String?) {
        val uid = targetAccountId ?: sessionManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            val account = repository.getAccountById(uid) ?: return@launch
            val updated = account.copy(activeClassId = classId)
            repository.getAccountDao().updateAccount(updated)
            repository.pushAccount(uid)
        }
    }

    private fun assignClassToUser(classId: String, targetAccountId: String?) {
        val uid = targetAccountId ?: sessionManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            database.accountClassAccessDao().insert(
                com.azuratech.azuratime.core.data.local.AccountClassAccessEntity(
                    accountId = uid,
                    classId = classId,
                    schoolId = sessionManager.getActiveSchoolId() ?: "",
                ),
            )
        }
    }

    private fun removeClassAccess(classId: String, targetAccountId: String?) {
        val uid = targetAccountId ?: sessionManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            database.accountClassAccessDao().deleteSpecificAccess(uid, classId)
        }
    }

    private fun updatePhoto(uri: android.net.Uri) {
        val uid = sessionManager.getCurrentUserId() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.updatePhoto(uid, uri.toString())
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, pendingPhotoUri = null) }
                    _uiEvent.emit(UiEvent.ShowSnackbar("Foto profil berhasil diperbarui"))
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun clearPhoto() {
        _uiState.update { it.copy(pendingPhotoUri = null) }
    }

    private fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
            _uiEvent.emit(UiEvent.NavigateTo("login"))
        }
    }

    // --- LEGACY COMPATIBILITY ---
    val currentUser = _uiState.map { it.userProfile }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val assignedClassIds = _uiState.map { it.assignedClassIds }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allUsersInSameSchool = _uiState.map { it.allUsersInSameSchool }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val selectedTargetUser = _uiState.map { it.selectedTargetUser }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val targetAssignedClassIds = _uiState.map { it.targetAssignedClassIds }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setTargetUser(accountId: String, name: String, email: String) {
        _uiState.update {
            it.copy(
                selectedTargetUser = AccountEntity(
                    accountId = accountId,
                    name = name,
                    email = email,
                ),
            )
        }
    }

    fun refreshCurrentUserFromCloud() {
        onEvent(AccountUiEvent.LoadProfile)
    }
}
