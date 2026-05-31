package com.azuratech.azuratime.features.dashboard.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.ui.theme.AzuraTheme
import com.azuratech.azuratime.core.util.isAdmin
import com.azuratech.azuratime.features.account.data.local.toDomain
import com.azuratech.azuratime.features.dashboard.ui.components.*
import com.azuratech.azuratime.features.school.ui.list.AddSchoolDialog
import com.azuratech.azuratime.features.school.ui.list.SchoolViewModel

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel(),
    schoolViewModel: SchoolViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val uiEffect by viewModel.uiEffectFlow.collectAsStateWithLifecycle(initialValue = null)
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddSchoolDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiEffect) {
        when (val effect = uiEffect) {
            is DashboardUiEffect.NavigateTo -> navController.navigate(effect.route)
            is DashboardUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            is DashboardUiEffect.ShowToast -> { /* Handle Toast if needed */ }
            null -> {}
        }
    }

    AzuraScreen(
        title = "Azura Time",
        snackbarHost = { SnackbarHost(snackbarHostState) },
        actions = {
            IconButton(onClick = { viewModel.onEvent(DashboardUiEvent.Logout) }) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                DashboardContent(
                    navController = navController,
                    data = uiState,
                    schoolViewModel = schoolViewModel,
                    availableClasses = schoolViewModel.uiStateFlow.collectAsStateWithLifecycle().value.availableClasses,
                    snackbarHostState = snackbarHostState,
                    showAddSchoolDialog = showAddSchoolDialog,
                    onAddSchoolClick = { showAddSchoolDialog = true },
                    onDismissAddSchool = { showAddSchoolDialog = false },
                    onSyncClick = { viewModel.onEvent(DashboardUiEvent.Refresh) },
                    onRegisterStudentClick = { viewModel.onEvent(DashboardUiEvent.OnRegisterStudentClick) },
                    onSelectClass = { classId ->
                        viewModel.onEvent(DashboardUiEvent.SelectActiveClass(classId))
                    },
                    onAttendanceClick = { classId ->
                        viewModel.onEvent(DashboardUiEvent.SelectActiveClass(classId, Screen.AttendanceCapture.route))
                    },
                    onRosterClick = { classId ->
                        viewModel.onEvent(DashboardUiEvent.SelectActiveClass(classId, Screen.StudentRoster.route))
                    },
                    onLogoutClick = {
                        viewModel.onEvent(DashboardUiEvent.Logout)
                    },
                )
            }
        }
    }
}

@Composable
fun DashboardContent(
    navController: NavController,
    data: DashboardUiState,
    schoolViewModel: SchoolViewModel,
    availableClasses: List<com.azuratech.azuraengine.model.ClassModel>,
    snackbarHostState: SnackbarHostState,
    showAddSchoolDialog: Boolean,
    onAddSchoolClick: () -> Unit,
    onDismissAddSchool: () -> Unit,
    onSyncClick: () -> Unit,
    onRegisterStudentClick: () -> Unit,
    onSelectClass: (String?) -> Unit,
    onAttendanceClick: (String) -> Unit,
    onRosterClick: (String) -> Unit,
    onLogoutClick: () -> Unit,
) {
    val schoolUiState by schoolViewModel.uiStateFlow.collectAsStateWithLifecycle()
    val schools = schoolUiState.schools
    val activeSchoolId = schoolUiState.activeSchoolId
    val account = data.account

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
    ) {
        // --- 👤 PROFILE & WORKSPACE ---
        item {
            ProfileHeader(
                name = account?.name ?: "Guest",
                email = account?.email,
                schoolName = data.currentSchool?.name,
                photoUrl = account?.photoUrl,
                onLogout = onLogoutClick,
                onProfileClick = { navController.navigate(Screen.Profile.route) },
            )
        }

        item {
            MySchoolsCard(
                viewModel = schoolViewModel,
                accountId = account?.accountId ?: "",
                isApproved = data.isApproved,
                globalRole = account?.role ?: "USER",
                onSchoolClick = { school ->
                    schoolViewModel.onEvent(com.azuratech.azuratime.features.school.ui.list.SchoolUiEvent.SelectSchool(school))
                },
                onAddSchoolClick = onAddSchoolClick,
                onJoinSchoolClick = { navController.navigate(Screen.FindSchool.route) },
            )
        }

        // --- 🚀 QUICK ACTIONS & STATS ---
        if (activeSchoolId != null) {
            item {
                SyncStatusCard(
                    isSyncing = data.isSyncing,
                    lastSync = "Baru saja",
                    onSyncClick = onSyncClick,
                )
            }

            // Visible Version Badge for testing updates
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AssistChip(
                        onClick = {},
                        label = { Text("v1.1.4", style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        ),
                    )
                }
            }

            if (!data.isApproved) {
                item {
                    PendingApprovalCard()
                }
            }

            if (data.isApproved) {
                item {
                    ActiveSessionCard(
                        allClasses = data.allClasses,
                        activeClassId = account?.activeClassId,
                        onSelectClass = onSelectClass,
                    )
                }

                if (data.sessionStudents.isNotEmpty()) {
                    item { SessionStudentsList(students = data.sessionStudents) }
                }

                item {
                    MyAssignedClassesSection(
                        myClasses = data.assignedClasses,
                        onNavigateToAll = { navController.navigate(Screen.MyAssignedClass.route) },
                        onAttendanceClick = onAttendanceClick,
                        onRosterClick = onRosterClick,
                    )
                }
            }

            if (data.isApproved) {
                val isAdmin = account?.toDomain().isAdmin(activeSchoolId ?: "")
                item {
                    AccountTasksGrid(
                        navController = navController,
                        isAdmin = isAdmin,
                        currentRole = data.currentRole,
                        onRegisterStudentClick = onRegisterStudentClick,
                        accountId = account?.accountId,
                    )
                }

                // AI Native Feature Section
                item {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = AzuraSpacing.md))
                    Text(
                        text = "AI Native Tools",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = AzuraSpacing.md),
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = AzuraSpacing.md),
                        onClick = { navController.navigate(Screen.AiMusic.route) },
                    ) {
                        Row(
                            modifier = Modifier.padding(AzuraSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(AzuraSpacing.md))
                            Column {
                                Text("Traditional Music AI", style = MaterialTheme.typography.titleMedium)
                                Text("Rekomendasi musik pengiring belajar", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(AzuraSpacing.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Silakan pilih sekolah untuk memulai",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showAddSchoolDialog) {
        AddSchoolDialog(
            availableClasses = availableClasses,
            onDismissRequest = onDismissAddSchool,
            onConfirmClick = { name, timezone, classes ->
                schoolViewModel.onEvent(com.azuratech.azuratime.features.school.ui.list.SchoolUiEvent.CreateSchool(name, timezone, classes))
                onDismissAddSchool()
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewDashboard() {
    AzuraTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Dashboard Content Preview")
        }
    }
}
