package com.azuratech.azuratime.features.account.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch

// 🔥 DB & ViewModels
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.core.domain.model.SyncStatus

// 🔥 Azura Design System
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing

@Composable
fun FindSchoolScreen(
    navController: NavController,
    workspaceViewModel: WorkspaceViewModel,
    currentAccount: AccountEntity?,
) {
    val uiState by workspaceViewModel.uiStateFlow.collectAsStateWithLifecycle()
    val searchQuery = uiState.searchQuery
    val searchResults = uiState.searchResults
    val accessRequests = uiState.accessRequests
    val focusManager = LocalFocusManager.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope() // 🔥 Needed for non-blocking snackbars

    // 🔥 FIXED: Wrapped snackbar calls in scope.launch to prevent blocking the LaunchedEffect
    LaunchedEffect(uiState.status) {
        when (val currentStatus = uiState.status) {
            is WorkspaceStatus.Success -> {
                scope.launch { snackbarHostState.showSnackbar("Success!") }
                workspaceViewModel.onEvent(WorkspaceUiEvent.ResetState)
            }
            is WorkspaceStatus.RequestSent -> {
                scope.launch { snackbarHostState.showSnackbar("Join request to ${currentStatus.schoolName} has been sent!") }
                workspaceViewModel.onEvent(WorkspaceUiEvent.ResetState)
            }
            is WorkspaceStatus.RequestFailed -> {
                scope.launch { snackbarHostState.showSnackbar(currentStatus.message ?: "Failed to send request") }
                workspaceViewModel.onEvent(WorkspaceUiEvent.ResetState)
            }
            is WorkspaceStatus.Error -> {
                scope.launch { snackbarHostState.showSnackbar(currentStatus.message) }
                workspaceViewModel.onEvent(WorkspaceUiEvent.ResetState)
            }
            else -> {}
        }
    }

    AzuraScreen(
        title = "Find Workspace",
        onBack = { navController.popBackStack() },
    ) {
        // 🔥 FIXED: Removed nested Scaffold. AzuraScreen provides a BoxScope,
        // so we use Box to layer the UI and the SnackbarHost.
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = AzuraSpacing.md), // AzuraScreen already handles horizontal padding
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
            ) {
                // 🔍 Search Bar with Keyboard Support
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        workspaceViewModel.onEvent(WorkspaceUiEvent.UpdateSearchQuery(it))
                    },
                    label = { Text("Search School Name or ID...") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = AzuraShapes.medium,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                )

                // 📊 Result Handling
                when {
                    searchQuery.length < 3 -> {
                        EmptyDiscoveryState(
                            icon = Icons.Default.TravelExplore,
                            message = "Type at least 3 characters to search for schools.",
                        )
                    }
                    searchResults.isEmpty() -> {
                        EmptyDiscoveryState(
                            icon = Icons.Default.Search,
                            message = "No schools found.",
                        )
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                            contentPadding = PaddingValues(bottom = 100.dp),
                        ) {
                            items(searchResults) { school ->
                                val schoolId = school["schoolId"] as? String ?: ""
                                val schoolName = school["schoolName"] as? String ?: "Unknown School"

                                // Cek status membership di semua level (Active/Pending)
                                val membership = currentAccount?.memberships?.get(schoolId)
                                val profile = accessRequests.find { it.schoolId == schoolId }

                                val isFollowing = membership != null || profile != null
                                val status = membership?.role ?: profile?.status?.name ?: ""
                                val isSynced = profile?.syncStatus == SyncStatus.SYNCED

                                SchoolFollowCard(
                                    name = schoolName,
                                    id = schoolId,
                                    status = status,
                                    isFollowing = isFollowing,
                                    isSynced = isSynced,
                                    isLoading = uiState.status is WorkspaceStatus.Switching,
                                    onFollowClick = {
                                        if (currentAccount != null) {
                                            workspaceViewModel.onEvent(WorkspaceUiEvent.SendJoinRequest(currentAccount.accountId, schoolId, schoolName))
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // 🔥 Added SnackbarHost anchored to the bottom
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
fun EmptyDiscoveryState(icon: ImageVector, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
            Spacer(Modifier.height(AzuraSpacing.sm))
            Text(message, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SchoolFollowCard(
    name: String,
    id: String,
    status: String,
    isFollowing: Boolean,
    isSynced: Boolean,
    isLoading: Boolean,
    onFollowClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = AzuraShapes.small,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.width(AzuraSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(text = "ID: $id", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }

            if (isFollowing) {
                AssistChip(
                    onClick = {},
                    label = { Text(if (status == "PENDING") "Waiting" else "Registered") },
                    leadingIcon = {
                        if (!isSynced) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(14.dp))
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    },
                )
            } else if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Button(onClick = onFollowClick, shape = AzuraShapes.small, enabled = !isLoading) {
                    Text("Follow")
                }
            }
        }
    }
}
