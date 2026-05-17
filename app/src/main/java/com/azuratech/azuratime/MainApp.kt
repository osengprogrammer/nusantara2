package com.azuratech.azuratime

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.azuratech.azuratime.core.boot.BootState
import com.azuratech.azuratime.core.boot.BootViewModel
import com.azuratech.azuratime.core.ui.MainScreen
import com.azuratech.azuratime.features.auth.ui.LoginScreen
import com.azuratech.azuratime.features.account.ui.membership.MembershipScreen
import com.azuratech.azuratime.core.ui.MainViewModel
import com.azuratech.azuratime.features.auth.ui.AuthViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MainApp(onBootReady: () -> Unit = {}) {
    val bootViewModel: BootViewModel = hiltViewModel()
    val bootState by bootViewModel.stateFlow.collectAsState()

    LaunchedEffect(bootState) {
        if (bootState != BootState.Loading) {
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
                BootState.Loading -> LoadingScreen(onRetry = { bootViewModel.recheck() })

                BootState.NeedLogin -> {
                    LoginScreen(
                        onNavigateToDashboard = {
                            bootViewModel.recheck()
                        },
                    )
                }

                BootState.NeedActivation -> {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    val email = firebaseUser?.email ?: ""
                    val displayName = firebaseUser?.displayName ?: ""

                    MembershipScreen(
                        email = email,
                        displayName = displayName,
                        onApprovedClick = { bootViewModel.recheck() },
                        onLogoutClick = { authViewModel.logout { bootViewModel.recheck() } },
                    )
                }

                BootState.Ready -> {
                    val mainViewModel: MainViewModel = hiltViewModel()
                    val isRevoked by mainViewModel.isRevokedFlow.collectAsState()

                    LaunchedEffect(Unit) {
                        mainViewModel.initializeApp()
                    }

                    if (isRevoked) {
                        val authViewModel: AuthViewModel = hiltViewModel()
                        SecurityAlertDialog(
                            message = "Akses Anda telah dicabut.",
                            onReLogin = {
                                authViewModel.logout()
                                bootViewModel.recheck()
                            },
                        )
                    } else {
                        MainScreen()
                    }
                }

                BootState.Expired -> {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    SecurityAlertDialog(
                        message = "Sesi Anda telah berakhir.",
                        onReLogin = {
                            authViewModel.logout()
                            bootViewModel.recheck()
                        },
                    )
                }

                is BootState.Error -> {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    SecurityAlertDialog(
                        message = state.message,
                        onReLogin = {
                            authViewModel.logout()
                            bootViewModel.recheck()
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingScreen(onRetry: () -> Unit) {
    var showRetry by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5000)
        showRetry = true
    }
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!showRetry) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Menyiapkan Enkripsi...", style = MaterialTheme.typography.bodySmall)
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
        title = { Text("Keamanan Sistem", fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onReLogin) { Text("Login Ulang") } },
        dismissButton = { TextButton(onClick = { android.os.Process.killProcess(android.os.Process.myPid()) }) { Text("Tutup") } },
    )
}
