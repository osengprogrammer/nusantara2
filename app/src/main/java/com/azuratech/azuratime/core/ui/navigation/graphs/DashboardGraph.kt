package com.azuratech.azuratime.core.ui.navigation.graphs
import com.azuratech.azuratime.features.payment.ui.PaymentScreen

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.azuratech.azuratime.core.navigation.NavigationRoutes
import com.azuratech.azuratime.features.dashboard.ui.DashboardScreen
import com.azuratech.azuratime.features.aimusic.ui.AiMusicScreen

fun NavGraphBuilder.dashboardGraph(navController: NavController) {
    composable(NavigationRoutes.DASHBOARD) {
        val schoolVm: com.azuratech.azuratime.features.school.ui.list.SchoolViewModel = hiltViewModel()
        val schoolState by schoolVm.uiStateFlow.collectAsState()
        DashboardScreen(
            navController = navController,
            schools = schoolState.schools,
            isLoadingSchools = schoolState.isLoading,
            activeSchoolId = schoolState.activeSchoolId,
            availableClasses = schoolState.availableClasses,
            onRefreshSchools = { schoolVm.onEvent(com.azuratech.azuratime.features.school.ui.list.SchoolUiEvent.LoadSchools(schoolState.accountId)) },
            onSelectSchool = { school -> schoolVm.onEvent(com.azuratech.azuratime.features.school.ui.list.SchoolUiEvent.SelectSchool(school)) },
            onCreateSchool = { name, timezone, classes -> schoolVm.onEvent(com.azuratech.azuratime.features.school.ui.list.SchoolUiEvent.CreateSchool(name, timezone, classes)) },
        )
    }

    composable(NavigationRoutes.AI_MUSIC) {
        AiMusicScreen(onNavigateBack = { navController.popBackStack() })
    }

    // Payment feature navigation
    composable(NavigationRoutes.PAYMENT) {
        com.azuratech.azuratime.features.payment.ui.PaymentScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }
}
