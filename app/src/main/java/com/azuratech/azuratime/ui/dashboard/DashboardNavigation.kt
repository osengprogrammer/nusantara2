package com.azuratech.azuratime.ui.dashboard

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.azuratech.azuratime.core.navigation.Screen

fun NavGraphBuilder.dashboardGraph(
    navController: androidx.navigation.NavController
) {
    navigation(
        startDestination = Screen.Dashboard.route,
        route = "dashboard_graph"
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController)
        }
    }
}
