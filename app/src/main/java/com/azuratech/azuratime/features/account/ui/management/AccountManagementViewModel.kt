package com.azuratech.azuratime.features.account.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
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
        // 0. Synchronize Active School ID (SSOT)
        sessionManager.activeSchoolIdFlow
            .onEach { schoolId ->
                _uiStateFlow.update { it.copy(activeSchoolId = schoolId) }
            }
            .launchIn(viewModelScope)

        // 1. Current Account Profile & Active Class (Mapped from Domain Account)
        sessionManager.currentAccountIdFlow
            .filterNotNull()
            .combine(sessionManager.activeSchoolIdFlow) { accountId, schoolId -> accountId to schoolId }
            .flatMapLatest { (accountId, schoolId) ->
                repository.getAccountFlow(accountId).map { it to schoolId }
            }
            .onEach { (result, schoolId) ->
                result.onSuccess { account ->
                    val roleStr = if (schoolId != null) account.memberships[schoolId]?.role else null
                    val role = com.azuratech.azuratime.core.domain.model.AccountRole.fromString(roleStr ?: account.role.name)

                    _uiStateFlow.update {
                        it.copy(
                            accountProfile = account.toProfileCompat(),
                            activeClassId = account.activeClassId,
                            currentAccountRole = role,
                            activeSchoolId = schoolId,
                        )
                    }

                    Log.d("AZURA_DEBUG_ROLE", "Role: ${role.name}, School: $schoolId, NetworkSize: ${_uiStateFlow.value.allAccountsInSameSchool.size}")
                }
            }
            .launchIn(viewModelScope)

        // 2. Assigned Class IDs for current account
        sessionManager.activeSchoolIdFlow
            .filterNotNull()
            .combine(sessionManager.currentAccountIdFlow.filterNotNull()) { schoolId, accountId -> schoolId to accountId }
            .flatMapLatest { (schoolId, accountId) -> database.accountClassAccessDao().getAssignmentsFlow(accountId, schoolId) }
            .onEach { assignments ->
                val classIds = assignments.map { it.classId }.distinct()
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
            .flatMapLatest { (targetId, schoolId) -> database.accountClassAccessDao().getAssignmentsFlow(targetId, schoolId) }
            .onEach { assignments ->
                val classIds = assignments.map { it.classId }.distinct()
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
            is AccountUiEvent.Logout -> handleLogout()
            is AccountUiEvent.ClearError -> _uiStateFlow.update { it.copy(error = null) }
            is AccountUiEvent.NavigateBack -> { /* Handled in Screen */ }
            is AccountUiEvent.UpdatePendingRole -> updatePendingRole(event.requestId, event.role)
            is AccountUiEvent.ApproveFollower -> approveFollower(event.requestId)
            is AccountUiEvent.ChangeMemberRole -> changeMemberRole(event.targetAccountId, event.newRole)
            is AccountUiEvent.RemoveMember -> removeMember(event.targetAccountId)
        }
    }

    private fun removeMember(targetAccountId: String) {
        val currentAccountId = sessionManager.getCurrentAccountId() ?: return
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            repository.unfollowAccount(currentAccountId, targetAccountId)
                .onSuccess {
                    _uiStateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(AccountUiEffect.ShowSnackbar("Member removed from school."))
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun updatePendingRole(requestId: String, role: AccountRole) {
        _uiStateFlow.update {
            it.copy(selectedRoles = it.selectedRoles + (requestId to role))
        }
    }

    private fun changeMemberRole(targetAccountId: String, newRole: AccountRole) {
        val schoolId = sessionManager.getActiveSchoolId() ?: return
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true) }
            repository.updateMemberRole(targetAccountId, schoolId, newRole)
                .onSuccess {
                    _uiStateFlow.update { it.copy(isLoading = false) }
                    _uiEffectFlow.emit(AccountUiEffect.ShowSnackbar("Member role updated to ${newRole.name}"))
                }
                .onFailure { error ->
                    _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
                }
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
            repository.selectActiveClass(accountId, classId)
        }
    }

    private fun assignClassToAccount(classId: String, targetAccountId: String?) {
        val accountId = targetAccountId ?: sessionManager.getCurrentAccountId() ?: return
        viewModelScope.launch {
            val schoolId = sessionManager.getActiveSchoolId() ?: ""
            repository.assignClassToAccount(accountId, classId, schoolId)
        }
    }

    private fun removeClassAccess(classId: String, targetAccountId: String?) {
        val accountId = targetAccountId ?: sessionManager.getCurrentAccountId() ?: return
        viewModelScope.launch {
            repository.removeClassAccess(accountId, classId)
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

    private fun handleLogout() {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoggingOut = true) }
            sessionManager.clearSession()
            // 🔥 AI Native: BootViewModel will handle root UI transition.
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
