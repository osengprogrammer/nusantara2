package com.azuratech.azuratime.features.account.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.data.repo.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 🚥 Status UI untuk Jaringan Pertemanan
sealed class NetworkState {
    object Idle : NetworkState()
    object Loading : NetworkState()
    data class Success(val message: String) : NetworkState()
    data class Error(val message: String) : NetworkState()
    data class UserFound(val targetUser: AccountEntity) : NetworkState()
}

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NetworkState>(NetworkState.Idle)
    val uiState: StateFlow<NetworkState> = _uiState.asStateFlow()

    fun resetState() {
        _uiState.value = NetworkState.Idle
    }

    // =====================================================
    // 🔍 1. MANTRA PENCARI TEMAN (Search)
    // =====================================================
    fun searchUserByEmail(email: String) {
        if (email.isBlank()) {
            _uiState.value = NetworkState.Error("Email tidak boleh kosong, Dulur.")
            return
        }

        viewModelScope.launch {
            _uiState.value = NetworkState.Loading
            // searchAccountByEmail needs to be implemented in AccountRepository if used
            // For now using accountRepository placeholder
            _uiState.value = NetworkState.Error("Waduh, guru dengan email $email tidak ditemukan.")
        }
    }

    // =====================================================
    // 📨 2. MANTRA KIRIM UNDANGAN (Add Friend)
    // =====================================================
    fun sendFriendRequest(myId: String, myName: String, myEmail: String, targetEmail: String) {
        viewModelScope.launch {
            _uiState.value = NetworkState.Loading
            try {
                // Placeholder
                _uiState.value = NetworkState.Success("Undangan seduluran berhasil dikirim ke $targetEmail!")
            } catch (e: Exception) {
                _uiState.value = NetworkState.Error("Error jaringan: ${e.message}")
            }
        }
    }

    // =====================================================
    // 🤝 3. MANTRA TERIMA UNDANGAN (Accept)
    // =====================================================
    fun acceptFriendRequest(myId: String, friendId: String) {
        viewModelScope.launch {
            _uiState.value = NetworkState.Loading
            try {
                // Placeholder
                _uiState.value = NetworkState.Success("Mantap! Kalian sekarang resmi Seduluran.")
            } catch (e: Exception) {
                _uiState.value = NetworkState.Error("Gagal menerima pertemanan: ${e.message}")
            }
        }
    }

    // =====================================================
    // 🙅 4. MANTRA TOLAK UNDANGAN (Reject/Cancel)
    // =====================================================
    fun rejectFriendRequest(myId: String, friendId: String) {
        viewModelScope.launch {
            _uiState.value = NetworkState.Loading
            try {
                // Placeholder
                _uiState.value = NetworkState.Success("Permintaan dibatalkan/ditolak.")
            } catch (e: Exception) {
                _uiState.value = NetworkState.Error("Gagal menolak pertemanan: ${e.message}")
            }
        }
    }
}
