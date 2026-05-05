package com.azuratech.azuratime.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.azuratech.azuraengine.model.User
import com.azuratech.azuratime.core.navigation.Screen
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.core.designsystem.AzuraButton
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuratime.ui.core.designsystem.AzuraTextField
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import kotlinx.coroutines.launch

@Composable
fun NetworkScreen(
    navController: NavController,
    networkViewModel: NetworkViewModel,
    userViewModel: UserManagementViewModel
) {
    val currentUser by userViewModel.currentUser.collectAsStateWithLifecycle()
    val uiState by networkViewModel.uiState.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        userViewModel.refreshCurrentUserFromCloud()
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is NetworkState.Success -> {
                scope.launch { snackbarHostState.showSnackbar(state.message) }
                userViewModel.refreshCurrentUserFromCloud()
                networkViewModel.resetState()
            }
            is NetworkState.Error -> {
                scope.launch { snackbarHostState.showSnackbar(state.message) }
                networkViewModel.resetState()
            }
            else -> {}
        }
    }

    AzuraScreen(
        title = "Jaringan Sosial",
        content = {
            Box(modifier = Modifier.fillMaxSize()) {
                NetworkContent(
                    currentUser = currentUser,
                    uiState = uiState,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    onRefresh = { userViewModel.refreshCurrentUserFromCloud() },
                    onAcceptFriend = { friendId -> currentUser?.userId?.let { networkViewModel.acceptFriendRequest(it, friendId) } },
                    onRejectFriend = { friendId -> currentUser?.userId?.let { networkViewModel.rejectFriendRequest(it, friendId) } },
                    onSearchUser = { query -> networkViewModel.searchUserByEmail(query) },
                    onAddFriend = { myId, myName, myEmail, targetEmail ->
                        networkViewModel.sendFriendRequest(myId, myName, myEmail, targetEmail)
                    },
                    onViewTargetUserClasses = { friendId, name, email ->
                        userViewModel.setTargetUser(friendId, name, email)
                        navController.navigate(Screen.MyAssignedClass.createRoute(targetUserId = friendId))
                    },
                    onQueryChange = {}
                )

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    )
}
