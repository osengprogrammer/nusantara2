package com.azuratech.azuratime.ui.core.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.navigation.NavigationRoutes
import androidx.hilt.navigation.compose.hiltViewModel
import com.azuratech.azuratime.ui.add.*
import com.azuratech.azuratime.ui.biometric.BiometricScreen
import com.azuratech.azuratime.ui.classes.*
import com.azuratech.azuratime.ui.admin.PendingSchoolsScreen
import com.azuratech.azuratime.ui.data.DataIntegrityScreen
import com.azuratech.azuratime.ui.data.DataManagementScreen

fun NavGraphBuilder.managementGraph(
    navController: NavController
) {
    val uri = "azuratime://azuratech.com"

    navigation(
        startDestination = NavigationRoutes.REGISTRATION_MENU,
        route = NavigationRoutes.MANAGEMENT_GRAPH
    ) {
        composable(NavigationRoutes.REGISTRATION_MENU) {
            RegistrationMenuScreen(
                onNavigateToAddUser = { navController.navigate(NavigationRoutes.ADD_USER) },
                onNavigateToBulkRegister = { navController.navigate(NavigationRoutes.BULK_REGISTER) },
                onNavigateToBiometricManagement = { navController.navigate(NavigationRoutes.BIOMETRIC_MANAGEMENT) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(NavigationRoutes.ADD_USER) {
            AddUserScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(NavigationRoutes.BULK_REGISTER) {
            BulkRegistrationScreen(
                onNavigateBack = { navController.popBackStack() },
                bulkViewModel = hiltViewModel()
            )
        }
        composable(NavigationRoutes.BIOMETRIC_MANAGEMENT) {
            BiometricScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(NavigationRoutes.MANAGE_FACES) {
            FaceListScreen(
                onEditUser = { faceId -> navController.navigate(Screen.EditUser.createRoute(faceId)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(NavigationRoutes.DATA_DASHBOARD) {
            DataIntegrityScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(NavigationRoutes.DATA_MANAGEMENT) {
            DataManagementScreen(
                initialDataType = it.arguments?.getString("dataType") ?: "FACES",
                onNavigateBack = { navController.popBackStack() },
                onNavigateToClassList = { navController.navigate(NavigationRoutes.MANAGE_FACES) },
                registerViewModel = hiltViewModel()
            )
        }
        composable(
            route = NavigationRoutes.EDIT_USER,
            arguments = listOf(navArgument("faceId") { type = NavType.StringType })
        ) { entry ->
            EditUserScreen(
                faceId = entry.arguments?.getString("faceId") ?: "",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = NavigationRoutes.CLASS_LIST,
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
            route = NavigationRoutes.CLASS_MANAGEMENT,
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
            route = NavigationRoutes.CLASS_DETAIL,
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
                onAddStudent = { navController.navigate(NavigationRoutes.MANAGE_FACES) }
            )
        }

        composable(NavigationRoutes.PENDING_SCHOOLS) {
            PendingSchoolsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
