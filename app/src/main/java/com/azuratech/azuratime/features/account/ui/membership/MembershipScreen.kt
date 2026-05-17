package com.azuratech.azuratime.features.account.ui.membership

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.boot.BootViewModel
import com.azuratech.azuratime.features.account.domain.model.AccessRequestProfile
import com.azuratech.azuratime.core.domain.model.SyncStatus

// 🔥 Azura Design System Imports
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.ui.theme.AzuraShapes

@Composable
fun MembershipScreen(
    email: String,
    displayName: String? = null,
    onApprovedClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val membershipViewModel: MembershipViewModel = hiltViewModel()
    val bootViewModel: BootViewModel = hiltViewModel()

    val state by membershipViewModel.state.collectAsStateWithLifecycle()

    @Suppress("UNUSED_VARIABLE")
    val memberships by membershipViewModel.memberships.collectAsStateWithLifecycle()
    val accessRequests by membershipViewModel.accessRequests.collectAsStateWithLifecycle()

    LaunchedEffect(email) {
        membershipViewModel.checkMembership(email, displayName)
    }

    LaunchedEffect(state) {
        if (state is MembershipState.Approved) {
            membershipViewModel.activateMembership()
            bootViewModel.recheck()
            onApprovedClick()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val currentState = state

            if (currentState is MembershipState.Loading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                when (currentState) {
                    is MembershipState.Pending -> {
                        PendingView(
                            email = email,
                            accessRequests = accessRequests,
                            onLogoutClick = onLogoutClick,
                        )
                    }
                    is MembershipState.Rejected -> {
                        RejectedView(reason = currentState.reason, onLogoutClick = onLogoutClick)
                    }
                    is MembershipState.Error -> {
                        ErrorView(
                            message = currentState.message,
                            onRetry = { membershipViewModel.checkMembership(email, displayName) },
                            onLogoutClick = onLogoutClick,
                        )
                    }
                    else -> {
                        // For Approved or Idle state when we have data
                        PendingView(
                            email = email,
                            accessRequests = accessRequests,
                            onLogoutClick = onLogoutClick,
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 🎨 AZURA-STYLED COMPONENTS
// ==========================================

@Composable
fun PendingView(
    email: String,
    accessRequests: List<AccessRequestProfile>,
    onLogoutClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(AzuraSpacing.xl).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(AzuraSpacing.xl))
        Icon(Icons.Default.HourglassEmpty, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(AzuraSpacing.lg))
        Text("Menunggu Persetujuan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(AzuraSpacing.md))
        Text("Akun Anda sedang dalam antrean verifikasi oleh administrator. Mohon tunggu beberapa saat.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(AzuraSpacing.lg))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = AzuraShapes.medium) {
            SelectionContainer {
                Text(email, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = AzuraSpacing.lg, vertical = AzuraSpacing.sm), color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(AzuraSpacing.xl))

        if (accessRequests.isNotEmpty()) {
            Text(
                "Permintaan Bergabung:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
            )
            Spacer(modifier = Modifier.height(AzuraSpacing.sm))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
            ) {
                items(accessRequests) { request ->
                    AccessRequestItem(request)
                }
            }
        } else {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(AzuraSpacing.xl))
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(onClick = onLogoutClick) {
            Text("Bukan akun Anda? Ganti Akun")
        }
    }
}

@Composable
fun AccessRequestItem(request: AccessRequestProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(AzuraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(request.schoolName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(request.status.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            if (request.syncStatus != SyncStatus.SYNCED) {
                Icon(Icons.Default.CloudOff, contentDescription = "Unsynced", modifier = Modifier.size(16.dp), tint = Color.Gray)
            }
        }
    }
}

@Composable
fun RejectedView(reason: String?, onLogoutClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(AzuraSpacing.xl).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Block, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(AzuraSpacing.lg))
        Text("Akses Ditolak", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(AzuraSpacing.md))
        Text(reason ?: "Akun ini tidak memiliki akses ke dalam sistem. Silakan hubungi administrator.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(AzuraSpacing.xl))

        Button(onClick = onLogoutClick, shape = AzuraShapes.medium, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
            Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(AzuraSpacing.sm))
            Text("Keluar")
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit, onLogoutClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(AzuraSpacing.xl).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(AzuraSpacing.lg))
        Text("Terjadi Kesalahan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(AzuraSpacing.md))
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(AzuraSpacing.xl))

        Row(horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.md)) {
            OutlinedButton(onClick = onLogoutClick, shape = AzuraShapes.medium) { Text("Keluar") }
            Button(onClick = onRetry, shape = AzuraShapes.medium) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(AzuraSpacing.sm))
                Text("Coba Lagi")
            }
        }
    }
}
