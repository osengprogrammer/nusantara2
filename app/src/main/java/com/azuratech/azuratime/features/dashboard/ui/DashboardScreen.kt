package com.azuratech.azuratime.features.dashboard.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.azuratech.azuratime.core.util.showToast
import com.azuratech.azuratime.MainActivity
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.core.ui.designsystem.ConflictResolverDialog
import com.azuratech.azuratime.core.ui.UiEvent
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.designsystem.WorkspaceSelector
import com.azuratech.azuratime.features.school.ui.list.AddSchoolDialog
import com.azuratech.azuratime.features.dashboard.ui.components.*
import com.azuratech.azuratime.features.school.ui.list.SchoolViewModel
import com.azuratech.azuratime.features.reporting.ui.integrity.IntegritySummaryWidget
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.ui.theme.AzuraTheme
import com.google.firebase.auth.FirebaseAuth

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel(),
    schoolViewModel: SchoolViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val schoolUiState by schoolViewModel.uiStateFlow.collectAsStateWithLifecycle()
    val availableClasses = schoolUiState.availableClasses
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddSchoolDialog by remember { mutableStateOf(false) }

    val accountId = uiState.account?.accountId
    LaunchedEffect(accountId) {
        if (accountId != null) {
            schoolViewModel.onEvent(com.azuratech.azuratime.features.school.ui.list.SchoolUiEvent.LoadSchools(accountId))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is DashboardUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is DashboardUiEffect.ShowToast -> context.showToast(effect.message)
                is DashboardUiEffect.NavigateTo -> {
                    if (effect.route == "login") {
                        val intent = Intent(context, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    } else {
                        navController.navigate(effect.route)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        schoolViewModel.uiEventFlow.collect { event: UiEvent ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            !uiState.isReady -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                if (uiState.conflicts.isNotEmpty()) {
                    val firstConflict = uiState.conflicts.first()
                    ConflictResolverDialog(
                        conflict = firstConflict,
                        onResolveClick = { useCloud -> viewModel.onEvent(DashboardUiEvent.ResolveConflict(firstConflict, useCloud)) },
                    )
                }

                DashboardContent(
                    navController = navController,
                    data = uiState,
                    schoolViewModel = schoolViewModel,
                    availableClasses = availableClasses,
                    snackbarHostState = snackbarHostState,
                    showAddSchoolDialog = showAddSchoolDialog,
                    onAddSchoolClick = { showAddSchoolDialog = true },
                    onDismissAddSchool = { showAddSchoolDialog = false },
                    onSyncClick = { viewModel.onEvent(DashboardUiEvent.Refresh) },
                    onRegisterStudentClick = { viewModel.onEvent(DashboardUiEvent.OnRegisterStudentClick) },
                    onSelectClass = { classId ->
                        viewModel.onEvent(DashboardUiEvent.SelectActiveClass(classId))
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
    onLogoutClick: () -> Unit,
) {
    val schoolUiState by schoolViewModel.uiStateFlow.collectAsStateWithLifecycle()
    val schools = schoolUiState.schools
    val activeSchoolId = schoolUiState.activeSchoolId
    val activeSchool = schools.find { it.id == activeSchoolId }

    AzuraScreen(
        title = activeSchool?.name?.let { "Azura - $it" } ?: "Azura IMS",
        snackbarHostState = snackbarHostState,
        actions = {
            WorkspaceSelector(
                schoolViewModel = schoolViewModel,
                workspaceViewModel = hiltViewModel(),
            )
            DashboardSyncButton(
                isSyncing = data.isSyncing,
                onSyncClick = onSyncClick,
            )
        },
    ) {
        val account = data.account ?: return@AzuraScreen
        val photoUrl = FirebaseAuth.getInstance().currentUser?.photoUrl

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.lg),
            ) {
                if (!data.isApproved) {
                    item {
                        AzuraCard(
                            title = "Akses Dibatasi",
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        ) {
                            Text(
                                text = "Akun Anda sedang menunggu verifikasi Admin. Fitur scanner akan muncul setelah disetujui.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                item {
                    ProfileHeader(
                        name = account.name,
                        email = account.email,
                        schoolName = activeSchool?.name ?: account.schoolName ?: "",
                        photoUrl = photoUrl,
                        onLogout = onLogoutClick,
                        onProfileClick = { navController.navigate(Screen.Profile.route) },
                    )
                }

                item {
                    FollowingButton(
                        pendingRequests = data.pendingRequests,
                        onClick = { navController.navigate(Screen.Following.route) },
                    )
                }

                if (data.currentRole == "ADMIN" || data.currentRole == "SUPER_ADMIN") {
                    item {
                        Surface(
                            onClick = { navController.navigate(Screen.DataDashboard.route) },
                            color = Color.Transparent,
                            shape = AzuraShapes.medium,
                        ) {
                            IntegritySummaryWidget(
                                totalStudents = data.totalStudents,
                                unassignedCount = data.unassignedStudents,
                                brokenLinks = data.brokenAssignments,
                                unsyncedCount = data.unsyncedRecords,
                            )
                        }
                    }
                }

                // 👑 SUPER ADMIN MODERATION
                if (data.currentRole == "SUPER_ADMIN") {
                    item {
                        AzuraCard(
                            title = "Moderasi Sistem",
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Persetujuan Sekolah", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Lihat pendaftaran sekolah yang menunggu verifikasi.", style = MaterialTheme.typography.bodySmall)
                                }
                                Button(
                                    onClick = { navController.navigate(Screen.PendingSchools.route) },
                                    shape = AzuraShapes.medium,
                                ) {
                                    Text("Buka")
                                }
                            }
                        }
                    }
                }

                if (data.isSyncing) {
                    item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp)) }
                }

                // 🏫 School Management Section (Exempt from isApproved for first-time creation)
                val showSchoolCard = schools.isEmpty() || data.isApproved || data.currentRole == "ADMIN"
                if (showSchoolCard) {
                    item {
                        MySchoolsCard(
                            viewModel = schoolViewModel,
                            accountId = account.accountId,
                            isApproved = data.isApproved,
                            globalRole = data.currentRole,
                            onSchoolClick = { clickedSchoolId ->
                                val school = schools.find { it.id == clickedSchoolId }
                                if (school != null) {
                                    schoolViewModel.onEvent(com.azuratech.azuratime.features.school.ui.list.SchoolUiEvent.SelectSchool(school))
                                }
                            },
                            onAddSchoolClick = {
                                onAddSchoolClick()
                            },
                        )
                    }
                }

                if (data.isApproved) {
                    item {
                        ActiveSessionCard(
                            allClasses = data.allClasses,
                            activeClassId = account.activeClassId,
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
                        )
                    }
                }

                item {
                    AccountTasksGrid(
                        navController = navController,
                        isAdmin = data.currentRole == "ADMIN" || data.currentRole == "SUPER_ADMIN",
                        currentRole = data.currentRole,
                        onRegisterStudentClick = onRegisterStudentClick,
                        accountId = account.accountId,
                        isEnabled = activeSchool?.status == "ACTIVE",
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    RecentScansHeader(navController = navController)
                }

                items(data.recentRecords) { record ->
                    DashboardAttendanceItem(record)
                }
            }

            if (showAddSchoolDialog) {
                AddSchoolDialog(
                    availableClasses = availableClasses,
                    onDismissRequest = onDismissAddSchool,
                    onConfirmClick = { name: String, timezone: String, selectedClassIds: List<String> ->
                        schoolViewModel.onEvent(com.azuratech.azuratime.features.school.ui.list.SchoolUiEvent.CreateSchool(name, timezone, selectedClassIds))
                        onDismissAddSchool()
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewEmpty() {
    AzuraTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Dashboard is empty")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLoaded() {
    AzuraTheme {
        // Here you would render a mock DashboardContent or similar
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Dashboard Loaded")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewError() {
    AzuraTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(AzuraSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "An error occurred", color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(AzuraSpacing.md))
            Button(onClick = {}) {
                Text("Retry")
            }
        }
    }
}
