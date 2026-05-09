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
import com.azuratech.azuratime.domain.model.StudentProfile
import com.azuratech.azuratime.domain.model.SyncStatus
import com.azuratech.azuratime.ui.core.designsystem.AzuraButton
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuraengine.model.ClassModel
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
    val faceList by viewModel.faceList.collectAsStateWithLifecycle()
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

            data.studentForDeletion?.let { studentId ->
                val studentName = faceList.find { it.studentId == studentId }?.name ?: "Siswa"
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
                faceList = faceList,
                searchQuery = data.searchQuery,
                onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                onQuickEdit = { profile -> viewModel.onEditStudentClicked(profile) },
                onFullEdit = onEditUser,
                onManageClasses = { profile -> 
                    targetStudentId = profile.studentId
                    showClassPicker = true
                },
                onDelete = { profile -> 
                    viewModel.requestDeleteStudent(profile.studentId)
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
    faceList: List<StudentProfile>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onQuickEdit: (StudentProfile) -> Unit,
    onFullEdit: (String) -> Unit,
    onManageClasses: (StudentProfile) -> Unit,
    onDelete: (StudentProfile) -> Unit
) {
    AzuraScreen(
        title = "Manajemen Personil",
        content = {
            Column(modifier = Modifier.fillMaxSize()) {
                AzuraTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    label = "Cari nama...",
                    modifier = Modifier.fillMaxWidth().padding(top = AzuraSpacing.md),
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )

                Spacer(modifier = Modifier.height(AzuraSpacing.md))

                if (faceList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada pengguna ditemukan", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(faceList, key = { it.studentId }) { profile ->
                            FaceListItemCard(
                                profile = profile,
                                onQuickEdit = { onQuickEdit(profile) },
                                onFullEdit = { onFullEdit(profile.studentId) },
                                onManageClasses = { onManageClasses(profile) },
                                onDelete = { onDelete(profile) }
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
    profile: StudentProfile,
    onQuickEdit: () -> Unit,
    onFullEdit: () -> Unit,
    onDelete: () -> Unit,
    onManageClasses: () -> Unit
) {
    val isUnassigned = profile.classIds.isEmpty()
    var showMenu by remember { mutableStateOf(false) }

    AzuraCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnassigned) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        content = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FaceAvatar(photoPath = profile.photoUrl, size = 64)
                Spacer(modifier = Modifier.width(AzuraSpacing.md))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (profile.syncStatus != SyncStatus.SYNCED) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.CloudOff, 
                                contentDescription = "Belum tersinkronisasi",
                                modifier = Modifier.size(14.dp),
                                tint = Color.Gray
                            )
                        }
                    }

                    Text(
                        text = if (isUnassigned) "Belum ada kelas" else "Terdaftar di ${profile.classIds.size} kelas",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUnassigned) Color.Red else MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )

                    Text(
                        text = if (profile.faceExists) "📷 Biometric Ready" else "❌ No Photo",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (profile.faceExists) Color(0xFF2E7D32) else Color.Red
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
                faceList = PreviewMocks.mockFaceListData.students.map { it.faceWithDetails.toProfile() },
                searchQuery = "",
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
                faceList = emptyList(),
                searchQuery = "",
                onSearchQueryChanged = {},
                onQuickEdit = {},
                onFullEdit = {},
                onManageClasses = {},
                onDelete = {}
            )
        }
    }
}

// Helper for preview
private fun com.azuratech.azuratime.data.local.FaceWithDetails.toProfile() = StudentProfile(
    studentId = face.studentId ?: face.faceId,
    name = face.name,
    schoolId = face.schoolId,
    photoUrl = face.photoUrl,
    faceId = face.faceId
)

