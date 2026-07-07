package com.azuratech.azuratime.core.ui.navigation.graphs
import com.azuratech.azuratime.features.payment.ui.PaymentScreen

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.azuratech.azuratime.core.navigation.NavigationRoutes
import com.azuratech.azuratime.features.dashboard.ui.DashboardScreen
import com.azuratech.azuratime.features.aimusic.ui.AiMusicScreen

fun NavGraphBuilder.dashboardGraph(navController: NavController) {
    composable(NavigationRoutes.DASHBOARD) {
        DashboardScreen(navController = navController)
    }

    composable(NavigationRoutes.AI_MUSIC) {
        AiMusicScreen(onNavigateBack = { navController.popBackStack() })
    }

    // Payment feature navigation
    composable(NavigationRoutes.PAYMENT) {
        com.azuratech.azuratime.features.payment.ui.PaymentScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
