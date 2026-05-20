package com.azuratech.azuratime.features.account.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.features.account.data.local.AccountEntity

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FollowingScreen(
    onNavigateBack: () -> Unit = {},
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

    AzuraScreen(
        title = "Following",
        onBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Cari") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = {
                    BadgedBox(badge = { if (uiState.pendingRequests.isNotEmpty()) Badge { Text(uiState.pendingRequests.size.toString()) } }) {
                        Text("Permintaan")
                    }
                })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Teman") })
            }

            Column(modifier = Modifier.fillMaxSize().padding(AzuraSpacing.md)) {
                when (selectedTab) {
                    0 -> SearchTab(uiState, searchEmail, { searchEmail = it }, { viewModel.onEvent(FollowingUiEvent.SearchByEmail(it)) }, { viewModel.onEvent(FollowingUiEvent.SendConnectionRequest(it)) })
                    1 -> RequestsTab(uiState, { viewModel.onEvent(FollowingUiEvent.AcceptRequest(it)) }, { viewModel.onEvent(FollowingUiEvent.DeclineRequest(it)) })
                    2 -> ConnectionsTab(uiState, { viewModel.onEvent(FollowingUiEvent.SelectFriendForAssignment(it)) })
                }
            }
        }

        // 🏫 Class Assignment Dialog
        if (uiState.selectedFriendForAssignment != null) {
            ClassAssignmentDialog(
                friend = uiState.selectedFriendForAssignment!!,
                availableClasses = uiState.availableClasses,
                isProcessing = uiState.isProcessing,
                onDismiss = { viewModel.onEvent(FollowingUiEvent.SelectFriendForAssignment(null)) },
                onConfirm = { classIds -> viewModel.onEvent(FollowingUiEvent.AssignClasses(uiState.selectedFriendForAssignment!!.accountId, classIds)) },
            )
        }
    }
}

@Composable
fun SearchTab(state: FollowingUiState, email: String, onEmailChange: (String) -> Unit, onSearch: (String) -> Unit, onAdd: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Cari Guru (Email)") },
            modifier = Modifier.fillMaxWidth(),
            shape = AzuraShapes.medium,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                Button(onClick = { onSearch(email) }, enabled = !state.isLoading && email.isNotBlank(), shape = AzuraShapes.small) {
                    Text("Cari")
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
                        AccountResultItem(account, state.isProcessing, { onAdd(account.accountId) })
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Cari rekan guru untuk terhubung.") }
            }
        }
    }
}

@Composable
fun RequestsTab(state: FollowingUiState, onAccept: (String) -> Unit, onDecline: (String) -> Unit) {
    if (state.pendingRequests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Tidak ada permintaan baru.")
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
fun ConnectionsTab(state: FollowingUiState, onAssign: (AccountEntity) -> Unit) {
    if (state.connections.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Belum ada teman terhubung.")
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)) {
            items(state.connections) { account ->
                ConnectedFriendItem(account, { onAssign(account) })
            }
        }
    }
}

@Composable
fun ClassAssignmentDialog(
    friend: AccountEntity,
    availableClasses: List<ClassModel>,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val selectedIds = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = AzuraShapes.large, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Beri Akses Kelas", style = MaterialTheme.typography.titleLarge)
                Text("Pilih kelas yang akan dikelola oleh ${friend.name}", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))

                Box(Modifier.heightIn(max = 300.dp)) {
                    LazyColumn {
                        items(availableClasses) { cls ->
                            Row(
                                modifier = Modifier.fillMaxWidth().selectable(
                                    selected = selectedIds.contains(cls.id),
                                    onClick = { if (selectedIds.contains(cls.id)) selectedIds.remove(cls.id) else selectedIds.add(cls.id) },
                                ).padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = selectedIds.contains(cls.id), onCheckedChange = null)
                                Spacer(Modifier.width(8.dp))
                                Text(cls.name)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(selectedIds.toList()) }, enabled = !isProcessing && selectedIds.isNotEmpty()) {
                        if (isProcessing) CircularProgressIndicator(Modifier.size(16.dp)) else Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun AccountResultItem(account: AccountEntity, isProcessing: Boolean, onAdd: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = AzuraShapes.medium) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = account.photoUrl ?: "https://ui-avatars.com/api/?name=${account.name}", contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(account.name, fontWeight = FontWeight.Bold)
                Text(account.email, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onAdd, enabled = !isProcessing, shape = AzuraShapes.small) { Text("Tambah") }
        }
    }
}

@Composable
fun ConnectedFriendItem(account: AccountEntity, onAssign: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = AzuraShapes.medium) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = account.photoUrl ?: "https://ui-avatars.com/api/?name=${account.name}", contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(account.name, fontWeight = FontWeight.Bold)
                Text(account.email, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onAssign) { Icon(Icons.Default.School, contentDescription = "Beri Akses Kelas", tint = MaterialTheme.colorScheme.primary) }
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
                    Button(onClick = onAccept, enabled = !isProcessing, shape = AzuraShapes.small, modifier = Modifier.height(32.dp)) { Text("Terima", style = MaterialTheme.typography.labelSmall) }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onDecline, enabled = !isProcessing, shape = AzuraShapes.small, modifier = Modifier.height(32.dp)) { Text("Tolak", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}
