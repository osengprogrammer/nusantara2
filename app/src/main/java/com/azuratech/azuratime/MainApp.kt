package com.azuratech.azuratime

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.boot.BootUiEvent
import com.azuratech.azuratime.core.boot.BootUiState
import com.azuratech.azuratime.core.boot.BootViewModel
import com.azuratech.azuratime.core.ui.MainScreen
import com.azuratech.azuratime.core.ui.MainUiEvent
import com.azuratech.azuratime.core.ui.MainViewModel
import com.azuratech.azuratime.features.account.ui.membership.MembershipScreen
import com.azuratech.azuratime.features.auth.ui.AuthUiEvent
import com.azuratech.azuratime.features.auth.ui.AuthViewModel
import androidx.navigation.compose.*
import com.azuratech.azuratime.features.auth.ui.LoginScreen
import com.azuratech.azuratime.features.auth.ui.WelcomeScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MainApp(onBootReady: () -> Unit = {}) {
    val bootViewModel: BootViewModel = hiltViewModel()
    val bootState by bootViewModel.uiStateFlow.collectAsStateWithLifecycle()
    val isLoggingOut by bootViewModel.isLoggingOut.collectAsStateWithLifecycle()
    val isSessionClearing by bootViewModel.isSessionClearing.collectAsStateWithLifecycle()

    val isClearingEffect = isLoggingOut || isSessionClearing

    LaunchedEffect(bootState) {
        if (bootState != BootUiState.Loading) {
            onBootReady()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Crossfade(
            targetState = bootState,
            animationSpec = tween(durationMillis = 400),
            label = "BootStateTransition",
        ) { state ->
            when (state) {
                BootUiState.Loading -> LoadingScreen(
                    onRetry = { bootViewModel.onEvent(BootUiEvent.Recheck) },
                    isLoggingOut = isClearingEffect,
                )

                BootUiState.NeedLogin -> {
                    val authNavController = rememberNavController()
                    NavHost(
                        navController = authNavController,
                        startDestination = "welcome",
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        composable("welcome") {
                            WelcomeScreen(onNavigateToLogin = { authNavController.navigate("login") })
                        }
                        composable("login") {
                            LoginScreen(
                                onNavigateToDashboard = {
                                    bootViewModel.onEvent(BootUiEvent.Recheck)
                                },
                            )
                        }
                    }
                }

                BootUiState.NeedActivation -> {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    val email = firebaseUser?.email ?: ""
                    val displayName = firebaseUser?.displayName ?: ""

                    MembershipScreen(
                        email = email,
                        displayName = displayName,
                        onApprovedClick = { bootViewModel.onEvent(BootUiEvent.Recheck) },
                        onLogoutClick = {
                            authViewModel.onEvent(
                                AuthUiEvent.Logout(),
                            )
                        },
                    )
                }

                BootUiState.Ready -> {
                    val mainViewModel: MainViewModel = hiltViewModel()
                    val mainState by mainViewModel.uiStateFlow.collectAsStateWithLifecycle()

                    LaunchedEffect(Unit) {
                        mainViewModel.onEvent(MainUiEvent.InitializeApp)
                    }

                    if (mainState.isRevoked) {
                        val authViewModel: AuthViewModel = hiltViewModel()
                        SecurityAlertDialog(
                            message = "Akses Anda telah dicabut.",
                            onReLogin = {
                                authViewModel.onEvent(
                                    AuthUiEvent.Logout(),
                                )
                            },
                        )
                    } else {
                        MainScreen()
                    }
                }

                is BootUiState.Error -> {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    SecurityAlertDialog(
                        message = state.message,
                        onReLogin = {
                            authViewModel.onEvent(
                                AuthUiEvent.Logout {
                                    bootViewModel.onEvent(BootUiEvent.Recheck)
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingScreen(onRetry: () -> Unit, isLoggingOut: Boolean = false) {
    var showRetry by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5000)
        showRetry = true
    }
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!showRetry) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                if (isLoggingOut) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Logging out...", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) { Text("Segarkan") }
            }
        }
    }
}

@Composable
fun SecurityAlertDialog(message: String, onReLogin: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("System Security", fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onReLogin) { Text("Relogin") } },
        dismissButton = { TextButton(onClick = { android.os.Process.killProcess(android.os.Process.myPid()) }) { Text("Close") } },
    )
}
