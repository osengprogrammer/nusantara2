package com.azuratech.azuratime.core.ui.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.core.navigation.NavigationRoutes
import com.azuratech.azuratime.features.account.ui.profile.AccountProfileScreen
import com.azuratech.azuratime.features.account.ui.components.MyAssignedClassScreen
import com.azuratech.azuratime.features.account.ui.components.NetworkScreen

fun NavGraphBuilder.accountGraph(
    navController: androidx.navigation.NavController,
) {
    val uri = "azuratime://azuratech.com"

    navigation(
        startDestination = NavigationRoutes.ACCOUNT_PROFILE,
        route = NavigationRoutes.ACCOUNT_GRAPH,
    ) {
        composable(NavigationRoutes.ACCOUNT_PROFILE) {
            AccountProfileScreen(
                userViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                workspaceViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(
            route = NavigationRoutes.SCHOOL_LIST,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType }),
            deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "$uri/schools/{accountId}" }),
        ) {
            com.azuratech.azuratime.features.school.ui.list.SchoolListScreen(
                onNavigateBack = { navController.popBackStack() },
                onSchoolClick = { schoolId ->
                    navController.navigate(Screen.ClassList.createRoute(schoolId))
                },
            )
        }
        composable(
            route = NavigationRoutes.MY_ASSIGNED_CLASSES,
            arguments = listOf(
                navArgument("targetUserId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("schoolId") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
        ) { backStackEntry ->
            val targetUserId = backStackEntry.arguments?.getString("targetUserId")
            com.azuratech.azuratime.features.account.ui.components.MyAssignedClassScreen(
                targetUserId = targetUserId,
                onNavigateBack = { navController.popBackStack() },
                userViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                classViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
            )
        }
        composable(NavigationRoutes.NETWORK) {
            com.azuratech.azuratime.features.account.ui.components.NetworkScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(NavigationRoutes.DEBUG) {
            com.azuratech.azuratime.features.account.ui.debug.DebugScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
