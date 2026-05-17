package com.azuratech.azuratime.features.auth.ui

import android.app.Application
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.features.auth.data.repo.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 🔥 sealed class AuthState TIDAK ADA DI SINI LAGI (mengambil dari AuthState.kt)

@HiltViewModel
class AuthViewModel @Inject constructor(
    application: Application,
    private val repository: AuthRepository,
) : AndroidViewModel(application) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            try {
                val (account, isNewAccount) = repository.signInWithGoogle(idToken)

                if (account == null) {
                    _authState.value = AuthState.Error("Login gagal: Data akun tidak ditemukan.")
                    return@launch
                }

                if (isNewAccount) {
                    val hardwareId = Settings.Secure.getString(
                        getApplication<Application>().contentResolver,
                        Settings.Secure.ANDROID_ID,
                    )
                    val autoRegData = mapOf(
                        "email" to account.email,
                        "name" to account.name.ifBlank { "User Baru" },
                        "status" to "PENDING",
                        "role" to "PENDING",
                        "hardwareId" to hardwareId,
                        "createdAt" to System.currentTimeMillis(),
                    )

                    try {
                        repository.registerMembership(account.accountId, autoRegData)
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Auto-register Firestore failed: ${e.message}")
                    }

                    // Gunakan state sesuai file AuthState.kt kamu
                    _authState.value = AuthState.Success(account.email, "PENDING")
                } else {
                    val schoolId = account.activeSchoolId ?: ""
                    val currentRole = account.memberships[schoolId]?.role ?: "USER"

                    _authState.value = AuthState.Success(account.email, currentRole)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Login gagal: ${e.localizedMessage}")
            }
        }
    }

    fun registerNewSchool(data: Map<String, Any?>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val randomNumber = (1..5000).random()
        val defaultName = "Azura Candi $randomNumber"
        val finalData = data.toMutableMap().apply {
            this["accountId"] = uid
            if (this["schoolName"]?.toString().isNullOrBlank()) {
                this["schoolName"] = defaultName
            }
        }

        viewModelScope.launch {
            try {
                @Suppress("UNCHECKED_CAST")
                repository.registerMembership(uid, finalData as Map<String, Any>)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Gagal daftar candi: ${e.message}")
            }
        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllDataAndSignOut()
            _authState.value = AuthState.Idle
            onComplete()
        }
    }
}
