package com.azuratech.azuratime.core.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.boot.BootUiEvent
import com.azuratech.azuratime.core.boot.BootUiState
import com.azuratech.azuratime.core.boot.BootViewModel
import com.azuratech.azuratime.features.account.ui.membership.MembershipScreen
import com.azuratech.azuratime.features.auth.ui.AuthUiEvent
import com.azuratech.azuratime.features.auth.ui.AuthViewModel
import com.azuratech.azuratime.features.auth.ui.LoginScreen

@Composable
fun RootScreen() {
    val bootViewModel: BootViewModel = hiltViewModel()
    val mainViewModel: MainViewModel = hiltViewModel()
    val bootState by bootViewModel.uiStateFlow.collectAsStateWithLifecycle()
    val mainState by mainViewModel.uiStateFlow.collectAsStateWithLifecycle()

    Crossfade(targetState = bootState, animationSpec = tween(500), label = "RootState") { state ->
        when (state) {
            BootUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            BootUiState.NeedLogin -> {
                LoginScreen(
                    onNavigateToDashboard = {
                        bootViewModel.onEvent(BootUiEvent.Recheck)
                    },
                )
            }
            BootUiState.NeedActivation -> {
                val email = mainState.currentEmail
                val authViewModel: AuthViewModel = hiltViewModel()
                MembershipScreen(
                    email = email,
                    onApprovedClick = { bootViewModel.onEvent(BootUiEvent.Recheck) },
                    onLogoutClick = {
                        authViewModel.onEvent(
                            AuthUiEvent.Logout {
                                bootViewModel.onEvent(BootUiEvent.Recheck)
                            },
                        )
                    },
                )
            }
            BootUiState.Ready -> {
                LaunchedEffect(Unit) { mainViewModel.onEvent(MainUiEvent.InitializeApp) }
                MainScreen()
            }
            else -> { /* Handle Error/Expired */ }
        }
    }
}
