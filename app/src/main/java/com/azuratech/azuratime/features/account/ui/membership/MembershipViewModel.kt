package com.azuratech.azuratime.features.account.ui.membership

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.data.local.Membership
import com.azuratech.azuratime.features.account.domain.repository.AccessRequestRepository
import com.azuratech.azuratime.features.account.data.repo.MembershipRepository
import com.azuratech.azuratime.features.account.data.repo.AccountRepository
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
) : ViewModel() {

    // =====================================================
    // 📊 REACTIVE STREAMS (SSOT)
    // =====================================================

    @OptIn(ExperimentalCoroutinesApi::class)
    val user: StateFlow<AccountEntity?> = sessionManager.currentUserIdFlow
        .filterNotNull()
        .flatMapLatest { uid -> accountRepository.observeAccountEntity(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val accessRequests: StateFlow<List<AccessRequestProfile>> = sessionManager.currentUserIdFlow
        .filterNotNull()
        .flatMapLatest { uid ->
            accessRequestRepository.observeRequestsByUser(uid)
                .map { entities -> entities.map { it.toProfile() } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 🔥 DERIVED UI STATE
     * Combines user status and pending requests for a single SSOT state.
     */
    val state: StateFlow<MembershipState> = combine(user, accessRequests) { user, requests ->
        when {
            user == null -> MembershipState.Loading
            user.status == "PENDING" -> MembershipState.Pending
            user.status == SessionManager.STATUS_ACTIVE -> MembershipState.Approved
            user.status == "REJECTED" -> MembershipState.Rejected("Akun Anda ditolak oleh administrator.")
            requests.isNotEmpty() -> MembershipState.Pending
            else -> MembershipState.Idle
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MembershipState.Loading)

    val memberships: StateFlow<List<com.azuratech.azuratime.features.account.data.local.Membership>> = user.map {
        it?.memberships?.values?.toList() ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // =====================================================
    // 🛠️ ACTION HANDLERS
    // =====================================================

    fun checkMembership(email: String, displayName: String? = null) {
        val uid = sessionManager.getCurrentUserId() ?: return

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
                val localAccount = accountRepository.getAccountDao().getAccountById(uid)

                // If account doesn't exist locally OR they are stuck in PENDING, push to the memberships collection
                if (localAccount == null || localAccount.status == "PENDING") {
                    membershipRepository.createPendingUser(uid, email, displayName)
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
            val uid = sessionManager.getCurrentUserId() ?: return@launch
            val accountEntity = accountRepository.getAccountDao().getAccountById(uid)
            accountEntity?.let {
                val data = mapOf(
                    "accountId" to it.accountId,
                    "status" to it.status,
                    "activeSchoolId" to (it.activeSchoolId ?: ""),
                    "role" to it.role,
                    "memberships" to it.memberships,
                )
                membershipRepository.activateSession(data)
            }
        }
    }
}
