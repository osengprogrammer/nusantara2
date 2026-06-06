package com.azuratech.azuratime.features.account.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.domain.model.toProfileCompat
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.account.domain.repository.AccessRequestRepository
import com.azuratech.azuratime.features.school.domain.repository.SchoolRepository
import com.azuratech.azuratime.core.domain.model.AccountRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🚀 ACCOUNT MANAGEMENT VIEW MODEL (v3.2.0-ai-native)
 * Unified ViewModel for account profile, school memberships, and network.
 */
@HiltViewModel
class AccountManagementViewModel @Inject constructor(
    private val repository: AccountRepository,
    private val accessRequestRepository: AccessRequestRepository,
    private val schoolRepository: SchoolRepository,
    private val sessionManager: SessionManager,
    private val database: AppDatabase,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(AccountUiState())
    val uiStateFlow: StateFlow<AccountUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<AccountUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    init {
        observeData()
        onEvent(AccountUiEvent.LoadProfile)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeData() {
        // 1. Current Account Profile & Active Class (Mapped from Domain Account)
        sessionManager.currentAccountIdFlow
            .filterNotNull()
            .flatMapLatest { accountId -> repository.getAccountFlow(accountId) }
            .onEach { result ->
                result.onSuccess { account ->
                    _uiStateFlow.update {
                        it.copy(
                            accountProfile = account.toProfileCompat(),
                            activeClassId = account.activeClassId,
                        )
                    }
                }
            }
            .launchIn(viewModelScope)

        // 2. Assigned Class IDs for current account
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .combine(sessionManager.currentAccountIdFlow.filterNotNull()) { schoolId, accountId -> schoolId to accountId }
            .flatMapLatest { (schoolId, accountId) -> database.accountClassAccessDao().getAssignedClassIds(accountId, schoolId) }
            .onEach { classIds ->
                _uiStateFlow.update { it.copy(assignedClassIds = classIds) }
            }
            .launchIn(viewModelScope)

        // 3. Available Classes in current school
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId -> schoolRepository.observeClassesFlow(schoolId) }
            .onEach { result ->
                result.onSuccess { classes ->
                    _uiStateFlow.update { it.copy(availableClasses = classes) }
                }
            }
            .launchIn(viewModelScope)

        // 4. All accounts in same school (Network)
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                database.accountDao().observeAllAccountsFlow().map { accounts ->
                    accounts.filter { it.memberships.containsKey(schoolId) }
                }
            }
            .onEach { accounts ->
                _uiStateFlow.update { it.copy(allAccountsInSameSchool = accounts) }
            }
            .launchIn(viewModelScope)

        // 5. Target Account Assigned Class IDs
        _uiStateFlow.map { it.selectedTargetAccount }
            .distinctUntilChanged()
            .combine(sessionManager.activeSchoolIdFlow.filterNotNull()) { target, schoolId ->
                if (target != null) target.accountId to schoolId else null
            }
            .filterNotNull()
            .flatMapLatest { (targetId, schoolId) -> database.accountClassAccessDao().getAssignedClassIds(targetId, schoolId) }
            .onEach { classIds ->
                _uiStateFlow.update { it.copy(targetAssignedClassIds = classIds) }
            }
            .launchIn(viewModelScope)

        // 6. Pending Followers for active school
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .flatMapLatest { schoolId ->
                database.accessRequestDao().observePendingRequestsBySchoolFlow(schoolId)
            }
            .onEach { requests ->
                _uiStateFlow.update { it.copy(pendingFollowers = requests) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: AccountUiEvent) {
        when (event) {
            is AccountUiEvent.LoadProfile -> loadProfile()
            is AccountUiEvent.UpdateDisplayName -> updateDisplayName(event.newName)
            is AccountUiEvent.SelectActiveClass -> selectActiveClass(event.classId, event.targetAccountId)
            is AccountUiEvent.UpdatePhoto -> updatePhoto(event.uri)
            is AccountUiEvent.AssignClassToAccount -> assignClassToAccount(event.classId, event.targetAccountId)
            is AccountUiEvent.RemoveClassAccess -> removeClassAccess(event.classId, event.targetAccountId)
            is AccountUiEvent.ClearPhoto -> clearPhoto()
            is AccountUiEvent.Logout -> logout()
            is AccountUiEvent.ClearError -> _uiStateFlow.update { it.copy(error = null) }
            is AccountUiEvent.NavigateBack -> { /* Handled in Screen */ }
            is AccountUiEvent.UpdatePendingRole -> updatePendingRole(event.requestId, event.role)
            is AccountUiEvent.ApproveFollower -> approveFollower(event.requestId)
        }
    }

    private fun updatePendingRole(requestId: String, role: AccountRole) {
        _uiStateFlow.update {
            it.copy(selectedRoles = it.selectedRoles + (requestId to role))
        }
    }

    private fun approveFollower(requestId: String) {
        val role = _uiStateFlow.value.selectedRoles[requestId] ?: AccountRole.USER
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            accessRequestRepository.approveRequest(requestId, role)
                .onSuccess {
                    _uiStateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(AccountUiEffect.ShowSnackbar("Follower approved as ${role.name}"))
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun loadProfile() {
        val uid = sessionManager.getCurrentAccountId() ?: return
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            repository.getProfile(uid)
                .onSuccess { profile ->
                    _uiStateFlow.update { it.copy(isLoading = false, accountProfile = profile) }
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun updateDisplayName(newName: String) {
        val accountId = sessionManager.getCurrentAccountId() ?: return
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            repository.updateDisplayName(accountId, newName)
                .onSuccess {
                    _uiStateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(AccountUiEffect.ShowSnackbar("Display name updated successfully"))
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun selectActiveClass(classId: String?, targetAccountId: String?) {
        val accountId = targetAccountId ?: sessionManager.getCurrentAccountId() ?: return
        viewModelScope.launch {
            repository.getAccountById(accountId).onSuccess { account ->
                val updated = account.copy(activeClassId = classId)
                database.accountDao().updateAccount(updated)
                repository.pushAccount(accountId)
            }
        }
    }

    private fun assignClassToAccount(classId: String, targetAccountId: String?) {
        val accountId = targetAccountId ?: sessionManager.getCurrentAccountId() ?: return
        viewModelScope.launch {
            database.accountClassAccessDao().insert(
                com.azuratech.azuratime.features.account.data.local.AccountClassAccessEntity(
                    accountId = accountId,
                    classId = classId,
                    schoolId = sessionManager.getActiveSchoolId() ?: "",
                ),
            )
        }
    }

    private fun removeClassAccess(classId: String, targetAccountId: String?) {
        val accountId = targetAccountId ?: sessionManager.getCurrentAccountId() ?: return
        viewModelScope.launch {
            database.accountClassAccessDao().deleteSpecificAccess(accountId, classId)
        }
    }

    private fun updatePhoto(uri: android.net.Uri) {
        val accountId = sessionManager.getCurrentAccountId() ?: return
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            repository.updatePhoto(accountId, uri.toString())
                .onSuccess {
                    _uiStateFlow.update { it.copy(isLoading = false, pendingPhotoUri = null) }
                    _uiEffectFlow.emit(AccountUiEffect.ShowSnackbar("Profile photo updated successfully"))
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun clearPhoto() {
        _uiStateFlow.update { it.copy(pendingPhotoUri = null) }
    }

    private fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
            _uiEffectFlow.emit(AccountUiEffect.NavigateTo("login"))
        }
    }

    // --- LEGACY COMPATIBILITY ---
    val currentAccountFlow = _uiStateFlow.map { it.accountProfile }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val assignedClassIdsFlow = _uiStateFlow.map { it.assignedClassIds }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allAccountsInSameSchoolFlow = _uiStateFlow.map { it.allAccountsInSameSchool }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val selectedTargetAccountFlow = _uiStateFlow.map { it.selectedTargetAccount }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val targetAssignedClassIdsFlow = _uiStateFlow.map { it.targetAssignedClassIds }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setTargetAccount(accountId: String, name: String, email: String) {
        _uiStateFlow.update {
            it.copy(
                selectedTargetAccount = AccountEntity(
                    accountId = accountId,
                    name = name,
                    email = email,
                ),
            )
        }
    }

    fun refreshCurrentAccountFromCloud() {
        onEvent(AccountUiEvent.LoadProfile)
    }
}
