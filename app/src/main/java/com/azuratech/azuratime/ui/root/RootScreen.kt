package com.azuratech.azuratime.ui.root

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.azuratech.azuratime.core.boot.BootState
import com.azuratech.azuratime.core.boot.BootViewModel
import com.azuratech.azuratime.ui.main.MainScreen
import com.azuratech.azuratime.ui.auth.LoginScreen
import com.azuratech.azuratime.ui.membership.MembershipScreen
import com.azuratech.azuratime.ui.main.MainViewModel
import com.azuratech.azuratime.ui.auth.AuthViewModel

@Composable
fun RootScreen() {
    val bootViewModel: BootViewModel = hiltViewModel()
    val mainViewModel: MainViewModel = hiltViewModel()
    val bootState by bootViewModel.state.collectAsState()

    Crossfade(targetState = bootState, animationSpec = tween(500), label = "RootState") { state ->
        when (state) {
            BootState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            BootState.NeedLogin -> {
                LoginScreen(
                    onLoginSuccess = { email: String, role: String? -> 
                        bootViewModel.recheck() 
                    }
                )
            }
            BootState.NeedActivation -> {
                val email = mainViewModel.getCurrentEmail()
                val authViewModel: AuthViewModel = hiltViewModel()
                MembershipScreen(
                    email = email,
                    onApproved = { bootViewModel.recheck() },
                    onLogout = { authViewModel.logout { bootViewModel.recheck() } }
                )
            }
            BootState.Ready -> {
                LaunchedEffect(Unit) { mainViewModel.initializeApp() }
                MainScreen()
            }
            else -> { /* Handle Error/Expired */ }
        }
    }
}