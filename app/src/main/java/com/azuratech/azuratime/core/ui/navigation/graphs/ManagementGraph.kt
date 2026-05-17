package com.azuratech.azuratime.core.ui.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.core.navigation.NavigationRoutes
import androidx.hilt.navigation.compose.hiltViewModel
import com.azuratech.azuratime.features.student.ui.RegistrationMenuScreen
import com.azuratech.azuratime.features.student.ui.form.AddStudentScreen
import com.azuratech.azuratime.features.student.ui.form.EditStudentScreen
import com.azuratech.azuratime.features.student.ui.bulk.BulkRegistrationScreen
import com.azuratech.azuratime.features.student.ui.roster.StudentRosterScreen
import com.azuratech.azuratime.features.student.ui.roster.StudentRosterBarcodeScreen
import com.azuratech.azuratime.features.biometric.ui.enroll.BiometricScreen
import com.azuratech.azuratime.features.school.ui.classes.ClassManagementScreen
import com.azuratech.azuratime.features.school.ui.classes.ClassDetailScreen
import com.azuratech.azuratime.features.school.ui.admin.PendingSchoolsScreen
import com.azuratech.azuratime.features.reporting.ui.integrity.DataIntegrityScreen
import com.azuratech.azuratime.features.reporting.ui.integrity.DataManagementScreen

fun NavGraphBuilder.managementGraph(
    navController: NavController,
) {
    val uri = "azuratime://azuratech.com"

    navigation(
        startDestination = NavigationRoutes.REGISTRATION_MENU,
        route = NavigationRoutes.MANAGEMENT_GRAPH,
    ) {
        composable(NavigationRoutes.REGISTRATION_MENU) {
            RegistrationMenuScreen(
                onNavigateToAddStudent = { navController.navigate(NavigationRoutes.ADD_STUDENT) },
                onNavigateToBulkRegister = { navController.navigate(NavigationRoutes.BULK_REGISTER) },
                onNavigateToBiometricManagement = { navController.navigate(NavigationRoutes.BIOMETRIC_MANAGEMENT) },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(NavigationRoutes.ADD_STUDENT) {
            AddStudentScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(NavigationRoutes.BULK_REGISTER) {
            BulkRegistrationScreen(
                onNavigateBack = { navController.popBackStack() },
                bulkViewModel = hiltViewModel(),
            )
        }
        composable(NavigationRoutes.BIOMETRIC_MANAGEMENT) {
            BiometricScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(NavigationRoutes.STUDENT_ROSTER) {
            StudentRosterScreen(
                onEditStudentClick = { studentId -> navController.navigate(Screen.EditStudent.createRoute(studentId)) },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(NavigationRoutes.STUDENT_ROSTER_BARCODE) {
            StudentRosterBarcodeScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(NavigationRoutes.DATA_DASHBOARD) {
            DataIntegrityScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(NavigationRoutes.DATA_MANAGEMENT) {
            DataManagementScreen(
                initialDataType = it.arguments?.getString("dataType") ?: "FACES",
                onNavigateBack = { navController.popBackStack() },
                onNavigateToClassList = { navController.navigate(NavigationRoutes.STUDENT_ROSTER) },
                registerViewModel = hiltViewModel(),
            )
        }
        composable(
            route = NavigationRoutes.EDIT_STUDENT,
            arguments = listOf(navArgument("studentId") { type = NavType.StringType }),
        ) { entry ->
            EditStudentScreen(
                faceId = entry.arguments?.getString("studentId") ?: "",
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(
            route = NavigationRoutes.CLASS_LIST,
            arguments = listOf(navArgument("schoolId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "$uri/classes/{schoolId}" }),
        ) {
            ClassManagementScreen(
                onNavigateBack = { navController.popBackStack() },
                onClassSelected = { id, name ->
                    navController.navigate(Screen.ClassDetail.createRoute(id, name))
                },
            )
        }
        composable(
            route = NavigationRoutes.CLASS_MANAGEMENT,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType }),
        ) {
            ClassManagementScreen(
                onNavigateBack = { navController.popBackStack() },
                onClassSelected = { id, name ->
                    navController.navigate(Screen.ClassDetail.createRoute(id, name))
                },
            )
        }
        composable(
            route = NavigationRoutes.CLASS_DETAIL,
            arguments = listOf(
                navArgument("classId") { type = NavType.StringType },
                navArgument("className") { type = NavType.StringType },
            ),
            deepLinks = listOf(navDeepLink { uriPattern = "$uri/class_detail/{classId}/{className}" }),
        ) { entry ->
            ClassDetailScreen(
                classId = entry.arguments?.getString("classId") ?: "",
                className = entry.arguments?.getString("className") ?: "",
                classViewModel = hiltViewModel(),
                biometricViewModel = hiltViewModel<com.azuratech.azuratime.features.biometric.ui.enroll.BiometricEnrollmentViewModel>(),
                onNavigateBack = { navController.popBackStack() },
                onAddStudent = { navController.navigate(NavigationRoutes.STUDENT_ROSTER) },
            )
        }

        composable(NavigationRoutes.PENDING_SCHOOLS) {
            PendingSchoolsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
