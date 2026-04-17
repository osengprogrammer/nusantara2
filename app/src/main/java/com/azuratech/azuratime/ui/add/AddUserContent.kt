package com.azuratech.azuratime.ui.add

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.ui.core.designsystem.AzuraButton
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.core.designsystem.AzuraTextField
import com.azuratech.azuratime.ui.core.designsystem.AzuraDropdownField
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun AddUserContent(
    uiState: StudentFormUiState,
    onNameChange: (String) -> Unit,
    onStudentIdChange: (String) -> Unit,
    onClassSelected: (String) -> Unit,
    onCaptureEmbedding: () -> Unit,
    onCapturePhoto: () -> Unit,
    onUploadPhoto: () -> Unit,
    onSubmit: () -> Unit,
    onFlipCamera: () -> Unit, // Added for consistency with a potential capture flow
    isClassExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedClassName = remember(uiState.selectedClassId, uiState.availableClasses) {
        uiState.availableClasses.find { it.id == uiState.selectedClassId }?.name ?: "Pilih Kelas..."
    }

    AzuraScreen(
        title = uiState.pageTitle,
        content = {
            Column(
                modifier = modifier
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
                            onValueChange = onStudentIdChange,
                            label = "Student ID *",
                            errorText = if (uiState.studentId.isBlank()) "Student ID tidak boleh kosong" else null
                        )
                    }
                }

                AzuraCard(title = "Biometrik & Foto") {
                    Column(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)) {
                        // Face Capture Logic (Simplified for the atom view)
                        if (uiState.embedding == null) {
                            AzuraButton(
                                text = "Scan Wajah untuk Embedding",
                                onClick = onCaptureEmbedding,
                                icon = Icons.Default.CameraAlt
                            )
                        } else {
                            Text("✅ Embedding Berhasil Diambil", color = MaterialTheme.colorScheme.primary)
                        }

                        if (uiState.capturedBitmap == null) {
                            AzuraButton(
                                text = "Ambil Foto Live",
                                onClick = onCapturePhoto,
                                icon = Icons.Default.CameraAlt
                            )
                        } else {
                            Text("✅ Foto Berhasil Diambil", color = MaterialTheme.colorScheme.primary)
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

                Spacer(modifier = Modifier.height(AzuraSpacing.md))

                AzuraButton(
                    text = "Daftarkan Siswa",
                    onClick = onSubmit,
                    enabled = uiState.isFormValid && !uiState.isSubmitting,
                    isLoading = uiState.isSubmitting,
                    icon = Icons.Default.PersonAdd,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    )
}
