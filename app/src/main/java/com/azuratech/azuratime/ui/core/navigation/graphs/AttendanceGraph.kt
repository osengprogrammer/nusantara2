package com.azuratech.azuratime.ui.core.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.azuratech.azuratime.core.navigation.Screen

fun NavGraphBuilder.attendanceGraph(
    navController: androidx.navigation.NavController
) {
    navigation(
        startDestination = Screen.CheckIn.route,
        route = "attendance_graph"
    ) {
        composable(Screen.CheckIn.route) {
            com.azuratech.azuratime.ui.checkin.CheckInScreen(
                useBackCamera = false,
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                teacherEmail = "", // This will be passed via ViewModel or provided externally
                onNavigateToBarcode = { navController.navigate(Screen.BarcodeScan.route) }
            )
        }
        composable(Screen.BarcodeScan.route) {
            com.azuratech.azuratime.ui.checkin.BarcodeScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                teacherEmail = ""
            )
        }
        composable(Screen.CheckInRecordEntity.route) {
            com.azuratech.azuratime.ui.checkin.CheckInRecordScreen(
                userEmail = "",
                onNavigateBack = { navController.popBackStack() },
                checkInViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                userViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                classViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            )
        }
        composable(Screen.ManualAttendance.route) {
            com.azuratech.azuratime.ui.checkin.ManualAttendanceScreen(
                faceViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                checkInViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                userViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                classViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
