package com.azuratech.azuratime.core.ui.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.features.school.ui.list.SchoolViewModel
import com.azuratech.azuratime.features.account.ui.components.WorkspaceViewModel

@Composable
fun WorkspaceSelector(
    schoolViewModel: SchoolViewModel,
    workspaceViewModel: WorkspaceViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    val schoolUiState by schoolViewModel.uiState.collectAsStateWithLifecycle()
    val schools = schoolUiState.schools
    val activeSchoolId = schoolUiState.activeSchoolId
    val activeSchool = schools.find { it.id == activeSchoolId }

    val workspaceUiState by workspaceViewModel.uiState.collectAsStateWithLifecycle()

    // Hide only when no schools are available AND no active school is set
    if (schools.isEmpty() && activeSchoolId == null) return

    val activeSchoolName = activeSchool?.name ?: if (activeSchoolId != null) "Syncing..." else "Pilih Workspace"

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        // 🔘 Anchor button
        OutlinedButton(
            onClick = { if (schools.isNotEmpty()) expanded = true },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            enabled = schools.isNotEmpty() || activeSchoolId != null,
        ) {
            if (activeSchool == null && activeSchoolId != null) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(text = activeSchoolName, maxLines = 1)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Ganti Workspace")
        }

        // 📋 Dropdown
        if (schools.isNotEmpty()) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                schools.forEach { school ->
                    val isActive = school.id == activeSchoolId
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = school.name,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                        },
                        onClick = {
                            expanded = false
                            if (!isActive) {
                                schoolViewModel.onEvent(com.azuratech.azuratime.features.school.ui.list.SchoolUiEvent.SelectSchool(school))
                            }
                        },
                        // Show a checkmark on the active item
                        trailingIcon = if (isActive) {
                            (
                                {
                                    Text("✓", color = MaterialTheme.colorScheme.primary)
                                }
                                )
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    // ⏳ Blocking loading overlay while the switch is in progress
    if (workspaceUiState is WorkspaceViewModel.WorkspaceState.Switching) {
        Dialog(onDismissRequest = { /* Prevent dismiss during switch */ }) {
            Card(
                modifier = Modifier.padding(16.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator()
                    Text("Menyiapkan Workspace...", fontWeight = FontWeight.Bold)
                    Text(
                        text = "Sedang menyinkronkan data biometrik dan absensi.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    // 🛠 Reset state once done
    LaunchedEffect(workspaceUiState) {
        when (workspaceUiState) {
            is WorkspaceViewModel.WorkspaceState.Success,
            is WorkspaceViewModel.WorkspaceState.Error,
            -> workspaceViewModel.resetState()
            else -> {}
        }
    }
}
