package com.azuratech.azuratime.ui.core.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import com.azuratech.azuratime.core.navigation.Screen
import androidx.hilt.navigation.compose.hiltViewModel
import com.azuratech.azuratime.ui.add.*
import com.azuratech.azuratime.ui.classes.*
import com.azuratech.azuratime.ui.admin.PendingSchoolsScreen

fun NavGraphBuilder.managementGraph(
    navController: NavController
) {
    val uri = "azuratime://azuratech.com"

    navigation(
        startDestination = Screen.RegistrationMenu.route,
        route = "management_graph"
    ) {
        composable(Screen.RegistrationMenu.route) {
            RegistrationMenuScreen(
                onNavigateToAddUser = { navController.navigate(Screen.AddUser.route) },
                onNavigateToBulkRegister = { navController.navigate(Screen.BulkRegister.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddUser.route) {
            AddUserScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.BulkRegister.route) {
            BulkRegistrationScreen(
                onNavigateBack = { navController.popBackStack() },
                bulkViewModel = hiltViewModel()
            )
        }
        composable(Screen.Manage.route) {
            FaceListScreen(
                onEditUser = { id -> navController.navigate(Screen.EditUser.createRoute(id)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.FaceListBarcode.route) {
            FaceListBarcodeScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.EditUser.route,
            arguments = listOf(navArgument("faceId") { type = NavType.StringType })
        ) { entry ->
            EditUserScreen(
                faceId = entry.arguments?.getString("faceId") ?: "",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.ClassList.route,
            arguments = listOf(navArgument("schoolId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "$uri/classes/{schoolId}" })
        ) { 
            ClassManagementScreen(
                onNavigateBack = { navController.popBackStack() },
                onClassClick = { id, name ->
                    navController.navigate(Screen.ClassDetail.createRoute(id, name))
                }
            )
        }
        composable(
            route = Screen.ClassManagement.route,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType })
        ) {
            ClassManagementScreen(
                onNavigateBack = { navController.popBackStack() },
                onClassClick = { id, name ->
                    navController.navigate(Screen.ClassDetail.createRoute(id, name))
                }
            )
        }
        composable(
            route = Screen.ClassDetail.route,
            arguments = listOf(
                navArgument("classId") { type = NavType.StringType },
                navArgument("className") { type = NavType.StringType }
            ),
            deepLinks = listOf(navDeepLink { uriPattern = "$uri/class_detail/{classId}/{className}" })
        ) { entry ->
            ClassDetailScreen(
                classId = entry.arguments?.getString("classId") ?: "",
                className = entry.arguments?.getString("className") ?: "",
                classViewModel = hiltViewModel(),
                faceViewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onAddStudent = { navController.navigate(Screen.Manage.route) }
            )
        }

        composable(Screen.PendingSchools.route) {
            PendingSchoolsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
