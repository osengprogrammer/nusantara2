package com.azuratech.azuratime.ui.core.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.navigation.NavigationRoutes

fun NavGraphBuilder.attendanceGraph(
    navController: androidx.navigation.NavController
) {
    navigation(
        startDestination = NavigationRoutes.CHECK_IN,
        route = NavigationRoutes.ATTENDANCE_GRAPH
    ) {
        composable(NavigationRoutes.CHECK_IN) {
            com.azuratech.azuratime.ui.checkin.AttendanceCaptureScreen(
                useBackCamera = false,
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                teacherEmail = "", // This will be passed via ViewModel or provided externally
                onBarcodeScanClick = { navController.navigate(NavigationRoutes.BARCODE_SCAN) }
            )
        }
        composable(NavigationRoutes.BARCODE_SCAN) {
            com.azuratech.azuratime.ui.checkin.BarcodeScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                teacherEmail = ""
            )
        }
        composable(NavigationRoutes.CHECKIN_HISTORY) {
            com.azuratech.azuratime.ui.checkin.CheckInRecordScreen(
                userEmail = "",
                onNavigateBack = { navController.popBackStack() },
                checkInViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                userViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                classViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            )
        }
        composable(NavigationRoutes.MANUAL_ATTENDANCE) {
            com.azuratech.azuratime.ui.checkin.ManualAttendanceScreen(
                faceViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                checkInViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                userViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                classViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
