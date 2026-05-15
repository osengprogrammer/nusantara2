package com.azuratech.azuratime.core.ui.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.core.navigation.NavigationRoutes

fun NavGraphBuilder.reportingGraph(
    navController: androidx.navigation.NavController
) {
    navigation(
        startDestination = NavigationRoutes.ATTENDANCE_MATRIX,
        route = NavigationRoutes.REPORTING_GRAPH
    ) {
        composable(NavigationRoutes.ATTENDANCE_MATRIX) {
            com.azuratech.azuratime.features.reporting.ui.matrix.AttendanceMatrixScreen(
                onNavigateBack = { navController.popBackStack() },
                onCellClick = { studentId, name, date ->
                    navController.navigate(Screen.DailyDetail.createRoute(studentId, name, date.toString()))
                }
            )
        }
        composable(
            route = NavigationRoutes.DAILY_DETAIL,
            arguments = listOf(
                androidx.navigation.navArgument("studentId") { androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("name") { androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("date") { androidx.navigation.NavType.StringType }
            )
        ) { entry ->
            com.azuratech.azuratime.features.reporting.ui.daily.DailyDetailScreen(
                faceId = entry.arguments?.getString("studentId") ?: "",
                studentName = entry.arguments?.getString("name") ?: "",
                dateString = entry.arguments?.getString("date") ?: "",
                onNavigateBack = { navController.popBackStack() },
                onNavigateToManual = { sId, dStr ->
                    navController.navigate(Screen.ManualAttendance.createRoute(sId, dStr))
                }
            )
        }
        composable(NavigationRoutes.AUDIT_LOG) {
            com.azuratech.azuratime.features.reporting.ui.audit.AuditLogScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
