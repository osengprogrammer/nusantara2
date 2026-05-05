package com.azuratech.azuratime.ui.user

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
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuratime.domain.model.SyncStatus

// 🔥 Azura Design System
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun FindSchoolScreen(
    navController: NavController,
    workspaceViewModel: WorkspaceViewModel,
    currentUser: UserEntity?
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by workspaceViewModel.schoolSearchResults.collectAsStateWithLifecycle()
    val accessRequests by workspaceViewModel.accessRequests.collectAsStateWithLifecycle(emptyList())
    val uiState by workspaceViewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope() // 🔥 Needed for non-blocking snackbars
    
    // 🔥 FIXED: Wrapped snackbar calls in scope.launch to prevent blocking the LaunchedEffect
    LaunchedEffect(uiState) {
        when (val currentState = uiState) {
            is WorkspaceViewModel.WorkspaceState.Success -> {
                scope.launch { snackbarHostState.showSnackbar("Berhasil!") }
                workspaceViewModel.resetState()
            }
            is WorkspaceViewModel.WorkspaceState.RequestSent -> {
                scope.launch { snackbarHostState.showSnackbar("Permintaan bergabung ke ${currentState.schoolName} telah dikirim!") }
                workspaceViewModel.resetState()
            }
            is WorkspaceViewModel.WorkspaceState.RequestFailed -> {
                scope.launch { snackbarHostState.showSnackbar(currentState.message ?: "Gagal mengirim permintaan") }
                workspaceViewModel.resetState()
            }
            is WorkspaceViewModel.WorkspaceState.Error -> {
                scope.launch { snackbarHostState.showSnackbar(currentState.message) }
                workspaceViewModel.resetState()
            }
            else -> {}
        }
    }

    AzuraScreen(
        title = "Temukan Workspace",
        onBack = { navController.popBackStack() }
    ) {
        // 🔥 FIXED: Removed nested Scaffold. AzuraScreen provides a BoxScope, 
        // so we use Box to layer the UI and the SnackbarHost.
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = AzuraSpacing.md), // AzuraScreen already handles horizontal padding
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
            ) {
                // 🔍 Search Bar with Keyboard Support
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        workspaceViewModel.searchSchools(it)
                    },
                    label = { Text("Cari Nama Sekolah atau ID...") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = AzuraShapes.medium,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )

                // 📊 Result Handling
                when {
                    searchQuery.length < 3 -> {
                        EmptyDiscoveryState(
                            icon = Icons.Default.TravelExplore,
                            message = "Ketik minimal 3 karakter untuk mencari sekolah."
                        )
                    }
                    searchResults.isEmpty() -> {
                        EmptyDiscoveryState(
                            icon = Icons.Default.Search,
                            message = "Sekolah tidak ditemukan."
                        )
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            items(searchResults) { school ->
                                val schoolId = school["schoolId"] as? String ?: ""
                                val schoolName = school["schoolName"] as? String ?: "Unknown School"
                                
                                // Cek status membership di semua level (Active/Pending)
                                val membership = currentUser?.memberships?.get(schoolId)
                                val localRequest = accessRequests.find { it.schoolId == schoolId }
                                
                                val isFollowing = membership != null || localRequest != null
                                val status = membership?.role ?: localRequest?.status?.name ?: ""
                                val isSynced = localRequest?.syncStatus == SyncStatus.SYNCED

                                SchoolFollowCard(
                                    name = schoolName,
                                    id = schoolId,
                                    status = status,
                                    isFollowing = isFollowing,
                                    isSynced = isSynced,
                                    isLoading = uiState is WorkspaceViewModel.WorkspaceState.Switching,
                                    onFollowClick = {
                                        if (currentUser != null) {
                                            workspaceViewModel.sendJoinRequest(currentUser, schoolId, schoolName)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 🔥 Added SnackbarHost anchored to the bottom
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
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
    onFollowClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = AzuraShapes.small,
                modifier = Modifier.size(48.dp)
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
                    label = { Text(if (status == "PENDING") "Menunggu" else "Terdaftar") },
                    leadingIcon = { 
                        if (!isSynced) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(14.dp))
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                )
            } else if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Button(onClick = onFollowClick, shape = AzuraShapes.small, enabled = !isLoading) {
                    Text("Ikuti")
                }
            }
        }
    }
}