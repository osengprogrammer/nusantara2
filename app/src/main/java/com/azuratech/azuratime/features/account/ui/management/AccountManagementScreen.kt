package com.azuratech.azuratime.features.account.ui.management

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.designsystem.AzuraButton
import com.azuratech.azuratime.core.ui.designsystem.StudentAvatar
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing

/**
 * 👤 ACCOUNT MANAGEMENT SCREEN (v3.2.0-ai-native)
 */
@Composable
fun AccountManagementScreen(
    viewModel: AccountManagementViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()

    AccountManagementContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun AccountManagementContent(
    uiState: AccountUiState,
    onEvent: (AccountUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
) {
    AzuraScreen(
        title = "Pengaturan Akun",
        onBack = onNavigateBack,
    ) {
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

                AzuraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(AzuraSpacing.md)) {
                        Text("Pilih Kelas Aktif", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(AzuraSpacing.sm))

                        if (uiState.availableClasses.isEmpty()) {
                            Text(
                                "Belum ada kelas tersedia di sekolah ini.",
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

                Spacer(modifier = Modifier.weight(1f))

                AzuraButton(
                    text = "Logout",
                    onClick = { onEvent(AccountUiEvent.Logout) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                )
            }
        }

        uiState.error?.let { error ->
            AlertDialog(
                onDismissRequest = { onEvent(AccountUiEvent.ClearError) },
                title = { Text("Terjadi Kesalahan") },
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewAccountLoading() {
    MaterialTheme {
        AccountManagementContent(
            uiState = AccountPreviewMocks.loading(),
            onEvent = {},
            onNavigateBack = {},
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
        )
    }
}
