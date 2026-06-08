package com.azuratech.azuratime.features.account.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.core.domain.model.AccountRole

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FollowingScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToAssignClass: (String, String) -> Unit = { _, _ -> },
    viewModel: FollowingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    var searchEmail by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(FollowingUiEvent.ClearError)
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.onEvent(FollowingUiEvent.ClearSuccessMessage)
        }
    }

    AzuraScreen(
        title = "Following",
        onBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Search") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = {
                    BadgedBox(badge = { if (uiState.pendingRequests.isNotEmpty()) Badge { Text(uiState.pendingRequests.size.toString()) } }) {
                        Text("Requests")
                    }
                })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Friends") })
            }

            Column(modifier = Modifier.fillMaxSize().padding(AzuraSpacing.md)) {
                when (selectedTab) {
                    0 -> SearchTab(
                        state = uiState,
                        email = searchEmail,
                        onEmailChange = { searchEmail = it },
                        onSearch = { viewModel.onEvent(FollowingUiEvent.SearchByEmail(it)) },
                        onAdd = { targetId ->
                            if (uiState.connections.any { it.accountId == targetId }) {
                                viewModel.onEvent(FollowingUiEvent.UnfollowFriend(targetId))
                            } else {
                                viewModel.onEvent(FollowingUiEvent.SendConnectionRequest(targetId))
                            }
                        },
                    )
                    1 -> RequestsTab(uiState, { viewModel.onEvent(FollowingUiEvent.AcceptRequest(it)) }, { viewModel.onEvent(FollowingUiEvent.DeclineRequest(it)) })
                    2 -> ConnectionsTab(
                        uiState = uiState,
                        onAssign = { account ->
                            val role = uiState.activeSchoolId?.let { schoolId ->
                                account.memberships[schoolId]?.role
                            } ?: "USER"
                            onNavigateToAssignClass(account.accountId, role)
                        },
                        onChangeRole = { id, role ->
                            viewModel.onEvent(FollowingUiEvent.ChangeMemberRole(id, role))
                        },
                        onUnfollow = { id ->
                            viewModel.onEvent(FollowingUiEvent.UnfollowFriend(id))
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun SearchTab(state: FollowingUiState, email: String, onEmailChange: (String) -> Unit, onSearch: (String) -> Unit, onAdd: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Search Account (Email)") },
            modifier = Modifier.fillMaxWidth(),
            shape = AzuraShapes.medium,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                Button(onClick = { onSearch(email) }, enabled = !state.isLoading && email.isNotBlank(), shape = AzuraShapes.small) {
                    Text("Search")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(AzuraSpacing.lg))
        Box(Modifier.weight(1f)) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (state.results.isNotEmpty()) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)) {
                    items(state.results) { account ->
                        AccountResultItem(
                            account = account,
                            state = state,
                            onAdd = { onAdd(account.accountId) },
                        )
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Search for fellow supervisors to connect.") }
            }
        }
    }
}

@Composable
fun RequestsTab(state: FollowingUiState, onAccept: (String) -> Unit, onDecline: (String) -> Unit) {
    if (state.pendingRequests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No new requests.")
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)) {
            items(state.pendingRequests) { account ->
                PendingRequestItem(account, state.isProcessing, { onAccept(account.accountId) }, { onDecline(account.accountId) })
            }
        }
    }
}

@Composable
fun ConnectionsTab(
    uiState: FollowingUiState,
    onAssign: (AccountEntity) -> Unit,
    onChangeRole: (String, AccountRole) -> Unit,
    onUnfollow: (String) -> Unit,
) {
    if (uiState.connections.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No connected friends yet.")
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)) {
            items(uiState.connections) { account ->
                ConnectedFriendItem(
                    account = account,
                    isAdmin = uiState.isAdmin,
                    activeSchoolId = uiState.activeSchoolId,
                    onAssign = { onAssign(account) },
                    onChangeRole = { role -> onChangeRole(account.accountId, role) },
                    onUnfollow = { onUnfollow(account.accountId) },
                )
            }
        }
    }
}

@Composable
fun AccountResultItem(
    account: AccountEntity,
    state: FollowingUiState,
    onAdd: () -> Unit,
) {
    val isFriend = state.connections.any { it.accountId == account.accountId }
    val isSent = state.sentRequestIds.contains(account.accountId)
    val isIncoming = state.pendingRequests.any { it.accountId == account.accountId }

    Card(modifier = Modifier.fillMaxWidth(), shape = AzuraShapes.medium) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = account.photoUrl ?: "https://ui-avatars.com/api/?name=${account.name}", contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(account.name, fontWeight = FontWeight.Bold)
                Text(account.email, style = MaterialTheme.typography.bodySmall)
            }

            when {
                isFriend -> {
                    IconButton(onClick = onAdd) { // In this context, onAdd will be mapped to Unfollow
                        Icon(
                            imageVector = Icons.Default.PersonRemove,
                            contentDescription = "Disconnect",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                isSent -> {
                    Button(
                        onClick = { },
                        enabled = false,
                        shape = AzuraShapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text("Requested")
                    }
                }
                isIncoming -> {
                    Text(
                        text = "Pending Response",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                else -> {
                    Button(onClick = onAdd, enabled = !state.isProcessing, shape = AzuraShapes.small) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectedFriendItem(
    account: AccountEntity,
    isAdmin: Boolean,
    activeSchoolId: String?,
    onAssign: () -> Unit,
    onChangeRole: (AccountRole) -> Unit,
    onUnfollow: () -> Unit,
) {
    val currentRole = activeSchoolId?.let { account.memberships[it]?.role } ?: account.role
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove Friend") },
            text = { Text("Are you sure you want to remove ${account.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUnfollow()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = AzuraShapes.medium) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = account.photoUrl ?: "https://ui-avatars.com/api/?name=${account.name}", contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(account.name, fontWeight = FontWeight.Bold)
                    Text(account.email, style = MaterialTheme.typography.bodySmall)
                    Text(text = "Role: $currentRole", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }

                if (isAdmin) {
                    IconButton(onClick = onAssign) { Icon(Icons.Default.School, contentDescription = "Grant Class Access", tint = MaterialTheme.colorScheme.primary) }
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Friend",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (isAdmin) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "Change Role:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
fun PendingRequestItem(account: AccountEntity, isProcessing: Boolean, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = AzuraShapes.medium) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = account.photoUrl ?: "https://ui-avatars.com/api/?name=${account.name}", contentDescription = null, modifier = Modifier.size(48.dp).clip(CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(account.name, fontWeight = FontWeight.Bold)
                Row(Modifier.padding(top = 4.dp)) {
                    Button(onClick = onAccept, enabled = !isProcessing, shape = AzuraShapes.small, modifier = Modifier.height(32.dp)) { Text("Accept", style = MaterialTheme.typography.labelSmall) }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onDecline, enabled = !isProcessing, shape = AzuraShapes.small, modifier = Modifier.height(32.dp)) { Text("Decline", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}
