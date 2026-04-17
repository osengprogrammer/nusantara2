package com.azuratech.azuratime.ui.core.designsystem

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
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuratime.ui.user.WorkspaceViewModel

@Composable
fun WorkspaceSelector(
    currentUser: UserEntity?,
    workspaceViewModel: WorkspaceViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val uiState by workspaceViewModel.uiState.collectAsStateWithLifecycle()

    // Hide when user hasn't loaded or has no workspace
    if (currentUser == null || currentUser.memberships.isEmpty()) return

    val activeSchoolId   = currentUser.activeSchoolId
    val activeSchoolName = currentUser.memberships[activeSchoolId]?.schoolName ?: "Pilih Workspace"

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {

        // 🔘 Anchor button
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(text = activeSchoolName, maxLines = 1)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Ganti Workspace")
        }

        // 📋 Dropdown
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            currentUser.memberships.forEach { (schoolId, membership) ->
                val isActive = schoolId == activeSchoolId
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = membership.schoolName,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color     = if (isActive)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text  = "Role: ${membership.role}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        if (!isActive) {
                            workspaceViewModel.changeWorkspace(
                                userId        = currentUser.userId,
                                newSchoolId   = schoolId,
                                newSchoolName = membership.schoolName
                            )
                        }
                    },
                    // Show a checkmark on the active item
                    trailingIcon = if (isActive) ({
                        Text("✓", color = MaterialTheme.colorScheme.primary)
                    }) else null
                )
            }
        }
    }

    // ⏳ Blocking loading overlay while the switch is in progress
    if (uiState is WorkspaceViewModel.WorkspaceState.Switching) {
        Dialog(onDismissRequest = { /* Prevent dismiss during switch */ }) {
            Card(
                modifier = Modifier.padding(16.dp),
                shape    = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier              = Modifier.padding(24.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally,
                    verticalArrangement   = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Menyiapkan Workspace...", fontWeight = FontWeight.Bold)
                    Text(
                        text      = "Sedang menyinkronkan data biometrik dan absensi.",
                        style     = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // 🛠 Reset state once done
    LaunchedEffect(uiState) {
        when (uiState) {
            is WorkspaceViewModel.WorkspaceState.Success,
            is WorkspaceViewModel.WorkspaceState.Error -> workspaceViewModel.resetState()
            else -> {}
        }
    }
}
