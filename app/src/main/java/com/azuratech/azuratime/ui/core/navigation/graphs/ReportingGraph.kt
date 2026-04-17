package com.azuratech.azuratime.ui.core.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.azuratech.azuratime.core.navigation.Screen

fun NavGraphBuilder.reportingGraph(
    navController: androidx.navigation.NavController
) {
    navigation(
        startDestination = Screen.AttendanceMatrix.route,
        route = "reporting_graph"
    ) {
        composable(Screen.AttendanceMatrix.route) {
            com.azuratech.azuratime.ui.report.AttendanceMatrixScreen(
                onBack = { navController.popBackStack() },
                onCellClick = { faceId, name, date ->
                    navController.navigate(Screen.DailyDetail.createRoute(faceId, name, date.toString()))
                }
            )
        }
        composable(
            route = Screen.DailyDetail.route,
            arguments = listOf(
                androidx.navigation.navArgument("faceId") { androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("name") { androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("date") { androidx.navigation.NavType.StringType }
            )
        ) { entry ->
            com.azuratech.azuratime.ui.report.DailyDetailScreen(
                faceId = entry.arguments?.getString("faceId") ?: "",
                studentName = entry.arguments?.getString("name") ?: "",
                dateString = entry.arguments?.getString("date") ?: "",
                checkInViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                userViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                classViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onBack = { navController.popBackStack() },
                onNavigateToManual = { fId, dStr ->
                    navController.navigate(Screen.ManualAttendance.createRoute(fId, dStr))
                }
            )
        }
    }
}
