package com.azuratech.azuratime.ui.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.ui.core.designsystem.AzuraButton
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.core.designsystem.AzuraTextField
import com.azuratech.azuratime.ui.core.designsystem.FaceAvatar
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun UserProfileScreen(
    userViewModel: UserManagementViewModel,
    workspaceViewModel: WorkspaceViewModel,
    onBack: () -> Unit
) {
    val user by userViewModel.currentUser.collectAsState()
    val activeSchoolId = user?.activeSchoolId
    val activeMembership = activeSchoolId?.let { user?.memberships?.get(it) }
    val role = activeMembership?.role ?: "—"
    val schoolName = activeMembership?.schoolName ?: "—"
    val isAdmin = role == "ADMIN"

    var editingName by remember { mutableStateOf(false) }
    var editingSchool by remember { mutableStateOf(false) }
    var nameInput by remember(user?.name) { mutableStateOf(user?.name ?: "") }
    var schoolInput by remember(schoolName) { mutableStateOf(schoolName) }
    var snackMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackMessage) {
        snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackMessage = null
        }
    }

    UserProfileContent(
        user = user,
        role = role,
        schoolName = schoolName,
        isAdmin = isAdmin,
        editingName = editingName,
        editingSchool = editingSchool,
        nameInput = nameInput,
        schoolInput = schoolInput,
        onToggleNameEdit = { editingName = !editingName },
        onToggleSchoolEdit = { editingSchool = !editingSchool },
        onSaveName = { newName ->
            if (newName.isBlank()) {
                snackMessage = "Nama tidak boleh kosong"
            } else {
                userViewModel.updateDisplayName(newName,
                    onSuccess = {
                        editingName = false
                        snackMessage = "Nama berhasil diperbarui"
                    },
                    onError = { snackMessage = it }
                )
            }
        },
        onCancelNameEdit = {
            nameInput = user?.name ?: ""
            editingName = false
        },
        onSaveSchool = { newSchool ->
            if (newSchool.isBlank()) {
                snackMessage = "Nama sekolah tidak boleh kosong"
            } else {
                val userId = user?.userId
                val schoolId = activeSchoolId
                if (userId != null && schoolId != null) {
                    workspaceViewModel.updateSchoolName(schoolId, userId, newSchool,
                        onSuccess = {
                            editingSchool = false
                            snackMessage = "Nama sekolah berhasil diperbarui"
                        },
                        onError = { snackMessage = it }
                    )
                }
            }
        },
        onCancelSchoolEdit = {
            schoolInput = schoolName
            editingSchool = false
        },
        onNameInputChange = { nameInput = it },
        onSchoolInputChange = { schoolInput = it }
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
fun UserProfileContent(
    user: com.azuratech.azuraengine.model.User?,
    role: String,
    schoolName: String,
    isAdmin: Boolean,
    editingName: Boolean,
    editingSchool: Boolean,
    nameInput: String,
    schoolInput: String,
    onToggleNameEdit: () -> Unit,
    onToggleSchoolEdit: () -> Unit,
    onSaveName: (String) -> Unit,
    onCancelNameEdit: () -> Unit,
    onSaveSchool: (String) -> Unit,
    onCancelSchoolEdit: () -> Unit,
    onNameInputChange: (String) -> Unit,
    onSchoolInputChange: (String) -> Unit
) {
    AzuraScreen(
        title = "Profil Saya",
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = AzuraSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
            ) {
                FaceAvatar(photoPath = null, size = 96)

                Text(
                    text = user?.email ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(AzuraSpacing.sm))

                AzuraCard(title = "Nama Tampilan") {
                    if (editingName) {
                        Column(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                            AzuraTextField(
                                value = nameInput,
                                onValueChange = onNameInputChange,
                                label = "Nama Baru"
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                                OutlinedButton(
                                    onClick = onCancelNameEdit,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Batal") }
                                AzuraButton(
                                    text = "Simpan",
                                    onClick = { onSaveName(nameInput) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = user?.name ?: "—", style = MaterialTheme.typography.bodyLarge)
                            IconButton(onClick = onToggleNameEdit) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit nama", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                AzuraCard(title = "Workspace Aktif") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(AzuraSpacing.sm))
                        Text(text = "Workspace Aktif", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(AzuraSpacing.sm))

                    if (editingSchool && isAdmin) {
                        Column(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                            AzuraTextField(
                                value = schoolInput,
                                onValueChange = onSchoolInputChange,
                                label = "Nama sekolah baru"
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                                OutlinedButton(
                                    onClick = onCancelSchoolEdit,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Batal") }
                                AzuraButton(
                                    text = "Simpan",
                                    onClick = { onSaveSchool(schoolInput) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = schoolName, style = MaterialTheme.typography.bodyLarge)
                                Text(text = role, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            if (isAdmin) {
                                IconButton(onClick = onToggleSchoolEdit) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit nama sekolah", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
