package com.azuratech.azuratime.features.dashboard.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.designsystem.theme.AzuraShapes
import com.azuratech.azuratime.core.designsystem.theme.AzuraSpacing
import com.azuratech.azuratime.core.designsystem.theme.AzuraTheme
import com.azuratech.azuratime.core.util.isAdmin
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
    var showHealthSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiEffect) {
        when (val effect = uiEffect) {
            is DashboardUiEffect.NavigateTo -> navController.navigate(effect.route)
            is DashboardUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            is DashboardUiEffect.ShowToast -> { /* Handle Toast if needed */ }
            else -> { /* Handle other effects like TriggerAtomicExit */ }
        }
    }

    AzuraScreen(
        title = "Azura Time",
        snackbarHost = { SnackbarHost(snackbarHostState) },
        actions = {
            if (uiState.account.isAdmin(uiState.currentSchool?.id ?: "")) {
                SystemHealthIcon(
                    isSyncing = uiState.isSyncing,
                    hasError = uiState.error != null,
                    onClick = { showHealthSheet = true },
                )
            }

            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Settings")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Logout") },
                    onClick = {
                        showMenu = false
                        showLogoutConfirmDialog = true
                    },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                )
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
                    onAddSchoolClick = { showAddSchoolDialog = true },
                    onSyncClick = { viewModel.onEvent(DashboardUiEvent.Refresh) },
                    onRegisterStudentClick = { viewModel.onEvent(DashboardUiEvent.OnRegisterStudentClick) },
                    onAttendanceClick = { classId ->
                        viewModel.onEvent(DashboardUiEvent.SelectActiveClass(classId, Screen.AttendanceCapture.route))
                    },
                    onRosterClick = { classId ->
                        viewModel.onEvent(DashboardUiEvent.SelectActiveClass(classId, Screen.StudentRoster.route))
                    },
                )
            }
        }
    }

    if (showHealthSheet) {
        HealthDashboardBottomSheet(
            onDismiss = { showHealthSheet = false },
            uiState = uiState,
            onRetrySync = {
                viewModel.onEvent(DashboardUiEvent.Refresh)
                showHealthSheet = false
            },
        )
    }

    if (showAddSchoolDialog) {
        AddSchoolDialog(
            availableClasses = schoolViewModel.uiStateFlow.collectAsStateWithLifecycle().value.availableClasses,
            onDismissRequest = { showAddSchoolDialog = false },
            onConfirmClick = { name, timezone, classes ->
                schoolViewModel.onEvent(com.azuratech.azuratime.features.school.ui.list.SchoolUiEvent.CreateSchool(name, timezone, classes))
                showAddSchoolDialog = false
            },
        )
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.unsyncedRecords > 0) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (uiState.unsyncedRecords > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.width(AzuraSpacing.sm))
                    Text(
                        text = if (uiState.unsyncedRecords > 0) "Data Belum Sinkron!" else "Konfirmasi Logout",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            text = {
                Column {
                    if (uiState.unsyncedRecords > 0) {
                        Text(
                            text = "⚠️ Perhatian, Brother! Ada ${uiState.unsyncedRecords} data lokal (absensi/biometrik/kelas) yang belum disinkronkan ke Cloud Firestore.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(AzuraSpacing.sm))
                        Text(
                            text = "Jika Anda logout sekarang, data ini akan hilang secara permanen dari perangkat ini. Harap batalkan dan lakukan Sinkronisasi di Dashboard terlebih dahulu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = "Apakah Anda yakin ingin keluar? Semua data Anda saat ini telah tersinkronisasi dengan aman di Cloud Firestore.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.onEvent(DashboardUiEvent.Logout)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.unsyncedRecords > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text("Logout Sekarang")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text(if (uiState.unsyncedRecords > 0) "Batalkan (Rekomendasi)" else "Batal")
                }
            },
        )
    }
}

@Composable
fun DashboardContent(
    navController: NavController,
    data: DashboardUiState,
    schoolViewModel: SchoolViewModel,
    onAddSchoolClick: () -> Unit,
    onSyncClick: () -> Unit,
    onRegisterStudentClick: () -> Unit,
    onAttendanceClick: (String) -> Unit,
    onRosterClick: (String) -> Unit,
) {
    val schoolUiState by schoolViewModel.uiStateFlow.collectAsStateWithLifecycle()
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
                photoUrl = account?.photoUrl?.let { Uri.parse(it) },
                onProfileClick = { navController.navigate(Screen.Profile.route) },
            )
        }

        item {
            MySchoolsCard(
                viewModel = schoolViewModel,
                accountId = account?.accountId ?: "",
                isApproved = data.isApproved,
                globalRole = account?.role?.name ?: "GUEST",
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = AzuraSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
                ) {
                    SyncStatusCard(
                        isSyncing = data.isSyncing,
                        lastSync = "Just now",
                        onSyncClick = onSyncClick,
                        modifier = Modifier.weight(1f),
                    )
                    Card(
                        modifier = Modifier.weight(0.6f).height(IntrinsicSize.Min),
                        shape = AzuraShapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(AzuraSpacing.md).fillMaxHeight(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "${data.totalActiveStudents}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Active Students",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            // Visible Version Badge for testing updates
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AssistChip(
                        onClick = {},
                        label = { Text("v${com.azuratech.azuratime.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall) },
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
                if (data.showSupervisorOnboarding) {
                    item {
                        SupervisorOnboardingCard(
                            onClick = {
                                navController.navigate(Screen.AssignClass.createRoute(data.account?.accountId ?: "", data.currentRole))
                            },
                        )
                    }
                }

                item {
                    ActiveSessionCard(
                        activeSession = data.activeSession,
                        allSessionsToday = data.allSessionsToday,
                        onStartAttendance = { sessionId ->
                            navController.navigate(com.azuratech.azuratime.core.navigation.NavigationRoutes.ATTENDANCE_CAPTURE.replace("{sessionId}", sessionId))
                        },
                        onManualPick = {
                            navController.navigate(Screen.SessionPicker.route)
                        },
                    )
                }

                if (data.isApproved && account.isAdmin(activeSchoolId)) {
                    item {
                        GpsGeofenceCard(
                            geofence = data.geofence,
                            onClick = { navController.navigate(Screen.GpsManagement.route) },
                        )
                    }
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
                val isAdmin = account.isAdmin(activeSchoolId)
                item {
                    AccountTasksGrid(
                        navController = navController,
                        isAdmin = isAdmin,
                        currentRole = data.currentRole,
                        onRegisterStudentClick = onRegisterStudentClick,
                        accountId = account?.accountId,
                        schoolId = activeSchoolId,
                        pendingRequests = data.pendingRequests,
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
                                Text("Music recommendations for studying", style = MaterialTheme.typography.bodySmall)
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
                        text = "Please select a school to start",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun SystemHealthIcon(
    isSyncing: Boolean,
    hasError: Boolean,
    onClick: () -> Unit,
) {
    val statusColor = when {
        hasError -> MaterialTheme.colorScheme.error
        isSyncing -> Color(0xFFFFC107)
        else -> Color(0xFF4CAF50)
    }

    Box(
        modifier = Modifier
            .padding(8.dp)
            .size(24.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = statusColor)
        }

        if (isSyncing) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
                color = statusColor.copy(alpha = 0.5f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthDashboardBottomSheet(
    onDismiss: () -> Unit,
    uiState: DashboardUiState,
    onRetrySync: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AzuraSpacing.lg)
                .navigationBarsPadding(),
        ) {
            Text("System Health", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = AzuraSpacing.md))

            HealthItem(
                label = "Cloud Sync",
                status = if (uiState.error == null) "Synced" else "Error",
                isError = uiState.error != null,
            )
            HealthItem(label = "Local DB", status = "Healthy", isError = false)
            HealthItem(label = "Template Version", status = "v3.4.0", isError = false)

            if (uiState.error != null) {
                Text(
                    text = "Last Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = AzuraSpacing.md),
                )
                Button(
                    onClick = onRetrySync,
                    modifier = Modifier.fillMaxWidth().padding(top = AzuraSpacing.lg),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Retry All Synchronization")
                }
            }
        }
    }
}

@Composable
fun HealthItem(label: String, status: String, isError: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = AzuraSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun SupervisorOnboardingCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AzuraSpacing.md),
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(AzuraSpacing.sm))
                Text(
                    text = "Welcome, Supervisor!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = "You haven't selected any classes to supervise. Please select a class to start taking attendance.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Select Class Now")
            }
        }
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
