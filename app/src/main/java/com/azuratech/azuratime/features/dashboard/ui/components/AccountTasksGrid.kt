package com.azuratech.azuratime.features.dashboard.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing

/**
 * 🛠️ ACCOUNT TASKS GRID (v3.2.1-ai-native)
 * Restored dashboard actions with strict RBAC enforcement.
 */
@Composable
fun AccountTasksGrid(
    navController: NavController,
    isAdmin: Boolean,
    currentRole: String = "USER",
    onRegisterStudentClick: () -> Unit,
    accountId: String? = null,
    pendingRequests: Int = 0,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val isSupervisor = currentRole == "SUPERVISOR" || currentRole == "TEACHER"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AzuraSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
    ) {
        // ======================================================
        // 🔥 Row 1: Academic Management (ADMIN ONLY)
        // ======================================================
        if (isAdmin) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
            ) {
                DashboardActionCard(
                    "Class Mgmt",
                    Icons.Default.Class,
                    MaterialTheme.colorScheme.primary,
                    {
                        if (accountId != null) {
                            navController.navigate(Screen.ClassManagement.createRoute(accountId))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isEnabled,
                )
                DashboardActionCard(
                    "Student Roster",
                    Icons.Default.People,
                    MaterialTheme.colorScheme.secondary,
                    { navController.navigate(Screen.StudentRoster.route) },
                    modifier = Modifier.weight(1f),
                    enabled = isEnabled,
                )
            }
        }

        // ======================================================
        // 🔥 Row 2: Attendance & Reports (ADMIN & SUPERVISOR)
        // ======================================================
        if (isAdmin || isSupervisor) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
            ) {
                DashboardActionCard(
                    "Attendance Matrix",
                    Icons.Default.GridView,
                    MaterialTheme.colorScheme.tertiary,
                    { navController.navigate(Screen.AttendanceMatrix.route) },
                    modifier = Modifier.weight(1f),
                    enabled = isEnabled,
                )
                DashboardActionCard(
                    "Reports",
                    Icons.Default.Assessment,
                    MaterialTheme.colorScheme.error,
                    { navController.navigate(Screen.AttendanceHistory.route) },
                    modifier = Modifier.weight(1f),
                    enabled = isEnabled,
                )
            }
        }

        // ======================================================
        // 🔥 Row 3: Identity & Security (ADMIN ONLY)
        // ======================================================
        if (isAdmin) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
            ) {
                DashboardActionCard(
                    "Account Mgmt",
                    Icons.Default.ManageAccounts,
                    MaterialTheme.colorScheme.primary,
                    { navController.navigate(Screen.Profile.route) },
                    modifier = Modifier.weight(1f),
                    enabled = isEnabled,
                )
                DashboardActionCard(
                    "Access Requests",
                    Icons.Default.NotificationsActive,
                    MaterialTheme.colorScheme.secondary,
                    { navController.navigate(Screen.PendingSchools.route) },
                    badgeCount = pendingRequests,
                    modifier = Modifier.weight(1f),
                    enabled = isEnabled,
                )
            }
        }

        // ======================================================
        // 🔥 Row 4: Quick Actions (ADMIN & SUPERVISOR)
        // ======================================================
        if (isAdmin || isSupervisor) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
            ) {
                DashboardActionCard(
                    "Register Student",
                    Icons.Default.PersonAdd,
                    MaterialTheme.colorScheme.primary,
                    onRegisterStudentClick,
                    modifier = Modifier.weight(1f),
                    enabled = isEnabled,
                )
                DashboardActionCard(
                    "Bulk Import",
                    Icons.Default.UploadFile,
                    MaterialTheme.colorScheme.secondary,
                    { navController.navigate(Screen.BulkRegister.route) },
                    modifier = Modifier.weight(1f),
                    enabled = isEnabled,
                )
            }
        }

        // ======================================================
        // 🔥 Row 5: Workspace & Network (Visible for ALL)
        // ======================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
        ) {
            DashboardActionCard(
                "Select School",
                Icons.Default.School,
                MaterialTheme.colorScheme.tertiary,
                {
                    if (accountId != null) {
                        navController.navigate(Screen.SchoolList.createRoute(accountId))
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = isEnabled,
            )
            DashboardActionCard(
                "School Network",
                Icons.Default.Hub,
                MaterialTheme.colorScheme.secondary,
                { navController.navigate(Screen.Following.route) },
                modifier = Modifier.weight(1f),
                enabled = isEnabled,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    badgeCount: Int = 0,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val alpha = if (enabled) 1f else 0.4f
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(110.dp),
        shape = AzuraShapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (enabled) 2.dp else 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f * alpha)),
    ) {
        Box {
            Row {
                Box(Modifier.width(6.dp).fillMaxHeight().background(color.copy(alpha = alpha)))
                Column(
                    Modifier
                        .padding(AzuraSpacing.md)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(icon, null, tint = color.copy(alpha = alpha), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        maxLines = 1,
                    )
                }
            }

            if (badgeCount > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(AzuraSpacing.sm),
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ) {
                    Text(badgeCount.toString())
                }
            }
        }
    }
}
