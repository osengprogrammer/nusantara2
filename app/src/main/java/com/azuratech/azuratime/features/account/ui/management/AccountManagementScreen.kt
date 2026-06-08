package com.azuratech.azuratime.features.account.ui.management

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
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
        onRemoveMemberRequest = { id ->
            targetAccountIdToDelete = id
            showDeleteDialog = true
        },
    )
}

@Composable
fun AccountManagementContent(
    uiState: AccountUiState,
    onEvent: (AccountUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onRemoveMemberRequest: (String) -> Unit,
) {
    AzuraScreen(
        title = "Account Settings",
        onBack = onNavigateBack,
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

                    // Active Class Selection
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

                    // Pending Followers (Requests to join school)
                    if (uiState.pendingFollowers.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                        ) {
                            Text(
                                text = "Pending Follower Requests",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = AzuraSpacing.md),
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
                    if (uiState.allAccountsInSameSchool.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                        ) {
                            Text(
                                text = "School Network",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = AzuraSpacing.md),
                            )
                            uiState.allAccountsInSameSchool
                                .filter { it.accountId != uiState.accountProfile?.accountId }
                                .forEach { account ->
                                    MemberItem(
                                        account = account,
                                        currentRole = account.memberships[uiState.activeSchoolId]?.role ?: account.role,
                                        currentUserRole = uiState.currentAccountRole,
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
fun MemberItem(
    account: AccountEntity,
    currentRole: String,
    currentUserRole: AccountRole,
    onChangeRole: (AccountRole) -> Unit,
    onRemoveMember: () -> Unit,
) {
    AzuraCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AzuraSpacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                StudentAvatar(photoPath = account.photoUrl, size = 40.dp)
                Spacer(modifier = Modifier.width(AzuraSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = account.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Current Role: $currentRole", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                // 🔥 ADMIN ONLY: Remove Member Button
                if (currentUserRole == AccountRole.ADMIN || currentUserRole == AccountRole.SUPER_ADMIN) {
                    IconButton(onClick = onRemoveMember) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Member",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // 🔥 ADMIN ONLY: Inline Role Selector for School Members
            if (currentUserRole == AccountRole.ADMIN || currentUserRole == AccountRole.SUPER_ADMIN) {
                Spacer(modifier = Modifier.height(AzuraSpacing.sm))
                HorizontalDivider(modifier = Modifier.padding(vertical = AzuraSpacing.xs), thickness = 0.5.dp)
                Text(text = "Change Role:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.xs),
                ) {
                    listOf(AccountRole.ADMIN, AccountRole.SUPERVISOR, AccountRole.USER).forEach { role ->
                        FilterChip(
                            selected = currentRole == role.name,
                            onClick = { if (currentRole != role.name) onChangeRole(role) },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.xs),
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
