package com.azuratech.azuratime.ui.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.ui.core.designsystem.AzuraButton
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.core.designsystem.AzuraTextField
import com.azuratech.azuratime.ui.core.designsystem.FaceAvatar
import com.azuratech.azuratime.ui.core.designsystem.MultiClassAssignmentDialog
import com.azuratech.azuratime.ui.core.designsystem.QuickEditFaceDialog
import com.azuratech.azuratime.ui.core.preview.AzuraPreviews
import com.azuratech.azuratime.ui.core.preview.PreviewMocks
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.theme.AzuraTheme


@Composable
fun FaceListScreen(
    viewModel: FaceListViewModel = hiltViewModel(),
    onEditUser: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allClasses by viewModel.allClasses.collectAsStateWithLifecycle()

    var showClassPicker by remember { mutableStateOf(false) }
    var targetStudentId by remember { mutableStateOf<String?>(null) }

    when (val state = uiState) {
        is FaceListUiState.Loading -> {
            AzuraScreen(title = "Manajemen Personil") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        is FaceListUiState.Success -> {
            val data = state.data
            // Dialogs handled in the Screen wrapper
            data.studentForQuickEdit?.let { faceWithDetails ->
                QuickEditFaceDialog(
                    face = faceWithDetails.face,
                    onDismiss = { viewModel.onDismissDialog() },
                    onSave = { updatedFace -> viewModel.onSaveChanges(updatedFace) }
                )
            }

            data.studentForClassAssignment?.let { student ->
                val studentDisplayItem = data.students.find { it.faceWithDetails.face.faceId == student.faceId }
                MultiClassAssignmentDialog(
                    studentName = student.name,
                    allClasses = data.allClasses,
                    assignedClassIds = studentDisplayItem?.assignedClassIds ?: emptyList(),
                    onDismiss = { viewModel.onDismissDialog() },
                    onToggle = { classId, isChecked ->
                        viewModel.onToggleStudentClassAssignment(student.faceId, classId, isChecked)
                    }
                )
            }

            data.studentForDeletion?.let { studentId ->
                val studentName = data.students.find { it.faceWithDetails.face.studentId == studentId }?.faceWithDetails?.face?.name ?: "Siswa"
                AlertDialog(
                    onDismissRequest = { viewModel.cancelDeleteStudent() },
                    title = { Text("Konfirmasi Hapus") },
                    text = { Text("Apakah Anda yakin ingin menghapus $studentName? Data absensi dan biometric akan hilang.") },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.confirmDeleteStudent() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Hapus")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.cancelDeleteStudent() }) {
                            Text("Batal")
                        }
                    }
                )
            }

            if (showClassPicker && targetStudentId != null) {
                AlertDialog(
                    onDismissRequest = { showClassPicker = false },
                    title = { Text("Pilih Kelas Baru") },
                    text = {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                            items(allClasses) { classModel ->
                                TextButton(
                                    onClick = {
                                        viewModel.onAssignStudentToClass(targetStudentId!!, classModel.id)
                                        showClassPicker = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(classModel.name, textAlign = androidx.compose.ui.text.style.TextAlign.Start, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showClassPicker = false }) { Text("Tutup") }
                    }
                )
            }

            FaceListContent(
                data = data,
                onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                onQuickEdit = { faceWithDetails -> viewModel.onEditStudentClicked(faceWithDetails) },
                onFullEdit = onEditUser,
                onManageClasses = { face -> 
                    targetStudentId = face.studentId
                    showClassPicker = true
                },
                onDelete = { face -> 
                    face.studentId?.let { viewModel.requestDeleteStudent(it) }
                }
            )
        }
        is FaceListUiState.Error -> {
            AzuraScreen(title = "Manajemen Personil") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun FaceListContent(
    data: FaceListData,
    onSearchQueryChanged: (String) -> Unit,
    onQuickEdit: (com.azuratech.azuratime.data.local.FaceWithDetails) -> Unit,
    onFullEdit: (String) -> Unit,
    onManageClasses: (com.azuratech.azuratime.data.local.FaceEntity) -> Unit,
    onDelete: (com.azuratech.azuratime.data.local.FaceEntity) -> Unit
) {
    AzuraScreen(
        title = "Manajemen Personil",
        content = {
            Column(modifier = Modifier.fillMaxSize()) {
                AzuraTextField(
                    value = data.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    label = "Cari nama...",
                    modifier = Modifier.fillMaxWidth().padding(top = AzuraSpacing.md),
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )

                Spacer(modifier = Modifier.height(AzuraSpacing.md))

                if (data.students.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada pengguna ditemukan", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(data.students, key = { it.faceWithDetails.face.faceId }) { student ->
                            FaceListItemCard(
                                student = student,
                                onQuickEdit = { onQuickEdit(student.faceWithDetails) },
                                onFullEdit = { onFullEdit(student.faceWithDetails.face.faceId) },
                                onManageClasses = { onManageClasses(student.faceWithDetails.face) },
                                onDelete = { onDelete(student.faceWithDetails.face) }
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun FaceListItemCard(
    student: StudentDisplayItem,
    onQuickEdit: () -> Unit,
    onFullEdit: () -> Unit,
    onDelete: () -> Unit,
    onManageClasses: () -> Unit
) {
    val isUnassigned = student.assignedClassNames == "Belum ada kelas"
    var showMenu by remember { mutableStateOf(false) }

    AzuraCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnassigned) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        content = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FaceAvatar(photoPath = student.faceWithDetails.face.photoUrl, size = 64)
                Spacer(modifier = Modifier.width(AzuraSpacing.md))

                Column(modifier = Modifier.weight(1f)) {
                    Text(student.faceWithDetails.face.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Text(
                        text = student.assignedClassNames,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUnassigned) Color.Red else MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )

                    Text(
                        text = if (student.isBiometricReady) "📷 Biometric Ready" else "❌ No Photo",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (student.isBiometricReady) Color(0xFF2E7D32) else Color.Red
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opsi")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ubah Kelas") },
                            onClick = { 
                                showMenu = false
                                onManageClasses() 
                            },
                            leadingIcon = { Icon(Icons.Default.School, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Ubah Nama") },
                            onClick = { 
                                showMenu = false
                                onQuickEdit() 
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Lihat Profil") },
                            onClick = { 
                                showMenu = false
                                onFullEdit() 
                            },
                            leadingIcon = { Icon(Icons.Default.Person, null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Hapus", color = MaterialTheme.colorScheme.error) },
                            onClick = { 
                                showMenu = false
                                onDelete() 
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        }
    )
}


@AzuraPreviews
@Composable
fun FaceListContentSuccessPreview() {
    AzuraTheme {
        Surface {
            FaceListContent(
                data = PreviewMocks.mockFaceListData,
                onSearchQueryChanged = {},
                onQuickEdit = {},
                onFullEdit = {},
                onManageClasses = {},
                onDelete = {}
            )
        }
    }
}

@AzuraPreviews
@Composable
fun FaceListContentLoadingPreview() {
    AzuraTheme {
        Surface {
            FaceListContent(
                data = FaceListData(),
                onSearchQueryChanged = {},
                onQuickEdit = {},
                onFullEdit = {},
                onManageClasses = {},
                onDelete = {}
            )
        }
    }
}
