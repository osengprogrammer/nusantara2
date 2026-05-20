package com.azuratech.azuratime.core.ui.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.core.navigation.NavigationRoutes
import com.azuratech.azuratime.features.account.ui.management.AccountManagementScreen
import com.azuratech.azuratime.features.account.ui.components.MyAssignedClassScreen
import com.azuratech.azuratime.features.account.ui.components.FollowingScreen

fun NavGraphBuilder.accountGraph(
    navController: androidx.navigation.NavController,
) {
    val uri = "azuratime://azuratech.com"

    navigation(
        startDestination = NavigationRoutes.ACCOUNT_PROFILE,
        route = NavigationRoutes.ACCOUNT_GRAPH,
    ) {
        composable(NavigationRoutes.ACCOUNT_PROFILE) {
            AccountManagementScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
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
                navArgument("targetAccountId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("schoolId") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
        ) { backStackEntry ->
            val targetAccountId = backStackEntry.arguments?.getString("targetAccountId")
            com.azuratech.azuratime.features.account.ui.components.MyAssignedClassScreen(
                targetAccountId = targetAccountId,
                onNavigateBack = { navController.popBackStack() },
                accountViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                classViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
            )
        }
        composable(NavigationRoutes.FOLLOWING) {
            com.azuratech.azuratime.features.account.ui.components.FollowingScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(NavigationRoutes.DEBUG) {
            com.azuratech.azuratime.features.account.ui.debug.DebugScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
