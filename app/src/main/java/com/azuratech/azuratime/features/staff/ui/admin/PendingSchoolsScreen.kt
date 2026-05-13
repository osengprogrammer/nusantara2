package com.azuratech.azuratime.ui.admin

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
import com.azuratech.azuraengine.model.School
import com.azuratech.azuratime.ui.core.UiEvent
import com.azuratech.azuratime.ui.core.designsystem.AzuraButton
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PendingSchoolsScreen(
    onBack: () -> Unit,
    viewModel: PendingSchoolsViewModel = hiltViewModel()
) {
    val pendingSchools by viewModel.pendingSchools.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var schoolToReject by remember { mutableStateOf<School?>(null) }
    var rejectionReason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    AzuraScreen(
        title = "Persetujuan Sekolah",
        onBack = onBack,
        snackbarHostState = snackbarHostState
    ) {
        if (pendingSchools.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Tidak ada sekolah yang menunggu persetujuan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AzuraSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
            ) {
                items(pendingSchools, key = { it.id }) { school ->
                    SchoolApprovalCard(
                        school = school,
                        onApprove = { viewModel.approve(school.id) },
                        onReject = { schoolToReject = school }
                    )
                }
            }
        }
    }

    if (schoolToReject != null) {
        AlertDialog(
            onDismissRequest = { schoolToReject = null; rejectionReason = "" },
            title = { Text("Tolak Pendaftaran Sekolah") },
            text = {
                Column {
                    Text("Berikan alasan penolakan untuk '${schoolToReject?.name}':")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Contoh: Data tidak valid") },
                        shape = AzuraShapes.medium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        schoolToReject?.let { viewModel.reject(it.id, rejectionReason) }
                        schoolToReject = null
                        rejectionReason = ""
                    },
                    enabled = rejectionReason.isNotBlank()
                ) {
                    Text("Tolak", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { schoolToReject = null; rejectionReason = "" }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun SchoolApprovalCard(
    school: School,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val dateString = remember(school.createdAt) { dateFormatter.format(Date(school.createdAt)) }

    AzuraCard {
        Column(modifier = Modifier.padding(AzuraSpacing.md)) {
            Text(
                text = school.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Didaftar oleh: ${school.accountId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "Pada: $dateString",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            
            Spacer(modifier = Modifier.height(AzuraSpacing.md))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = AzuraShapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tolak")
                }
                
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    shape = AzuraShapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) // Green 800
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Setujui")
                }
            }
        }
    }
}
