package com.azuratech.azuratime.ui.dashboard

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.azuratech.azuratime.MainActivity
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.ui.components.ConflictResolverDialog
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuratime.ui.core.designsystem.WorkspaceSelector
import com.azuratech.azuratime.ui.core.preview.AzuraPreviews
import com.azuratech.azuratime.ui.core.preview.PreviewMocks
import com.azuratech.azuratime.ui.dashboard.components.*
import com.azuratech.azuratime.ui.data.IntegritySummaryWidget
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.theme.AzuraTheme
import com.azuratech.azuratime.ui.util.UiState
import com.google.firebase.auth.FirebaseAuth

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.syncCompletedEvent.collect {
            Toast.makeText(context, "Sinkronisasi Selesai!", Toast.LENGTH_SHORT).show()
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

            if (data.conflicts.isNotEmpty()) {
                ConflictResolverDialog(
                    conflict = data.conflicts.first(),
                    onResolve = { useCloud -> viewModel.resolveConflict(data.conflicts.first(), useCloud) }
                )
            }

            DashboardContent(
                navController = navController,
                data = data,
                onSyncClick = { viewModel.sync() },
                onSelectClass = { classId -> viewModel.selectActiveClass(classId) },
                onLogout = {
                    viewModel.logout {
                        val intent = Intent(context, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    }
                }
            )
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
    onSyncClick: () -> Unit,
    onSelectClass: (String?) -> Unit,
    onLogout: () -> Unit
) {
    AzuraScreen(
        title = data.user?.schoolName?.let { "Azura - $it" } ?: "Azura IMS",
        actions = {
            WorkspaceSelector(
                currentUser = data.user,
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
                    schoolName = user.schoolName ?: "",
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

            if (data.currentRole == "ADMIN") {
                item {
                    IntegritySummaryWidget(
                        totalFaces = data.totalFaces,
                        unassignedCount = data.unassignedStudents,
                        brokenLinks = data.brokenAssignments,
                        unsyncedCount = data.unsyncedRecords
                    )
                }
            }

            if (data.isSyncing) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp)) }
            }

            if (data.isApproved) {
                item {
                    ActiveSessionCard(
                        allClasses = data.assignedClasses,
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
                    isAdmin = data.currentRole == "ADMIN"
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
                onSyncClick = {},
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
                onSyncClick = {},
                onSelectClass = {},
                onLogout = {}
            )
        }
    }
}