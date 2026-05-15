package com.azuratech.azuratime.features.staff.ui.membership

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.sync.SyncManager
import com.azuratech.azuratime.features.staff.data.local.StaffAccountEntity
import com.azuratech.azuratime.features.staff.data.local.Membership
import com.azuratech.azuratime.features.staff.data.local.toProfile
import com.azuratech.azuratime.features.staff.domain.repository.AccessRequestRepository
import com.azuratech.azuratime.features.staff.data.repo.MembershipRepository
import com.azuratech.azuratime.features.staff.data.repo.StaffAccountRepository
import com.azuratech.azuratime.features.staff.domain.model.AccessRequestProfile
import com.azuratech.azuratime.features.biometric.data.local.BiometricFaceEntity
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
 * Observes StaffAccountEntity and AccessRequestProfile from Room.
 */
@HiltViewModel
class MembershipViewModel @Inject constructor(
    private val userRepository: StaffAccountRepository,
    private val accessRequestRepository: AccessRequestRepository,
    private val membershipRepository: MembershipRepository,
    private val sessionManager: SessionManager,
    private val syncManager: SyncManager
) : ViewModel() {

    // =====================================================
    // 📊 REACTIVE STREAMS (SSOT)
    // =====================================================

    @OptIn(ExperimentalCoroutinesApi::class)
    val user: StateFlow<StaffAccountEntity?> = sessionManager.currentUserIdFlow
        .filterNotNull()
        .flatMapLatest { uid -> userRepository.getUserDao().observeUserById(uid) }
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

    val memberships: StateFlow<List<com.azuratech.azuratime.features.staff.data.local.Membership>> = user.map { 
        it?.memberships?.values?.toList() ?: emptyList() 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // =====================================================
    // 🛠️ ACTION HANDLERS
    // =====================================================

    fun checkMembership(email: String, displayName: String? = null) {
        val uid = sessionManager.getCurrentUserId() ?: return
        
        viewModelScope.launch {
            // Trigger background sync to refresh Room from Firestore
            syncManager.enqueueProfileSync(uid)
            syncManager.enqueueAccessSync(uid)

            // If user doesn't exist locally, create a stub to trigger UI
            if (userRepository.getUserDao().getUserById(uid) == null) {
                membershipRepository.createPendingUser(uid, email, displayName)
            }
        }
    }

    /**
     * Activation logic moved to handler to maintain security key injection.
     */
    fun activateMembership() {
        viewModelScope.launch {
            val uid = sessionManager.getCurrentUserId() ?: return@launch
            val userEntity = userRepository.getUserDao().getUserById(uid)
            userEntity?.let {
                val data = mapOf(
                    "userId" to it.userId,
                    "status" to it.status,
                    "activeSchoolId" to (it.activeSchoolId ?: ""),
                    "role" to it.role,
                    "memberships" to it.memberships
                )
                membershipRepository.activateSession(data)
            }
        }
    }
}

// 🔥 FACTORY DIHAPUS SEPENUHNYA DARI SINI