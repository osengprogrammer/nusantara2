package com.azuratech.azuratime.ui.core.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.azuratech.azuratime.core.navigation.Screen

fun NavGraphBuilder.userGraph(
    navController: androidx.navigation.NavController
) {
    val uri = "azuratime://azuratech.com"

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
        composable(
            route = Screen.SchoolList.route,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType }),
            deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "$uri/schools/{accountId}" })
        ) { 
            com.azuratech.azuratime.ui.school.SchoolListScreen(
                onNavigateBack = { navController.popBackStack() },
                onSchoolClick = { schoolId ->
                    navController.navigate(Screen.ClassList.createRoute(schoolId))
                }
            )
        }
        composable(
            route = Screen.MyAssignedClass.route,
            arguments = listOf(
                navArgument("targetUserId") { type = NavType.StringType; nullable = true },
                navArgument("schoolId") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val targetUserId = backStackEntry.arguments?.getString("targetUserId")
            com.azuratech.azuratime.ui.user.MyAssignedClassScreen(
                targetUserId = targetUserId,
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
