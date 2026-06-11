package com.azuratech.azuratime.core.ui.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.azuratech.azuratime.core.navigation.NavigationRoutes
import com.azuratech.azuratime.features.session.ui.SessionPickerScreen

import com.azuratech.azuratime.BuildConfig

fun NavGraphBuilder.attendanceGraph(
    navController: androidx.navigation.NavController,
) {
    navigation(
        startDestination = if (BuildConfig.ENABLE_SUBJECT_SESSION) {
            NavigationRoutes.SESSION_PICKER
        } else {
            NavigationRoutes.ATTENDANCE_CAPTURE
        },
        route = NavigationRoutes.ATTENDANCE_GRAPH,
    ) {
        composable(NavigationRoutes.SESSION_PICKER) {
            SessionPickerScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onNavigateToScanner = { sessionId ->
                    navController.navigate(NavigationRoutes.ATTENDANCE_CAPTURE.replace("{sessionId}", sessionId))
                },
                onShowSnackbar = { /* Handle snackbar if needed or pass from MainScreen */ },
            )
        }

        composable(NavigationRoutes.SESSION_MANAGEMENT) {
            com.azuratech.azuratime.features.session.ui.SessionManagementScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = NavigationRoutes.ATTENDANCE_CAPTURE,
            arguments = listOf(
                navArgument("sessionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            com.azuratech.azuratime.features.attendance.ui.capture.AttendanceCaptureScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onBarcodeScanClick = { navController.navigate(NavigationRoutes.BARCODE_SCAN) },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(NavigationRoutes.BARCODE_SCAN) {
            com.azuratech.azuratime.features.attendance.ui.capture.BarcodeScanScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(NavigationRoutes.MANUAL_ATTENDANCE) {
            com.azuratech.azuratime.features.attendance.ui.manual.ManualAttendanceScreen(
                biometricViewModel = androidx.hilt.navigation.compose.hiltViewModel<com.azuratech.azuratime.features.biometric.ui.enroll.BiometricEnrollmentViewModel>(),
                attendanceViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                accountViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                classViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(NavigationRoutes.ATTENDANCE_HISTORY) {
            com.azuratech.azuratime.features.attendance.ui.AttendanceScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                accountViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun TextPlaceholder(text: String) {
    androidx.compose.material3.Text(text)
}
