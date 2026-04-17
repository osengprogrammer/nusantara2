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

    // Dialogs handled in the Screen wrapper
    uiState.studentForQuickEdit?.let { faceWithDetails ->
        QuickEditFaceDialog(
            face = faceWithDetails.face,
            onDismiss = { viewModel.onDismissDialog() },
            onSave = { updatedFace -> viewModel.onSaveChanges(updatedFace) }
        )
    }

    uiState.studentForClassAssignment?.let { student ->
        val studentDisplayItem = uiState.students.find { it.faceWithDetails.face.faceId == student.faceId }
        MultiClassAssignmentDialog(
            studentName = student.name,
            allClasses = uiState.allClasses,
            assignedClassIds = studentDisplayItem?.assignedClassIds ?: emptyList(),
            onDismiss = { viewModel.onDismissDialog() },
            onToggle = { classId, isChecked ->
                viewModel.onToggleStudentClassAssignment(student.faceId, classId, isChecked)
            }
        )
    }

    FaceListContent(
        uiState = uiState,
        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
        onQuickEdit = { faceWithDetails -> viewModel.onEditStudentClicked(faceWithDetails) },
        onFullEdit = onEditUser,
        onManageClasses = { face -> viewModel.onAssignClassesClicked(face) },
        onDelete = { face -> viewModel.onDeleteStudent(face) }
    )
}

@Composable
fun FaceListContent(
    uiState: FaceListUiState,
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
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    label = "Cari nama...",
                    modifier = Modifier.fillMaxWidth().padding(top = AzuraSpacing.md),
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )

                Spacer(modifier = Modifier.height(AzuraSpacing.md))

                if (uiState.students.isEmpty() && !uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada pengguna ditemukan", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(uiState.students, key = { it.faceWithDetails.face.faceId }) { student ->
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

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(8.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onQuickEdit) { Text("Nama") }
                TextButton(onClick = onFullEdit) { Text("Profil") }
                Spacer(Modifier.width(8.dp))
                AzuraButton(
                    text = "Set Kelas",
                    onClick = onManageClasses,
                    modifier = Modifier.height(36.dp),
                    icon = Icons.Default.School
                )
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
                uiState = PreviewMocks.mockFaceListStateSuccess,
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
                uiState = PreviewMocks.mockFaceListStateLoading,
                onSearchQueryChanged = {},
                onQuickEdit = {},
                onFullEdit = {},
                onManageClasses = {},
                onDelete = {}
            )
        }
    }
}
