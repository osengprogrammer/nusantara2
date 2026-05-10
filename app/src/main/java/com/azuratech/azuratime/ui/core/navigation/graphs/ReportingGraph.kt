package com.azuratech.azuratime.ui.core.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.navigation.NavigationRoutes

fun NavGraphBuilder.reportingGraph(
    navController: androidx.navigation.NavController
) {
    navigation(
        startDestination = NavigationRoutes.ATTENDANCE_MATRIX,
        route = NavigationRoutes.REPORTING_GRAPH
    ) {
        composable(NavigationRoutes.ATTENDANCE_MATRIX) {
            com.azuratech.azuratime.ui.attendance.AttendanceMatrixScreen(
                onNavigateBack = { navController.popBackStack() },
                onCellClick = { faceId, name, date ->
                    navController.navigate(Screen.DailyDetail.createRoute(faceId, name, date.toString()))
                }
            )
        }
        composable(
            route = NavigationRoutes.DAILY_DETAIL,
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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToManual = { fId, dStr ->
                    navController.navigate(Screen.ManualAttendance.createRoute(fId, dStr))
                }
            )
        }
    }
}
