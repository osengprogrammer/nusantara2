package com.azuratech.azuratime.core.ui.navigation.graphs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.azuratech.azuratime.features.biometric.ui.assignment.StudentAssignmentScreen
import com.azuratech.azuratime.features.school.ui.classes.ClassManagementScreen
import com.azuratech.azuratime.features.school.ui.classes.ClassDetailScreen
import com.azuratech.azuratime.features.school.ui.admin.PendingSchoolsScreen
import com.azuratech.azuratime.features.reporting.ui.integrity.DataIntegrityScreen
import com.azuratech.azuratime.features.reporting.ui.integrity.DataManagementScreen
import com.azuratech.azuratime.features.school.ui.geofence.GpsManagementScreen
import com.azuratech.azuratime.features.school.ui.geofence.MapPickerScreen
import com.azuratech.azuratime.features.school.ui.list.SchoolViewModel
import com.azuratech.azuratime.features.session.ui.SessionManagementViewModel
import com.azuratech.azuratime.features.session.ui.SessionManagementUiEvent
import com.azuratech.azuratime.features.account.ui.management.AccountManagementViewModel
import com.azuratech.azuratime.features.student.ui.StudentViewModel

import com.azuratech.azuratime.features.school.ui.explorer.SchoolExplorerScreen

fun NavGraphBuilder.managementGraph(
    navController: NavController,
) {
    val uri = "azuratime://azuratech.com"

    navigation(
        startDestination = NavigationRoutes.REGISTRATION_MENU,
        route = NavigationRoutes.MANAGEMENT_GRAPH,
    ) {
        composable(NavigationRoutes.GPS_MANAGEMENT) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(NavigationRoutes.MANAGEMENT_GRAPH)
            }
            val schoolViewModel: SchoolViewModel = hiltViewModel(parentEntry)
            GpsManagementScreen(
                viewModel = schoolViewModel,
                onNavigateToMapPicker = { navController.navigate(NavigationRoutes.MAP_PICKER) },
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(NavigationRoutes.MAP_PICKER) { backStackEntry ->
            // Use parent graph to share the same ViewModel instance
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(NavigationRoutes.MANAGEMENT_GRAPH)
            }
            val schoolViewModel: SchoolViewModel = hiltViewModel(parentEntry)
            MapPickerScreen(
                viewModel = schoolViewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(NavigationRoutes.REGISTRATION_MENU) {
            RegistrationMenuScreen(
                onNavigateToAddStudent = { navController.navigate(NavigationRoutes.ADD_STUDENT) },
                onNavigateToBulkRegister = { navController.navigate(NavigationRoutes.BULK_REGISTER) },
                onNavigateToBiometricManagement = { navController.navigate(NavigationRoutes.BIOMETRIC_MANAGEMENT) },
                onNavigateToAssignment = { navController.navigate(NavigationRoutes.STUDENT_ASSIGNMENT) },
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
        composable(NavigationRoutes.STUDENT_ASSIGNMENT) {
            StudentAssignmentScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(NavigationRoutes.STUDENT_ROSTER) {
            StudentRosterScreen(
                onNavigateToWallet = { },
                onNavigateToHistory = { },
                onNavigateToDeduct = { },
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
        ) { entry ->
            val schoolId = entry.arguments?.getString("schoolId") ?: ""
            val sessionVm: SessionManagementViewModel = hiltViewModel()
            val accountVm: AccountManagementViewModel = hiltViewModel()
            val studentVm: StudentViewModel = hiltViewModel()
            val sessionState by sessionVm.uiStateFlow.collectAsState()
            val accountState by accountVm.uiStateFlow.collectAsState()
            val studentState by studentVm.uiStateFlow.collectAsState()
            SchoolExplorerScreen(
                schoolId = schoolId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddStudent = { navController.navigate(NavigationRoutes.ADD_STUDENT) },
                navController = navController,
                subjects = sessionState.subjects,
                availableSubjects = sessionState.availableSubjects,
                onDeleteSubject = { sessionVm.onEvent(SessionManagementUiEvent.DeleteSubject(it)) },
                onAddSubject = { name, desc -> sessionVm.onEvent(SessionManagementUiEvent.AddSubject(name, desc)) },
                allAccountsInSameSchool = accountState.allAccountsInSameSchool,
                activeSchoolId = accountState.activeSchoolId,
                students = studentState.students,
                isLoadingStudents = studentState.isLoading,
                onEditStudent = { profile -> studentVm.onEvent(com.azuratech.azuratime.features.student.ui.StudentUiEvent.OpenEditDialog(profile)) },
                onDeleteStudent = { studentId -> studentVm.onEvent(com.azuratech.azuratime.features.student.ui.StudentUiEvent.DeleteStudent(studentId)) },
            )
        }
        composable(
            route = NavigationRoutes.CLASS_MANAGEMENT,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType }),
        ) { entry ->
            @Suppress("UNUSED_VARIABLE")
            val accountId = entry.arguments?.getString("accountId") ?: ""
            ClassManagementScreen(
                onNavigateBack = { navController.popBackStack() },
                onClassClick = { classModel ->
                    navController.navigate(Screen.ClassDetail.createRoute(classModel.id, classModel.name))
                },
            )
        }
        composable(
            route = NavigationRoutes.SCHOOL_EXPLORER,
            arguments = listOf(navArgument("schoolId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val schoolId = backStackEntry.arguments?.getString("schoolId") ?: ""
            val sessionVm: SessionManagementViewModel = hiltViewModel()
            val accountVm: AccountManagementViewModel = hiltViewModel()
            val studentVm: StudentViewModel = hiltViewModel()
            val sessionState by sessionVm.uiStateFlow.collectAsState()
            val accountState by accountVm.uiStateFlow.collectAsState()
            val studentState by studentVm.uiStateFlow.collectAsState()
            SchoolExplorerScreen(
                schoolId = schoolId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddStudent = { navController.navigate(NavigationRoutes.ADD_STUDENT) },
                navController = navController,
                subjects = sessionState.subjects,
                availableSubjects = sessionState.availableSubjects,
                onDeleteSubject = { sessionVm.onEvent(SessionManagementUiEvent.DeleteSubject(it)) },
                onAddSubject = { name, desc -> sessionVm.onEvent(SessionManagementUiEvent.AddSubject(name, desc)) },
                allAccountsInSameSchool = accountState.allAccountsInSameSchool,
                activeSchoolId = accountState.activeSchoolId,
                students = studentState.students,
                isLoadingStudents = studentState.isLoading,
                onEditStudent = { profile -> studentVm.onEvent(com.azuratech.azuratime.features.student.ui.StudentUiEvent.OpenEditDialog(profile)) },
                onDeleteStudent = { studentId -> studentVm.onEvent(com.azuratech.azuratime.features.student.ui.StudentUiEvent.DeleteStudent(studentId)) },
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
            val classId = entry.arguments?.getString("classId")
            val className = entry.arguments?.getString("className")

            if (classId != null && className != null) {
                val sessionVm: SessionManagementViewModel = hiltViewModel()
                val sessionState by sessionVm.uiStateFlow.collectAsState()
                ClassDetailScreen(
                    classId = classId,
                    className = className,
                    classViewModel = hiltViewModel<com.azuratech.azuratime.features.school.ui.classes.ClassViewModel>(),
                    sessions = sessionState.sessions,
                    subjects = sessionState.subjects,
                    classes = sessionState.availableClasses,
                    assignments = sessionState.assignments,
                    selectedTier = sessionState.selectedTier,
                    onDeleteSession = { sessionVm.onEvent(SessionManagementUiEvent.DeleteSession(it)) },
                    onSelectTier = { sessionVm.onEvent(SessionManagementUiEvent.SelectTier(it)) },
                    onAddSession = { clsId, subjectId, tier, day, start, end ->
                        sessionVm.onEvent(
                            SessionManagementUiEvent.AddSession(
                                classId = clsId ?: "",
                                subjectId = subjectId,
                                sessionType = tier,
                                dayOfWeek = day,
                                startTime = start,
                                endTime = end,
                            ),
                        )
                    },
                    onNavigateBack = { navController.popBackStack() },
                    onAddStudent = { navController.navigate(NavigationRoutes.STUDENT_ROSTER) },
                )
            } else {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }

        composable(NavigationRoutes.PENDING_SCHOOLS) {
            PendingSchoolsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(NavigationRoutes.SCHOOL_TEMPLATES) {
            com.azuratech.azuratime.features.template.ui.TemplateDashboardScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
