package com.azuratech.azuratime.ui.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.core.util.showToast
import com.azuratech.azuratime.ui.core.designsystem.AzuraAuditTrail
import com.azuratech.azuratime.ui.core.designsystem.AzuraDropdownField
import com.azuratech.azuratime.ui.core.designsystem.AzuraButton
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.core.designsystem.AzuraTextField
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun EditUserScreen(
    faceId: String,
    onNavigateBack: () -> Unit,
    viewModel: StudentFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showFaceCapture by remember { mutableStateOf(false) }
    var isClassExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(faceId) {
        viewModel.loadStudentForEdit(faceId)
    }

    val selectedClassName = remember(uiState.selectedClassId, uiState.availableClasses) {
        uiState.availableClasses.find { it.id == uiState.selectedClassId }?.name ?: "Pilih Kelas..."
    }

    LaunchedEffect(uiState.formError) {
        uiState.formError?.let {
            context.showToast("Error: $it")
        }
    }

    EditUserContent(
        uiState = uiState,
        selectedClassName = selectedClassName,
        onNameChange = { viewModel.onNameChange(it) },
        onClassSelected = { viewModel.onClassSelected(it) },
        onCaptureEmbedding = { showFaceCapture = true },
        onCapturePhoto = { showFaceCapture = true },
        onUploadPhoto = { /* TODO */ },
        onSubmit = {
            viewModel.saveStudent { message ->
                context.showToast(message)
                onNavigateBack()
            }
        },
        isClassExpanded = isClassExpanded,
        onExpandedChange = { isClassExpanded = it }
    )

    if (showFaceCapture) {
        FaceCaptureScreen(
            mode = CaptureMode.EMBEDDING,
            onClose = { showFaceCapture = false },
            onEmbeddingCaptured = { embedding ->
                viewModel.onFaceCaptured(uiState.capturedBitmap!!, embedding)
                showFaceCapture = false
            },
            onPhotoCaptured = { bitmap ->
                viewModel.onPhotoCaptured(bitmap)
                showFaceCapture = false
            }
        )
    }
}

@Composable
fun EditUserContent(
    uiState: StudentFormUiState,
    selectedClassName: String,
    onNameChange: (String) -> Unit,
    onClassSelected: (String) -> Unit,
    onCaptureEmbedding: () -> Unit,
    onCapturePhoto: () -> Unit,
    onUploadPhoto: () -> Unit,
    onSubmit: () -> Unit,
    isClassExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    AzuraScreen(title = uiState.pageTitle, onBack = { /* Handled by Nav */ }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
        ) {
            Spacer(modifier = Modifier.height(AzuraSpacing.sm))

            AzuraDropdownField(
                label = "Tentukan Kelas",
                selectedValue = selectedClassName,
                options = uiState.availableClasses,
                isExpanded = isClassExpanded,
                onExpandedChange = onExpandedChange,
                onOptionSelected = {
                    onClassSelected(it.id)
                    onExpandedChange(false)
                },
                onEditClicked = {},
                getOptionLabel = { it.name }
            )

            AzuraCard(title = "Informasi Siswa") {
                Column(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)) {
                    AzuraTextField(
                        value = uiState.name,
                        onValueChange = onNameChange,
                        label = "Nama Lengkap *",
                        errorText = if (uiState.name.isBlank()) "Nama tidak boleh kosong" else null,
                        leadingIcon = { Icon(Icons.Default.Person, null) }
                    )

                    AzuraTextField(
                        value = uiState.studentId,
                        onValueChange = {},
                        label = "Student ID",
                        readOnly = true,
                        enabled = false
                    )
                }
            }

            AzuraCard(title = "Biometrik & Foto") {
                Column(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)) {
                    if (uiState.embedding == null) {
                        AzuraButton(
                            text = "Scan Wajah untuk Embedding",
                            onClick = onCaptureEmbedding,
                            icon = Icons.Default.CameraAlt
                        )
                    } else {
                        Text("✅ Embedding Tersimpan", color = MaterialTheme.colorScheme.primary)
                    }

                    if (uiState.capturedBitmap == null) {
                        AzuraButton(
                            text = "Ambil Foto Live",
                            onClick = onCapturePhoto,
                            icon = Icons.Default.CameraAlt
                        )
                    } else {
                        Text("✅ Foto Tersimpan", color = MaterialTheme.colorScheme.primary)
                    }

                    OutlinedButton(
                        onClick = onUploadPhoto,
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Upload, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Upload dari Galeri")
                    }
                }
            }

            AzuraAuditTrail(
                createdAt = System.currentTimeMillis(),
                createdBy = "Sistem",
                lastUpdated = System.currentTimeMillis()
            )

            Spacer(modifier = Modifier.height(AzuraSpacing.md))

            AzuraButton(
                text = "Simpan Perubahan",
                onClick = onSubmit,
                enabled = uiState.isFormValid && !uiState.isSubmitting,
                isLoading = uiState.isSubmitting,
                icon = Icons.Default.Save,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
