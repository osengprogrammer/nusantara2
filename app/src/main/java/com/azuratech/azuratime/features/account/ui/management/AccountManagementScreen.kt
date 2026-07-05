package com.azuratech.azuratime.features.account.ui.management

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.domain.model.AccountRole
import com.azuratech.azuratime.core.ui.designsystem.*
import com.azuratech.azuratime.core.designsystem.theme.AzuraSpacing
import com.azuratech.azuratime.core.util.showToast
import com.azuratech.azuratime.features.account.data.local.AccessRequestEntity
import com.azuratech.azuratime.features.account.data.local.AccountEntity

/**
 * 👤 ACCOUNT MANAGEMENT SCREEN (v3.2.1-ai-native)
 * Nuked and rebuilt for semantic purity and typo-free performance.
 */
@Composable
fun AccountManagementScreen(
    viewModel: AccountManagementViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToWelcome: () -> Unit,
    title: String = "Account Settings",
    onNavigateToBulkAssign: () -> Unit = {},
    onNavigateToAssignClass: (String, String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var targetAccountIdToDelete by remember { mutableStateOf<String?>(null) }

    // 🔥 AI Native: Collect and Handle UI Effects
    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is AccountUiEffect.ShowToast -> context.showToast(effect.message)
                is AccountUiEffect.ShowSnackbar -> context.showToast(effect.message)
                is AccountUiEffect.NavigateTo -> { /* Generic navigation if needed */ }
                AccountUiEffect.NavigateToWelcome -> onNavigateToWelcome()
                AccountUiEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    // Confirmation Dialog for Member Removal
    if (showDeleteDialog && targetAccountIdToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                targetAccountIdToDelete = null
            },
            title = { Text("Remove Member") },
            text = { Text("Are you sure you want to remove this member from the school network?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        targetAccountIdToDelete?.let { id ->
                            viewModel.onEvent(AccountUiEvent.RemoveMember(id))
                        }
                        showDeleteDialog = false
                        targetAccountIdToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    targetAccountIdToDelete = null
                }) {
                    Text("Cancel")
                }
            },
        )
    }

    AccountManagementContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        title = if (title == "Staff & Supervisors") "Staff Profiles" else title,
        onNavigateToBulkAssign = onNavigateToBulkAssign,
        onNavigateToAssignClass = onNavigateToAssignClass,
        onRemoveMemberRequest = { id ->
            targetAccountIdToDelete = id
            showDeleteDialog = true
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountManagementContent(
    uiState: AccountUiState,
    onEvent: (AccountUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onRemoveMemberRequest: (String) -> Unit,
    title: String = "Account Settings",
    onNavigateToBulkAssign: () -> Unit = {},
    onNavigateToAssignClass: (String, String) -> Unit = { _, _ -> },
) {
    AzuraScreen(
        title = title,
        onBack = onNavigateBack,
        actions = {
            if (title == "Staff Profiles") {
                IconButton(onClick = onNavigateToBulkAssign) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = "Bulk Assign",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
    ) {
        if (uiState.isLoggingOut) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Logging out...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(AzuraSpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
                ) {
                    val profile = uiState.accountProfile

                    StudentAvatar(photoPath = profile?.photoUrl, size = 96.dp)

                    Text(
                        text = profile?.name ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
                    )

                    Text(
                        text = profile?.email ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )

                    Spacer(modifier = Modifier.height(AzuraSpacing.lg))

                    // Active Class Selection (Only for personal Profile settings)
                    if (title != "Staff & Supervisors") {
                        AzuraCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(AzuraSpacing.md)) {
                                Text("Select Active Class", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(AzuraSpacing.sm))

                                if (uiState.availableClasses.isEmpty()) {
                                    Text(
                                        "No classes available in this school.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                } else {
                                    uiState.availableClasses.forEach { classModel ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            RadioButton(
                                                selected = classModel.id == uiState.activeClassId,
                                                onClick = { onEvent(AccountUiEvent.SelectActiveClass(classModel.id)) },
                                            )
                                            Text(classModel.name)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pending Followers (Requests to join school)
                    if (uiState.pendingFollowers.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                        ) {
                            SectionHeader(
                                title = "Pending Access Requests",
                                subtitle = "Review and approve new members",
                            )
                            uiState.pendingFollowers.forEach { request ->
                                PendingFollowerItem(
                                    request = request,
                                    selectedRole = uiState.selectedRoles[request.requestId] ?: AccountRole.USER,
                                    onRoleSelect = { role ->
                                        onEvent(AccountUiEvent.UpdatePendingRole(request.requestId, role))
                                    },
                                    onApprove = {
                                        onEvent(AccountUiEvent.ApproveFollower(request.requestId))
                                    },
                                )
                            }
                        }
                    }

                    // School Network (Member Role Management)
                    if (uiState.activeSchoolId != null && uiState.allAccountsInSameSchool.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                        ) {
                            SectionHeader(
                                title = "Active Staff Members",
                                subtitle = "Manage roles and classroom access",
                            )
                            uiState.allAccountsInSameSchool
                                .filter { it.accountId != uiState.accountProfile?.accountId }
                                .forEach { account ->
                                    val memberRoleInThisSchool = account.memberships[uiState.activeSchoolId]?.role ?: account.role
                                    MemberItem(
                                        account = account,
                                        currentRole = memberRoleInThisSchool,
                                        currentUserRole = uiState.currentAccountRole,
                                        onItemClick = { onNavigateToAssignClass(account.accountId, memberRoleInThisSchool) },
                                        onChangeRole = { newRole ->
                                            onEvent(AccountUiEvent.ChangeMemberRole(account.accountId, newRole))
                                        },
                                        onRemoveMember = { onRemoveMemberRequest(account.accountId) },
                                    )
                                }
                        }
                    }

                    AzuraButton(
                        text = "Logout",
                        onClick = { onEvent(AccountUiEvent.Logout) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    )
                }
            }
        }

        uiState.error?.let { error ->
            AlertDialog(
                onDismissRequest = { onEvent(AccountUiEvent.ClearError) },
                title = { Text("An Error Occurred") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { onEvent(AccountUiEvent.ClearError) }) {
                        Text("OK")
                    }
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AzuraSpacing.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = AzuraSpacing.xs),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemberItem(
    account: AccountEntity,
    currentRole: String,
    currentUserRole: AccountRole,
    onItemClick: () -> Unit,
    onChangeRole: (AccountRole) -> Unit,
    onRemoveMember: () -> Unit,
) {
    AzuraCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
    ) {
        Column(modifier = Modifier.padding(AzuraSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                StudentAvatar(photoPath = account.photoUrl, size = 40.dp)
                Spacer(modifier = Modifier.width(AzuraSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(8.dp))
                        RoleBadge(roleStr = currentRole)
                    }
                    Text(text = account.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                // 🔥 ADMIN ONLY: Management Actions
                if (currentUserRole == AccountRole.ADMIN || currentUserRole == AccountRole.SUPER_ADMIN) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onItemClick) {
                            Icon(
                                imageVector = Icons.Default.AssignmentInd,
                                contentDescription = "Edit Matrix",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = onRemoveMember) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove Member",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            // 🔥 ADMIN ONLY: Inline Role Selector for School Members
            if (currentUserRole == AccountRole.ADMIN || currentUserRole == AccountRole.SUPER_ADMIN) {
                Spacer(modifier = Modifier.height(AzuraSpacing.sm))
                HorizontalDivider(modifier = Modifier.padding(vertical = AzuraSpacing.xs), thickness = 0.5.dp)
                Text(text = "Change Role:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.xs),
                ) {
                    listOf(AccountRole.ADMIN, AccountRole.SUPERVISOR, AccountRole.USER).forEach { role ->
                        FilterChip(
                            selected = currentRole.equals(role.name, ignoreCase = true),
                            onClick = { if (!currentRole.equals(role.name, ignoreCase = true)) onChangeRole(role) },
                            label = { Text(role.name, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoleBadge(roleStr: String, modifier: Modifier = Modifier) {
    val role = try { AccountRole.valueOf(roleStr.uppercase()) } catch (e: Exception) { AccountRole.USER }
    val (color, label) = when (role) {
        AccountRole.SUPER_ADMIN -> MaterialTheme.colorScheme.error to "Super Admin"
        AccountRole.ADMIN -> MaterialTheme.colorScheme.primary to "Admin"
        AccountRole.SUPERVISOR -> MaterialTheme.colorScheme.secondary to "Supervisor"
        AccountRole.USER -> MaterialTheme.colorScheme.outline to "Member"
    }
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = androidx.compose.foundation.shape.CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PendingFollowerItem(
    request: AccessRequestEntity,
    selectedRole: AccountRole,
    onRoleSelect: (AccountRole) -> Unit,
    onApprove: () -> Unit,
) {
    AzuraCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AzuraSpacing.md)) {
            Text(
                text = "ID: ${request.accountId}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(AzuraSpacing.xs))
            Text(
                text = "Assign Role:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.xs),
            ) {
                listOf(AccountRole.ADMIN, AccountRole.SUPERVISOR, AccountRole.USER).forEach { role ->
                    FilterChip(
                        selected = selectedRole == role,
                        onClick = { onRoleSelect(role) },
                        label = { Text(role.name, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
            Spacer(modifier = Modifier.height(AzuraSpacing.sm))
            AzuraButton(
                text = "Approve",
                onClick = onApprove,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewAccountLoading() {
    MaterialTheme {
        AccountManagementContent(
            uiState = AccountPreviewMocks.loading(),
            onEvent = {},
            onNavigateBack = {},
            onRemoveMemberRequest = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewAccountPopulated() {
    MaterialTheme {
        AccountManagementContent(
            uiState = AccountPreviewMocks.populated(),
            onEvent = {},
            onNavigateBack = {},
            onRemoveMemberRequest = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewAccountError() {
    MaterialTheme {
        AccountManagementContent(
            uiState = AccountPreviewMocks.error(),
            onEvent = {},
            onNavigateBack = {},
            onRemoveMemberRequest = {},
        )
    }
}
