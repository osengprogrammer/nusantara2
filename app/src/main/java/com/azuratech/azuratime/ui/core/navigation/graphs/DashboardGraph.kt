package com.azuratech.azuratime.ui.core.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.ui.dashboard.DashboardScreen

fun NavGraphBuilder.dashboardGraph(navController: NavController) {
    composable(Screen.Dashboard.route) {
        DashboardScreen(navController = navController)
    }
}
