package com.azuratech.azuratime.ui.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuraengine.model.User
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.ui.core.designsystem.AzuraButton
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.core.designsystem.AzuraTextField
import com.azuratech.azuratime.ui.core.designsystem.AzuraUserRow
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun NetworkContent(
    currentUser: User?,
    uiState: NetworkState,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onRefresh: () -> Unit,
    onAcceptFriend: (String) -> Unit,
    onRejectFriend: (String) -> Unit,
    onSearchUser: (String) -> Unit,
    onAddFriend: (String, String, String, String) -> Unit,
    onViewTargetUserClasses: (String, String, String) -> Unit,
    onQueryChange: (String) -> Unit
) {
    AzuraScreen(
        title = "Jaringan Sedulur",
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { onTabSelected(0) },
                    text = { Text("Daftar Teman", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { onTabSelected(1) },
                    text = { Text("Cari Teman", fontWeight = FontWeight.Bold) }
                )
            }

            when (selectedTabIndex) {
                0 -> FriendListSection(currentUser, onAcceptFriend, onRejectFriend, onViewTargetUserClasses)
                1 -> SearchFriendSection(currentUser, uiState, onQueryChange, onSearchUser, onAddFriend)
            }
        }
    }
}

@Composable
fun FriendListSection(
    currentUser: User?,
    onAcceptFriend: (String) -> Unit,
    onRejectFriend: (String) -> Unit,
    onViewTargetUserClasses: (String, String, String) -> Unit
) {
    val friendsMap = currentUser?.friends ?: emptyMap()
    val pendingRequests = friendsMap.filterValues { it.status == "PENDING_APPROVAL" }
    val activeFriends = friendsMap.filterValues { it.status == "FRIENDS" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(AzuraSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        if (pendingRequests.isNotEmpty()) {
            item {
                Text("Permintaan Masuk", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            items(pendingRequests.entries.toList()) { entry ->
                val friendId = entry.key
                val connection = entry.value
                AzuraUserRow(
                    name = connection.friendName,
                    subtitle = connection.friendEmail,
                    trailingContent = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { onAcceptFriend(friendId) },
                                modifier = Modifier.background(Color(0xFF4CAF50), CircleShape).size(36.dp)
                            ) { Icon(Icons.Default.Check, contentDescription = "Terima", tint = Color.White) }

                            IconButton(
                                onClick = { onRejectFriend(friendId) },
                                modifier = Modifier.background(Color(0xFFF44336), CircleShape).size(36.dp)
                            ) { Icon(Icons.Default.Close, contentDescription = "Tolak", tint = Color.White) }
                        }
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(AzuraSpacing.md)) }
        }

        item {
            Text("Sedulur Aktif", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        if (activeFriends.isEmpty()) {
            item { Text("Belum ada teman. Yuk cari sedulur baru!", color = Color.Gray, modifier = Modifier.padding(vertical = AzuraSpacing.md)) }
        } else {
            items(activeFriends.entries.toList()) { entry ->
                val friendId = entry.key
                val connection = entry.value
                AzuraUserRow(
                    name = connection.friendName,
                    subtitle = connection.friendEmail,
                    trailingContent = {
                        IconButton(onClick = { onViewTargetUserClasses(friendId, connection.friendName, connection.friendEmail) }) {
                            Icon(Icons.Default.Handshake, contentDescription = "Berbagi Akses Kelas", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SearchFriendSection(
    currentUser: User?,
    uiState: NetworkState,
    onQueryChange: (String) -> Unit,
    onSearchUser: (String) -> Unit,
    onAddFriend: (String, String, String, String) -> Unit
) {
    var localQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(AzuraSpacing.md)) {
        AzuraTextField(
            value = localQuery,
            onValueChange = {
                localQuery = it
                onQueryChange(it)
            },
            label = "Masukkan Email Guru...",
            trailingIcon = {
                IconButton(onClick = { onSearchUser(localQuery) }) {
                    Icon(Icons.Default.Search, contentDescription = "Cari")
                }
            }
        )

        Spacer(modifier = Modifier.height(AzuraSpacing.lg))

        when (uiState) {
            is NetworkState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            is NetworkState.UserFound -> {
                val targetUser = uiState.targetUser
                val myId = currentUser?.userId ?: ""
                val existingStatus = currentUser?.getFriendStatus(targetUser.userId)

                AzuraCard(
                    title = "Hasil Pencarian",
                    content = {
                        AzuraUserRow(
                            name = targetUser.name,
                            subtitle = targetUser.email,
                            trailingContent = {
                                if (targetUser.userId == myId) {
                                    Text("Ini akun Anda", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                } else if (existingStatus != null) {
                                    Text(text = existingStatus, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                } else {
                                    AzuraButton(
                                        text = "Add Friend",
                                        onClick = {
                                            onAddFriend(
                                                myId,
                                                currentUser?.name ?: "Guru",
                                                currentUser?.email ?: "",
                                                targetUser.email
                                            )
                                        },
                                        modifier = Modifier.height(36.dp)
                                    )
                                }
                            }
                        )
                    }
                )
            }
            else -> {}
        }
    }
}
