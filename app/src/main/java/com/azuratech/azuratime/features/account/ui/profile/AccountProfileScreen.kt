package com.azuratech.azuratime.features.account.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.StudentAvatar
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.features.account.ui.management.AccountManagementViewModel
import com.azuratech.azuratime.features.account.ui.components.WorkspaceViewModel

@Composable
fun AccountProfileScreen(
    userViewModel: AccountManagementViewModel,
    @Suppress("UNUSED_PARAMETER") workspaceViewModel: WorkspaceViewModel,
    onNavigateBack: () -> Unit,
) {
    val user by userViewModel.currentUser.collectAsStateWithLifecycle()

    AzuraScreen(
        title = "Profil Saya",
        onBack = onNavigateBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = AzuraSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
        ) {
            StudentAvatar(photoPath = null, size = 96.dp)

            Text(
                text = user?.email ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )

            Spacer(modifier = Modifier.height(AzuraSpacing.sm))

            // Other profile details
        }
    }
}
