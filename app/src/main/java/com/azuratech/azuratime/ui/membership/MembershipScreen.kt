package com.azuratech.azuratime.ui.membership

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel // 🔥 Gunakan Hilt
import com.azuratech.azuratime.core.boot.BootViewModel

// 🔥 Azura Design System Imports
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.theme.AzuraShapes

@Composable
fun MembershipScreen(
    email: String,
    onApproved: () -> Unit,
    onLogout: () -> Unit
    // ❌ FIX: Parameter factory dihapus
) {
    // 🔥 FIX: Inisialisasi menggunakan Hilt
    val membershipViewModel: MembershipViewModel = hiltViewModel()
    val bootViewModel: BootViewModel = hiltViewModel()

    val state by membershipViewModel.state.collectAsState()
    val memberships by membershipViewModel.memberships.collectAsState()

    LaunchedEffect(email) {
        membershipViewModel.checkMembership(email)
    }

    LaunchedEffect(state) {
        if (state is MembershipState.Approved) {
            bootViewModel.recheck()
            onApproved()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val currentState = state
            
            if (currentState is MembershipState.Loading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else if (memberships != null && memberships!!.isEmpty()) {
                EmptyMembershipView(
                    onJoinByCode = { /* Open Dialog/Bottom Sheet */ },
                    onSearch = { /* Navigate to SchoolSearchScreen Stub */ }
                )
            } else {
                when (currentState) {
                    is MembershipState.Pending -> {
                        PendingView(email = email, onLogout = onLogout)
                    }
                    is MembershipState.Rejected -> {
                        RejectedView(reason = currentState.reason, onLogout = onLogout)
                    }
                    is MembershipState.Error -> {
                        ErrorView(
                            message = currentState.message,
                            onRetry = { membershipViewModel.checkMembership(email) },
                            onLogout = onLogout
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

// ==========================================
// 🎨 AZURA-STYLED COMPONENTS
// ==========================================

@Composable
fun EmptyMembershipView(onJoinByCode: () -> Unit, onSearch: () -> Unit) {
    Column(
        modifier = Modifier.padding(AzuraSpacing.xl).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.School, 
            null, 
            modifier = Modifier.size(100.dp), 
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(AzuraSpacing.lg))
        Text(
            "Anda belum bergabung ke sekolah manapun", 
            style = MaterialTheme.typography.headlineSmall, 
            fontWeight = FontWeight.Bold, 
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AzuraSpacing.md))
        Text(
            "Minta kode undangan dari admin sekolah, atau cari sekolah yang tersedia.", 
            style = MaterialTheme.typography.bodyMedium, 
            textAlign = TextAlign.Center, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(AzuraSpacing.xl))

        Button(
            onClick = onJoinByCode, 
            modifier = Modifier.fillMaxWidth(),
            shape = AzuraShapes.medium
        ) {
            Text("Masukkan Kode Sekolah")
        }
        Spacer(modifier = Modifier.height(AzuraSpacing.md))
        OutlinedButton(
            onClick = onSearch, 
            modifier = Modifier.fillMaxWidth(),
            shape = AzuraShapes.medium
        ) {
            Text("Cari Sekolah")
        }
    }
}

@Composable
fun PendingView(email: String, onLogout: () -> Unit) {
    Column(
        modifier = Modifier.padding(AzuraSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(AzuraSpacing.xl))

        TextButton(onClick = onLogout) {
            Text("Bukan akun Anda? Ganti Akun")
        }
    }
}

@Composable
fun RejectedView(reason: String?, onLogout: () -> Unit) {
    Column(
        modifier = Modifier.padding(AzuraSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Block, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(AzuraSpacing.lg))
        Text("Akses Ditolak", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(AzuraSpacing.md))
        Text(reason ?: "Akun ini tidak memiliki akses ke dalam sistem. Silakan hubungi administrator.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(AzuraSpacing.xl))

        Button(onClick = onLogout, shape = AzuraShapes.medium, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
            Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(AzuraSpacing.sm))
            Text("Keluar")
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit, onLogout: () -> Unit) {
    Column(
        modifier = Modifier.padding(AzuraSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(AzuraSpacing.lg))
        Text("Terjadi Kesalahan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(AzuraSpacing.md))
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(AzuraSpacing.xl))

        Row(horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.md)) {
            OutlinedButton(onClick = onLogout, shape = AzuraShapes.medium) { Text("Keluar") }
            Button(onClick = onRetry, shape = AzuraShapes.medium) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(AzuraSpacing.sm))
                Text("Coba Lagi")
            }
        }
    }
}