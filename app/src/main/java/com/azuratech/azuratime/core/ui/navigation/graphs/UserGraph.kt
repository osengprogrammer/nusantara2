package com.azuratech.azuratime.core.ui.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.core.navigation.NavigationRoutes
import com.azuratech.azuratime.features.staff.ui.profile.UserProfileScreen
import com.azuratech.azuratime.features.staff.ui.components.MyAssignedClassScreen
import com.azuratech.azuratime.features.staff.ui.components.NetworkScreen

fun NavGraphBuilder.userGraph(
    navController: androidx.navigation.NavController
) {
    val uri = "azuratime://azuratech.com"

    navigation(
        startDestination = NavigationRoutes.USER_PROFILE,
        route = NavigationRoutes.USER_GRAPH
    ) {
        composable(NavigationRoutes.USER_PROFILE) {
            UserProfileScreen(
                userViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                workspaceViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = NavigationRoutes.SCHOOL_LIST,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType }),
            deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "$uri/schools/{accountId}" })
        ) { 
            com.azuratech.azuratime.features.school.ui.list.SchoolListScreen(
                onNavigateBack = { navController.popBackStack() },
                onSchoolClick = { schoolId ->
                    navController.navigate(Screen.ClassList.createRoute(schoolId))
                }
            )
        }
        composable(
            route = NavigationRoutes.MY_ASSIGNED_CLASSES,
            arguments = listOf(
                navArgument("targetUserId") { type = NavType.StringType; nullable = true },
                navArgument("schoolId") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val targetUserId = backStackEntry.arguments?.getString("targetUserId")
            com.azuratech.azuratime.features.staff.ui.components.MyAssignedClassScreen(
                targetUserId = targetUserId,
                onNavigateBack = { navController.popBackStack() },
                userViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                classViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            )
        }
        composable(NavigationRoutes.NETWORK) {
            com.azuratech.azuratime.features.staff.ui.components.NetworkScreen(
                navController = navController,
                networkViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                userViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            )
        }
        composable(NavigationRoutes.DEBUG) {
            com.azuratech.azuratime.features.staff.ui.debug.DebugScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
