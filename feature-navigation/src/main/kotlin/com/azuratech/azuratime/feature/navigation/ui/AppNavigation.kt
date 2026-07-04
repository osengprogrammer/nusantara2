package com.azuratech.azuratime.feature.navigation.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "attendance_home") {
        // Core attendance routes (shared by both school and office flavors)
        composable("attendance_home") {
            // Attendance home screen - vocabulary is handled inside the screen
        }

        composable("biometric_enrollment") {
            // Biometric enrollment screen
        }

        composable("attendance_history") {
            // Attendance history screen
        }

        composable("attendance_capture") {
            // Attendance capture/check-in screen
        }

        composable("profile") {
            // User profile screen
        }

        composable("settings") {
            // Settings screen
        }
    }
}
