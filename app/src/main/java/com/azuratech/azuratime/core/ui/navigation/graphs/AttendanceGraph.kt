package com.azuratech.azuratime.core.ui.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.azuratech.azuratime.core.navigation.NavigationRoutes

fun NavGraphBuilder.attendanceGraph(
    navController: androidx.navigation.NavController,
) {
    navigation(
        startDestination = NavigationRoutes.ATTENDANCE_CAPTURE,
        route = NavigationRoutes.ATTENDANCE_GRAPH,
    ) {
        composable(NavigationRoutes.ATTENDANCE_CAPTURE) {
            com.azuratech.azuratime.features.attendance.ui.capture.AttendanceCaptureScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onBarcodeScanClick = { navController.navigate(NavigationRoutes.BARCODE_SCAN) },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(NavigationRoutes.BARCODE_SCAN) {
            TextPlaceholder("Barcode Screen")
        }

        composable(NavigationRoutes.MANUAL_ATTENDANCE) {
            com.azuratech.azuratime.features.attendance.ui.manual.ManualAttendanceScreen(
                biometricViewModel = androidx.hilt.navigation.compose.hiltViewModel<com.azuratech.azuratime.features.biometric.ui.enroll.BiometricEnrollmentViewModel>(),
                attendanceViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                userViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                classViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun TextPlaceholder(text: String) {
    androidx.compose.material3.Text(text)
}
