package com.azuratech.azuratime.ui.core.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.azuratech.azuratime.core.navigation.Screen

fun NavGraphBuilder.userGraph(
    navController: androidx.navigation.NavController
) {
    navigation(
        startDestination = Screen.Profile.route,
        route = "user_graph"
    ) {
        composable(Screen.Profile.route) {
            com.azuratech.azuratime.ui.user.UserProfileScreen(
                userViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                workspaceViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.MyAssignedClass.route) {
            com.azuratech.azuratime.ui.user.MyAssignedClassScreen(
                targetUserId = null,
                onNavigateBack = { navController.popBackStack() },
                userViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                classViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            )
        }
        composable(Screen.Network.route) {
            com.azuratech.azuratime.ui.user.NetworkScreen(
                navController = navController,
                networkViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                userViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            )
        }
        composable(Screen.Debug.route) {
            com.azuratech.azuratime.ui.debug.DebugScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
