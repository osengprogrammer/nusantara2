package com.azuratech.azuratime.ui.auth

import android.app.Application
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuratime.data.repository.AuthRepository
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
    private val repository: AuthRepository
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
                val (user, isNewUser) = repository.signInWithGoogle(idToken)

                if (user == null) {
                    _authState.value = AuthState.Error("Login gagal: Data pengguna tidak ditemukan.")
                    return@launch
                }

                if (isNewUser) {
                    val hardwareId = Settings.Secure.getString(
                        getApplication<Application>().contentResolver,
                        Settings.Secure.ANDROID_ID
                    )
                    val autoRegData = mapOf(
                        "email" to user.email,
                        "name" to user.name.ifBlank { "User Baru" },
                        "status" to "PENDING",
                        "role" to "PENDING",
                        "hardwareId" to hardwareId,
                        "createdAt" to System.currentTimeMillis()
                    )
                    
                    try {
                        repository.registerMembership(user.userId, autoRegData)
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Auto-register Firestore failed: ${e.message}")
                    }
                    
                    // Gunakan state sesuai file AuthState.kt kamu
                    _authState.value = AuthState.Success(user.email, "PENDING")
                    
                } else {
                    val schoolId = user.activeSchoolId ?: ""
                    val currentRole = user.memberships[schoolId]?.role ?: "USER"
                    
                    _authState.value = AuthState.Success(user.email, currentRole)
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
            this["userId"] = uid
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