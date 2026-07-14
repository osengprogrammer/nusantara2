package com.azuratech.azuratime.core.ui.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.core.navigation.NavigationRoutes
import com.azuratech.azuratime.features.account.ui.management.AccountManagementScreen
import com.azuratech.azuratime.features.account.ui.management.AssignClassScreen
import com.azuratech.azuratime.features.account.ui.management.BulkAssignMatrixScreen
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
                onNavigateToWelcome = {
                    // 🔥 AI Native: BootViewModel will handle root UI transition.
                    // No need to navigate local navController to auth routes.
                    android.util.Log.d("LogoutNav", "Logout triggered, awaiting root state transition...")
                },
            )
        }
        composable(NavigationRoutes.SUPERVISORS) {
            AccountManagementScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWelcome = {
                    android.util.Log.d("LogoutNav", "Logout triggered, awaiting root state transition...")
                },
                title = "Staff & Supervisors",
                onNavigateToBulkAssign = {
                    navController.navigate(Screen.BulkAssignMatrix.route)
                },
                onNavigateToAssignClass = { targetId, role ->
                    navController.navigate(Screen.AssignClass.createRoute(targetId, role))
                },
            )
        }
        composable(NavigationRoutes.BULK_ASSIGN_MATRIX) {
            BulkAssignMatrixScreen(
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
                    val route = Screen.SchoolExplorer.createRoute(schoolId)
                    android.util.Log.d("SchoolClick", "Navigating to: $route")
                    try {
                        navController.navigate(route)
                    } catch (e: Exception) {
                        android.util.Log.e("SchoolClick", "Navigation failed: ${e.message}", e)
                    }
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
            val classVm: com.azuratech.azuratime.features.school.ui.classes.ClassViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val classState by classVm.uiStateFlow.collectAsState()
            com.azuratech.azuratime.features.account.ui.components.MyAssignedClassScreen(
                targetAccountId = targetAccountId,
                onNavigateBack = { navController.popBackStack() },
                accountViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                allClasses = classState.classes,
            )
        }
        composable(
            route = NavigationRoutes.ASSIGN_CLASS,
            arguments = listOf(
                navArgument("targetAccountId") { type = NavType.StringType },
                navArgument("role") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val targetAccountId = backStackEntry.arguments?.getString("targetAccountId") ?: ""
            val roleStr = backStackEntry.arguments?.getString("role") ?: "USER"
            AssignClassScreen(
                targetAccountId = targetAccountId,
                accountRole = com.azuratech.azuratime.core.domain.model.AccountRole.fromString(roleStr),
                onNavigateBack = { navController.popBackStack() },
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
            )
        }
        composable(NavigationRoutes.FOLLOWING) {
            com.azuratech.azuratime.features.account.ui.components.FollowingScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAssignClass = { targetId, role ->
                    navController.navigate(Screen.AssignClass.createRoute(targetId, role))
                },
            )
        }
        composable(NavigationRoutes.DEBUG) {
            com.azuratech.azuratime.features.account.ui.debug.DebugScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
