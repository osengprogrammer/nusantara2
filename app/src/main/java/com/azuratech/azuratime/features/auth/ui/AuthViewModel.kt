package com.azuratech.azuratime.features.auth.ui

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuratime.features.auth.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🔐 AUTH VIEW MODEL (v3.2.0-ai-native)
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    application: Application,
    private val repository: AuthRepository,
) : AndroidViewModel(application) {

    private val _uiStateFlow = MutableStateFlow(AuthUiState())
    val uiStateFlow: StateFlow<AuthUiState> = _uiStateFlow.asStateFlow()

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.UpdateEmail -> _uiStateFlow.update { it.copy(email = event.email) }
            is AuthUiEvent.UpdatePassword -> _uiStateFlow.update { it.copy(password = event.password) }
            is AuthUiEvent.UpdateSchoolName -> _uiStateFlow.update { it.copy(schoolName = event.schoolName) }
            is AuthUiEvent.LoginWithEmail -> loginWithEmail()
            is AuthUiEvent.RegisterSchool -> registerNewSchool()
            is AuthUiEvent.SignInWithGoogle -> loginWithGoogle(event.idToken)
            is AuthUiEvent.ClearError -> _uiStateFlow.update { it.copy(error = null) }
            is AuthUiEvent.Logout -> logout(event.onComplete)
            is AuthUiEvent.NavigateToDashboard -> { /* Managed by screen */ }
        }
    }

    private fun loginWithEmail() {
        // Future implementation
    }

    private fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true, isGoogleSigning = true, error = null) }

            when (val result = repository.signInWithGoogle(idToken)) {
                is Result.Success -> {
                    val (account, isNewAccount) = result.data
                    if (isNewAccount) {
                        val hardwareId = Settings.Secure.getString(
                            getApplication<Application>().contentResolver,
                            Settings.Secure.ANDROID_ID,
                        )
                        val autoRegData = mapOf(
                            "email" to account.email,
                            "name" to account.name.ifBlank { "Account Baru" },
                            "status" to "PENDING",
                            "role" to "PENDING",
                            "hardwareId" to hardwareId,
                            "createdAt" to System.currentTimeMillis(),
                        )

                        when (val regResult = repository.registerMembership(account.accountId, autoRegData)) {
                            is Result.Success -> {
                                _uiStateFlow.update {
                                    it.copy(
                                        isLoading = false,
                                        isGoogleSigning = false,
                                        authStatus = AuthStatus.LoggedIn,
                                        accountEmail = account.email,
                                        accountRole = "PENDING",
                                    )
                                }
                            }
                            is Result.Failure -> {
                                _uiStateFlow.update {
                                    it.copy(
                                        isLoading = false,
                                        isGoogleSigning = false,
                                        error = regResult.error.message,
                                    )
                                }
                            }
                            is Result.Loading -> {}
                        }
                    } else {
                        val schoolId = account.activeSchoolId ?: ""
                        val currentRole = account.memberships[schoolId]?.role ?: "USER"
                        _uiStateFlow.update {
                            it.copy(
                                isLoading = false,
                                isGoogleSigning = false,
                                authStatus = AuthStatus.LoggedIn,
                                accountEmail = account.email,
                                accountRole = currentRole,
                            )
                        }
                    }
                }
                is Result.Failure -> {
                    _uiStateFlow.update { it.copy(isLoading = false, isGoogleSigning = false, error = result.error.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun registerNewSchool() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val randomNumber = (1..5000).random()
        val defaultName = "Azura Candi $randomNumber"
        val schoolName = _uiStateFlow.value.schoolName.ifBlank { defaultName }

        val finalData = mapOf(
            "accountId" to uid,
            "schoolName" to schoolName,
            "status" to "PENDING",
            "role" to "ADMIN",
            "createdAt" to System.currentTimeMillis(),
        )

        viewModelScope.launch {
            _uiStateFlow.update { it.copy(isLoading = true, authStatus = AuthStatus.Registering) }
            when (val regResult = repository.registerMembership(uid, finalData)) {
                is Result.Success -> {
                    _uiStateFlow.update { it.copy(isLoading = false, authStatus = AuthStatus.LoggedIn) }
                }
                is Result.Failure -> {
                    _uiStateFlow.update { it.copy(isLoading = false, error = regResult.error.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllDataAndSignOut()
            _uiStateFlow.value = AuthUiState()
            onComplete()
        }
    }
}
