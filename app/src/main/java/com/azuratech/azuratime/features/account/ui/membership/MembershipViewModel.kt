package com.azuratech.azuratime.features.account.ui.membership
import com.azuratech.azuratime.core.data.local.AccountEntity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.data.local.toProfile
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.features.account.domain.repository.AccessRequestRepository
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.account.domain.repository.MembershipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.azuratech.azuratime.core.data.local.toDomain




/**
 * 🛠️ MEMBERSHIP VIEW MODEL (v3.2.1-ai-native)
 * Observes AccountEntity and AccessRequestProfile from Room. Strict MVI.
 */
@HiltViewModel
class MembershipViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val accessRequestRepository: AccessRequestRepository,
    private val membershipRepository: MembershipRepository,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val _uiStateFlow = MutableStateFlow(MembershipUiState())
    val uiStateFlow: StateFlow<MembershipUiState> = _uiStateFlow.asStateFlow()

    init {
        observeData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeData() {
        val accountFlow = sessionManager.currentAccountIdFlow
            .filterNotNull()
            .flatMapLatest { uid ->
                accountRepository.observeAccountEntityFlow(uid).map { it.getOrNull() }
            }

        val requestsFlow = sessionManager.currentAccountIdFlow
            .filterNotNull()
            .flatMapLatest { uid ->
                accessRequestRepository.observeRequestsByAccountFlow(uid).map { it.getOrNull() ?: emptyList() }
            }

        combine(accountFlow, requestsFlow) { account, requests ->
            val status = when {
                account == null -> MembershipStatus.Loading
                account.status == "PENDING" -> MembershipStatus.Pending
                account.status == SessionManager.STATUS_ACTIVE -> MembershipStatus.Approved
                account.status == "REJECTED" -> MembershipStatus.Rejected("Your account was rejected by the administrator.")
                requests.isNotEmpty() -> MembershipStatus.Pending
                else -> MembershipStatus.Idle
            }

            _uiStateFlow.update { state ->
                state.copy(
                    account = account,
                    accessRequests = requests.map { it.toProfile() },
                    memberships = account?.memberships?.values?.map { it.toDomain() } ?: emptyList(),
                    status = status,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: MembershipUiEvent) {
        when (event) {
            is MembershipUiEvent.CheckMembership -> checkMembership(event.email, event.displayName)
            MembershipUiEvent.ActivateMembership -> activateMembership()
            MembershipUiEvent.ClearError -> _uiStateFlow.update { it.copy(error = null) }
        }
    }

    private fun checkMembership(email: String, displayName: String?) {
        val uid = sessionManager.getCurrentAccountId() ?: return

        viewModelScope.launch {
            val syncResult = accountRepository.syncAccount(uid)
            syncManager.enqueueAccessSync(uid)

            if (syncResult is com.azuratech.azuraengine.result.Result.Success) {
                if (syncResult.data.status == "PENDING") {
                    startPollingStatus(uid)
                }
            } else {
                accountRepository.getAccountById(uid).onSuccess { localAccount ->
                    if (localAccount.status == "PENDING") {
                        membershipRepository.createPendingAccount(uid, email, displayName)
                        startPollingStatus(uid)
                    }
                }.onFailure {
                    membershipRepository.createPendingAccount(uid, email, displayName)
                    startPollingStatus(uid)
                }
            }
        }
    }

    private fun startPollingStatus(uid: String) {
        viewModelScope.launch {
            var retries = 0
            while (retries < 12) {
                delay(10000)
                val result = accountRepository.syncAccount(uid)
                if (result is com.azuratech.azuraengine.result.Result.Success && result.data.status == SessionManager.STATUS_ACTIVE) {
                    break
                }
                retries++
            }
        }
    }

    private fun activateMembership() {
        viewModelScope.launch {
            val uid = sessionManager.getCurrentAccountId() ?: return@launch
            accountRepository.getAccountById(uid).onSuccess { entity ->
                val data = mapOf(
                    "accountId" to entity.accountId,
                    "status" to entity.status,
                    "activeSchoolId" to (entity.activeSchoolId ?: ""),
                    "role" to entity.role,
                    "memberships" to entity.memberships,
                )
                membershipRepository.activateSession(data)
            }
        }
    }
}
