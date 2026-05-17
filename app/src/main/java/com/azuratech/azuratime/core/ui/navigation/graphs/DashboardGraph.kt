package com.azuratech.azuratime.core.ui.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.azuratech.azuratime.core.navigation.NavigationRoutes
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.features.dashboard.ui.DashboardScreen

fun NavGraphBuilder.dashboardGraph(navController: NavController) {
    composable(NavigationRoutes.DASHBOARD) {
        DashboardScreen(navController = navController)
    }
}
