package com.azuratech.azuratime.ui.core.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.azuratech.azuratime.core.navigation.Screen

fun NavGraphBuilder.managementGraph(
    navController: androidx.navigation.NavController
) {
    navigation(
        startDestination = Screen.RegistrationMenu.route,
        route = "management_graph"
    ) {
        composable(Screen.RegistrationMenu.route) {
            com.azuratech.azuratime.ui.add.RegistrationMenuScreen(
                onNavigateToAddUser = { navController.navigate(Screen.AddUser.route) },
                onNavigateToBulkRegister = { navController.navigate(Screen.BulkRegister.route) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddUser.route) {
            com.azuratech.azuratime.ui.add.AddUserScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.BulkRegister.route) {
            com.azuratech.azuratime.ui.add.BulkRegistrationScreen(
                onNavigateBack = { navController.popBackStack() },
                bulkViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            )
        }
        composable(Screen.Manage.route) {
            com.azuratech.azuratime.ui.add.FaceListScreen(
                onEditUser = { id -> navController.navigate(Screen.EditUser.createRoute(id)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.FaceListBarcode.route) {
            com.azuratech.azuratime.ui.add.FaceListBarcodeScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.EditUser.route,
            arguments = listOf(androidx.navigation.navArgument("faceId") { androidx.navigation.NavType.StringType })
        ) { entry ->
            com.azuratech.azuratime.ui.add.EditUserScreen(
                faceId = entry.arguments?.getString("faceId") ?: "",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ClassList.route) {
            com.azuratech.azuratime.ui.classes.ClassManagementScreen(
                viewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onClassClick = { id, name ->
                    navController.navigate(Screen.ClassDetail.createRoute(id, name))
                }
            )
        }
        composable(
            route = Screen.ClassDetail.route,
            arguments = listOf(
                androidx.navigation.navArgument("id") { androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("name") { androidx.navigation.NavType.StringType }
            )
        ) { entry ->
            com.azuratech.azuratime.ui.classes.ClassDetailScreen(
                classId = entry.arguments?.getString("id") ?: "",
                className = entry.arguments?.getString("name") ?: "",
                classViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                faceViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                onBack = { navController.popBackStack() },
                onAddStudent = { navController.navigate(Screen.Manage.route) }
            )
        }
    }
}
