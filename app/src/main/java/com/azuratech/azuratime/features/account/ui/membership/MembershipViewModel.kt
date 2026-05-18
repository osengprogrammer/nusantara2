package com.azuratech.azuratime.features.account.ui.membership

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.onFailure
import com.azuratech.azuraengine.result.onSuccess
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.data.local.Membership
import com.azuratech.azuratime.features.account.domain.repository.AccessRequestRepository
import com.azuratech.azuratime.features.account.domain.repository.MembershipRepository
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.core.data.local.toProfile
import com.azuratech.azuratime.features.account.domain.model.AccessRequestProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🛠️ MEMBERSHIP VIEW MODEL
 * 🔥 v3.1: Reactive SSOT Migration (Phase 7.6)
 * Observes AccountEntity and AccessRequestProfile from Room.
 */
@HiltViewModel
class MembershipViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val accessRequestRepository: AccessRequestRepository,
    private val membershipRepository: MembershipRepository,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager,
    private val database: AppDatabase,
) : ViewModel() {

    // =====================================================
    // 📊 REACTIVE STREAMS (SSOT)
    // =====================================================

    @OptIn(ExperimentalCoroutinesApi::class)
    val accountFlow: StateFlow<AccountEntity?> = sessionManager.currentAccountIdFlow
        .filterNotNull()
        .flatMapLatest { uid ->
            accountRepository.observeAccountEntity(uid).map { result ->
                result.getOrNull()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val accessRequestsFlow: StateFlow<List<AccessRequestProfile>> = sessionManager.currentAccountIdFlow
        .filterNotNull()
        .flatMapLatest { uid ->
            accessRequestRepository.observeRequestsByAccount(uid)
                .map { result ->
                    result.getOrNull()?.map { it.toProfile() } ?: emptyList()
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 🔥 DERIVED UI STATE
     * Combines accountFlow status and pending requests for a single SSOT stateFlow.
     */
    val stateFlow: StateFlow<MembershipState> = combine(accountFlow, accessRequestsFlow) { account, requests ->
        when {
            account == null -> MembershipState.Loading
            account.status == "PENDING" -> MembershipState.Pending
            account.status == SessionManager.STATUS_ACTIVE -> MembershipState.Approved
            account.status == "REJECTED" -> MembershipState.Rejected("Akun Anda ditolak oleh administrator.")
            requests.isNotEmpty() -> MembershipState.Pending
            else -> MembershipState.Idle
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MembershipState.Loading)

    val membershipsFlow: StateFlow<List<Membership>> = accountFlow.map {
        it?.memberships?.values?.toList() ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // =====================================================
    // 🛠️ ACTION HANDLERS
    // =====================================================

    fun checkMembership(email: String, displayName: String? = null) {
        val uid = sessionManager.getCurrentAccountId() ?: return

        viewModelScope.launch {
            // 🔥 CRITICAL: Pull the latest status from Firestore first to see if Admin approved
            val syncResult = accountRepository.syncAccount(uid)

            // Trigger background sync to handle any local access requests
            syncManager.enqueueAccessSync(uid)

            if (syncResult is com.azuratech.azuraengine.result.Result.Success) {
                // If cloud pull was successful, and we are still pending, start polling
                if (syncResult.data.status == "PENDING") {
                    startPollingStatus(uid)
                }
            } else {
                // Cloud pull failed. Check local state.
                accountRepository.getAccountById(uid).onSuccess { localAccount ->
                    // If account exists locally AND they are stuck in PENDING, push to the memberships collection
                    if (localAccount.status == "PENDING") {
                        membershipRepository.createPendingAccount(uid, email, displayName)
                        startPollingStatus(uid)
                    }
                }.onFailure {
                    // Truly new user
                    membershipRepository.createPendingAccount(uid, email, displayName)
                    startPollingStatus(uid)
                }
            }
        }
    }

    private fun startPollingStatus(uid: String) {
        viewModelScope.launch {
            // Poll for up to 2 minutes (12 retries * 10 seconds)
            var retries = 0
            while (retries < 12) {
                kotlinx.coroutines.delay(10000) // Poll every 10 seconds
                val result = accountRepository.syncAccount(uid)
                if (result is com.azuratech.azuraengine.result.Result.Success && result.data.status == SessionManager.STATUS_ACTIVE) {
                    break // Approved!
                }
                retries++
            }
        }
    }

    /**
     * Activation logic moved to handler to maintain security key injection.
     */
    fun activateMembership() {
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
