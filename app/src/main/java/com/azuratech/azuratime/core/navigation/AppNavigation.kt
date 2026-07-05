package com.azuratech.azuratime.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.azuratech.azuratime.features.auth.ui.LoginScreen
import com.azuratech.azuratime.features.auth.ui.AuthViewModel
import com.azuratech.azuratime.features.auth.ui.AuthStatus
import com.azuratech.azuratime.features.dashboard.ui.DashboardScreen
import com.azuratech.azuratime.R

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val uiState by authViewModel.uiStateFlow.collectAsState()
    val context = LocalContext.current

    // Get web client ID from app resources
    val webClientId = context.getString(R.string.my_web_client_id)

    NavHost(
        navController = navController,
        startDestination = if (uiState.authStatus == AuthStatus.LoggedIn) "dashboard" else "login"
    ) {
        // Login Screen
        composable("login") {
            LoginScreen(
                onNavigateToDashboard = {
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                }
                }


            )
        }

        // Actual Dashboard Screen
        composable("dashboard") {
            DashboardScreen(navController = navController)
        }

        // Legacy routes (kept for backward compatibility)
        composable("attendance_home") {
            DashboardScreen(navController = navController)
        }

        composable("biometric_enrollment") {
            // TODO: Implement biometric enrollment screen
        }

        composable("attendance_history") {
            // TODO: Implement attendance history screen
        }

        composable("attendance_capture") {
            // TODO: Implement attendance capture screen
        }

        composable("profile") {
            // TODO: Implement profile screen
        }

        composable("settings") {
            // TODO: Implement settings screen
        }
    }
}
