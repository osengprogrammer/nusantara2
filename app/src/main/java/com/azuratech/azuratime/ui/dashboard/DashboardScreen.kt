package com.azuratech.azuratime.ui.dashboard

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.azuratech.azuratime.MainActivity
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.ui.core.designsystem.ConflictResolverDialog
import com.azuratech.azuratime.ui.core.UiEvent
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuratime.ui.core.designsystem.WorkspaceSelector
import com.azuratech.azuratime.ui.core.preview.AzuraPreviews
import com.azuratech.azuratime.ui.core.preview.PreviewMocks
import com.azuratech.azuratime.ui.dashboard.components.*
import com.azuratech.azuratime.ui.school.SchoolViewModel
import com.azuratech.azuratime.ui.school.AddSchoolDialog
import com.azuratech.azuratime.ui.data.IntegritySummaryWidget
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuraengine.model.School
import com.azuratech.azuratime.ui.theme.AzuraTheme
import com.azuratech.azuratime.ui.util.UiState
import com.google.firebase.auth.FirebaseAuth

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val schoolViewModel: SchoolViewModel = hiltViewModel()
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val availableClasses by schoolViewModel.availableClasses.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddSchoolDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.NavigateTo -> navController.navigate(event.route)
                is UiEvent.NavigateUp -> navController.navigateUp()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is DashboardViewModel.NavigationEvent.NavigateToRegistration -> {
                    navController.navigate(Screen.RegistrationMenu.route)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        schoolViewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
        }
    }

    when (val state = uiState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Success -> {
            val data = state.data
            
            if (!data.isReady) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Sync schoolViewModel accountId
                data.user?.userId?.let { userId ->
                    LaunchedEffect(userId) {
                        schoolViewModel.setAccountId(userId)
                    }
                }

                if (data.conflicts.isNotEmpty()) {
                    ConflictResolverDialog(
                        conflict = data.conflicts.first(),
                        onResolve = { useCloud -> viewModel.resolveConflict(data.conflicts.first(), useCloud) }
                    )
                }

                DashboardContent(
                    navController = navController,
                    data = data,
                    schoolViewModel = schoolViewModel,
                    availableClasses = availableClasses,
                    snackbarHostState = snackbarHostState,
                    showAddSchoolDialog = showAddSchoolDialog,
                    onAddSchoolClick = { showAddSchoolDialog = true },
                    onDismissAddSchool = { showAddSchoolDialog = false },
                    onSyncClick = { viewModel.sync() },
                    onRegisterStudentClick = { viewModel.onRegisterStudentClick() },
                    onSelectClass = { classId -> 
                        println("🖱 DASHBOARD: Ganti Kelas clicked for $classId")
                        println("💾 DASHBOARD: Updating activeClassId in ViewModel")
                        viewModel.selectActiveClass(classId) 
                    },
                    onLogout = {
                        viewModel.logout {
                            val intent = Intent(context, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }
        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(text = state.message ?: "Unknown Error")
            }
        }
        is UiState.Empty -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(text = "Empty")
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
    onRegisterStudentClick: () -> Unit, // 👈 Added
    onSelectClass: (String?) -> Unit,
    onLogout: () -> Unit
) {
    val activeSchool by schoolViewModel.activeSchool.collectAsStateWithLifecycle()
    val schools by schoolViewModel.allSchools.collectAsStateWithLifecycle()

    AzuraScreen(
        title = activeSchool?.name?.let { "Azura - $it" } ?: "Azura IMS",
        snackbarHostState = snackbarHostState,
        actions = {
            WorkspaceSelector(
                schoolViewModel = schoolViewModel,
                workspaceViewModel = hiltViewModel()
            )
            DashboardSyncButton(
                isSyncing = data.isSyncing,
                onSyncClick = onSyncClick
            )
        }
    ) {
        val user = data.user ?: return@AzuraScreen
        val photoUrl = FirebaseAuth.getInstance().currentUser?.photoUrl

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.lg)
            ) {
                if (!data.isApproved) {
                    item {
                        AzuraCard(
                            title = "Akses Dibatasi",
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                text = "Akun Anda sedang menunggu verifikasi Admin. Fitur scanner akan muncul setelah disetujui.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                item {
                    ProfileHeader(
                        name = user.name,
                        email = user.email,
                        schoolName = activeSchool?.name ?: user.schoolName ?: "",
                        photoUrl = photoUrl,
                        onLogout = onLogout,
                        onProfileClick = { navController.navigate(Screen.Profile.route) }
                    )
                }

                item {
                    SedulurNetworkButton(
                        pendingRequests = data.pendingRequests,
                        onClick = { navController.navigate(Screen.Network.route) }
                    )
                }

                if (data.currentRole == "ADMIN" || data.currentRole == "SUPER_ADMIN") {
                    item {
                        IntegritySummaryWidget(
                            totalFaces = data.totalFaces,
                            unassignedCount = data.unassignedStudents,
                            brokenLinks = data.brokenAssignments,
                            unsyncedCount = data.unsyncedRecords
                        )
                    }
                }

                // 👑 SUPER ADMIN MODERATION
                if (data.currentRole == "SUPER_ADMIN") {
                    item {
                        AzuraCard(
                            title = "Moderasi Sistem",
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Persetujuan Sekolah", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Lihat pendaftaran sekolah yang menunggu verifikasi.", style = MaterialTheme.typography.bodySmall)
                                }
                                Button(
                                    onClick = { navController.navigate(Screen.PendingSchools.route) },
                                    shape = AzuraShapes.medium
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
                        println("🔓 DEBUG: School card access (count=${schools.size}, approved=${data.isApproved})")
                        MySchoolsCard(
                            viewModel = schoolViewModel,
                            accountId = user.userId,
                            isApproved = data.isApproved,
                            onSchoolClick = {
                                if (user.userId.isNullOrEmpty()) {
                                    println("🚫 DEBUG: userId is null/empty")
                                    return@MySchoolsCard
                                }
                                println("🏫 DEBUG: userId=${user.userId}, route=${Screen.SchoolList.createRoute(user.userId)}")
                                navController.navigate(Screen.SchoolList.createRoute(user.userId))
                            },
                            onAddSchool = {
                                println("➕ DEBUG: Add School clicked")
                                onAddSchoolClick()
                            }
                        )
                    }
                }

                if (data.isApproved) {
                    item {
                        ActiveSessionCard(
                            allClasses = data.allClasses, // 🔥 Use all available classes
                            activeClassId = user.activeClassId,
                            onSelectClass = onSelectClass
                        )
                    }

                    if (data.sessionStudents.isNotEmpty()) {
                        item { SessionStudentsList(students = data.sessionStudents) }
                    }

                    item {
                        MyAssignedClassesSection(
                            myClasses = data.assignedClasses,
                            onNavigateToAll = { navController.navigate(Screen.MyAssignedClass.route) }
                        )
                    }
                }

                item {
                    TeacherTasksGrid(
                        navController = navController,
                        isAdmin = data.currentRole == "ADMIN" || data.currentRole == "SUPER_ADMIN",
                        currentRole = data.currentRole,
                        onRegisterStudentClick = onRegisterStudentClick,
                        accountId = user.userId,
                        isEnabled = activeSchool?.status == "ACTIVE" // 🔥 Added
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    RecentScansHeader(navController = navController)
                }

                items(data.recentRecords) { record ->
                    DashboardCheckInItem(record)
                }
            }

            if (showAddSchoolDialog) {
                AddSchoolDialog(
                    availableClasses = availableClasses,
                    onDismiss = onDismissAddSchool,
                    onConfirm = { name, timezone, selectedClassIds ->
                        schoolViewModel.createSchool(name, timezone, selectedClassIds)
                        onDismissAddSchool()
                    }
                )
            }
        }
    }
}

@AzuraPreviews
@Composable
fun DashboardContentSuccessPreview() {
    AzuraTheme {
        Surface {
            DashboardContent(
                navController = rememberNavController(),
                data = PreviewMocks.mockDashboardStateSuccess,
                schoolViewModel = hiltViewModel(),
                availableClasses = emptyList(),
                snackbarHostState = remember { SnackbarHostState() },
                showAddSchoolDialog = false,
                onAddSchoolClick = {},
                onDismissAddSchool = {},
                onSyncClick = {},
                onRegisterStudentClick = {}, // 👈 Added
                onSelectClass = {},
                onLogout = {}
            )
        }
    }
}

@AzuraPreviews
@Composable
fun DashboardContentLoadingPreview() {
    AzuraTheme {
        Surface {
            DashboardContent(
                navController = rememberNavController(),
                data = PreviewMocks.mockDashboardStateLoading,
                schoolViewModel = hiltViewModel(),
                availableClasses = emptyList(),
                snackbarHostState = remember { SnackbarHostState() },
                showAddSchoolDialog = false,
                onAddSchoolClick = {},
                onDismissAddSchool = {},
                onSyncClick = {},
                onRegisterStudentClick = {}, // 👈 Added
                onSelectClass = {},
                onLogout = {}
            )
        }
    }
}
