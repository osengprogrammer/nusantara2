package com.azuratech.azuratime.ui.user

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuratime.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// 🚥 Status UI untuk Jaringan Pertemanan
sealed class NetworkState {
    object Idle : NetworkState()
    object Loading : NetworkState()
    data class Success(val message: String) : NetworkState()
    data class Error(val message: String) : NetworkState()
    data class UserFound(val targetUser: UserEntity) : NetworkState()
}

@HiltViewModel
class NetworkViewModel @Inject constructor(
    application: Application,
    private val userRepository: UserRepository
) : AndroidViewModel(application) {

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
            try {
                // Cari di Firestore via UserRepository
                val targetUser = userRepository.getUserByEmailFromCloud(email)

                if (targetUser != null) {
                    _uiState.value = NetworkState.UserFound(targetUser)
                } else {
                    _uiState.value = NetworkState.Error("Waduh, guru dengan email $email tidak ditemukan.")
                }
            } catch (e: Exception) {
                _uiState.value = NetworkState.Error("Gagal mencari: ${e.message}")
            }
        }
    }

    // =====================================================
    // 📨 2. MANTRA KIRIM UNDANGAN (Add Friend)
    // =====================================================
    fun sendFriendRequest(myId: String, myName: String, myEmail: String, targetEmail: String) {
        viewModelScope.launch {
            _uiState.value = NetworkState.Loading
            try {
                val success = userRepository.sendFriendRequest(myId, myName, myEmail, targetEmail)
                if (success) {
                    _uiState.value = NetworkState.Success("Undangan seduluran berhasil dikirim ke $targetEmail!")
                } else {
                    _uiState.value = NetworkState.Error("Gagal mengirim undangan. Pastikan email benar.")
                }
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
                userRepository.acceptFriendRequest(myId, friendId)
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
                userRepository.rejectFriendRequest(myId, friendId)
                _uiState.value = NetworkState.Success("Permintaan dibatalkan/ditolak.")
            } catch (e: Exception) {
                _uiState.value = NetworkState.Error("Gagal menolak pertemanan: ${e.message}")
            }
        }
    }
}