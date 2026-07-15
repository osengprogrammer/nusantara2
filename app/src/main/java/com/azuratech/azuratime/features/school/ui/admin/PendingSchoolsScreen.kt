package com.azuratech.azuratime.features.school.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.features.school.domain.model.School
import com.azuratech.azuratime.core.ui.designsystem.AzuraCard
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PendingSchoolsScreen(
    onBack: () -> Unit,
    viewModel: PendingSchoolsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var schoolToReject by remember { mutableStateOf<School?>(null) }
    var rejectionReason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is PendingSchoolsUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    AzuraScreen(
        title = "School Approvals",
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.pendingSchools.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No schools waiting for approval.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AzuraSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
            ) {
                items(uiState.pendingSchools, key = { it.id }) { school ->
                    SchoolApprovalCard(
                        school = school,
                        onApprove = { viewModel.onEvent(PendingSchoolsUiEvent.ApproveSchool(school.id)) },
                        onReject = { schoolToReject = school },
                    )
                }
            }
        }
    }

    if (schoolToReject != null) {
        AlertDialog(
            onDismissRequest = {
                schoolToReject = null
                rejectionReason = ""
            },
            title = { Text("Reject School Registration") },
            text = {
                Column {
                    Text("Provide a rejection reason for '${schoolToReject?.name}':")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Example: Invalid data") },
                        shape = AzuraShapes.medium,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        schoolToReject?.let { viewModel.onEvent(PendingSchoolsUiEvent.RejectSchool(it.id, rejectionReason)) }
                        schoolToReject = null
                        rejectionReason = ""
                    },
                    enabled = rejectionReason.isNotBlank(),
                ) {
                    Text("Reject", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    schoolToReject = null
                    rejectionReason = ""
                }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
fun SchoolApprovalCard(
    school: School,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val dateString = remember(school.createdAt) { dateFormatter.format(Date(school.createdAt)) }

    AzuraCard {
        Column(modifier = Modifier.padding(AzuraSpacing.md)) {
            Text(
                text = school.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Registered by: ${school.accountId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = "At: $dateString",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )

            Spacer(modifier = Modifier.height(AzuraSpacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = AzuraShapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reject")
                }

                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    shape = AzuraShapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), // Green 800
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Approve")
                }
            }
        }
    }
}
