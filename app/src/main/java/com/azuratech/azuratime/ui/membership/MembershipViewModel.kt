package com.azuratech.azuratime.ui.membership

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.repo.MembershipDocUpdate
import com.azuratech.azuratime.data.repo.MembershipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🛠️ MEMBERSHIP VIEW MODEL
 * 100% Menggunakan Hilt. Bebas dari Factory manual.
 */
@HiltViewModel
class MembershipViewModel @Inject constructor(
    private val repository: MembershipRepository // 🔥 FIX: Diinjeksi otomatis oleh Hilt
) : ViewModel() {

    private val _state = MutableStateFlow<MembershipState>(MembershipState.Idle)
    val state: StateFlow<MembershipState> = _state.asStateFlow()

    private var observeJob: Job? = null

    fun checkMembership(email: String, displayName: String? = null) {
        _state.value = MembershipState.Loading
        
        val uid = repository.getCurrentUid()
        if (uid == null) {
            _state.value = MembershipState.Error("Sesi login tidak valid. Silakan login ulang.")
            return
        }

        viewModelScope.launch {
            try {
                val whitelistData = repository.checkWhitelisted(uid)
                if (whitelistData != null) {
                    withContext(Dispatchers.Main) {
                        activateMembership(whitelistData)
                    }
                    return@launch
                }

                val exists = repository.checkMembershipExists(uid)
                if (!exists) {
                    repository.createPendingUser(uid, email, displayName)
                    _state.value = MembershipState.Pending
                } 
                
                observeMembership(uid)

            } catch (e: Exception) {
                _state.value = MembershipState.Error(e.message ?: "Membership check failed")
            }
        }
    }

    private fun observeMembership(uid: String) {
        observeJob?.cancel()
        
        observeJob = viewModelScope.launch {
            repository.observeMembershipFlow(uid).collect { update ->
                when (update) {
                    is MembershipDocUpdate.StatusChanged -> {
                        when (update.status) {
                            "PENDING" -> {
                                repository.savePendingStatus()
                                _state.value = MembershipState.Pending
                            }
                            "ACTIVE", "APPROVED" -> {
                                if (update.data?.containsKey("secureIsoKey") == true) {
                                    activateMembership(update.data)
                                }
                            }
                            "REJECTED" -> {
                                _state.value = MembershipState.Rejected(update.reason)
                            }
                        }
                    }
                    is MembershipDocUpdate.DocumentMissing -> {
                        checkWhitelistedFinal(uid)
                    }
                    is MembershipDocUpdate.Error -> {
                        _state.value = MembershipState.Error(update.message)
                    }
                }
            }
        }
    }

    private fun checkWhitelistedFinal(uid: String) {
        viewModelScope.launch {
            try {
                val finalData = repository.pollWhitelistedFinal(uid)
                if (finalData != null) {
                    withContext(Dispatchers.Main) {
                        activateMembership(finalData)
                    }
                } else {
                    _state.value = MembershipState.Error("Activation record moved but not found.")
                }
            } catch (e: Exception) {
                _state.value = MembershipState.Error("Final check failed.")
            }
        }
    }

    private fun activateMembership(data: Map<String, Any>?) {
        val success = repository.activateSession(data)
        if (success) {
            _state.value = MembershipState.Approved
        } else {
            _state.value = MembershipState.Error("Activation failed: Invalid Security Key")
        }
    }

    override fun onCleared() {
        super.onCleared()
        observeJob?.cancel()
    }
}
// 🔥 FACTORY DIHAPUS SEPENUHNYA DARI SINI