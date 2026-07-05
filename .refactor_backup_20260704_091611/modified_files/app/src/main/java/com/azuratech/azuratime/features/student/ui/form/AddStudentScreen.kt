package com.azuratech.azuratime.features.student.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraAccountFormContent
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.CoreFaceCamera
import com.azuratech.azuratime.core.ui.designsystem.PermissionsHandler
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.util.showToast
import com.azuratech.azuratime.ml.detector.FaceAnalyzer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class)
@Composable
fun AddStudentScreen(
    onNavigateBack: () -> Unit,
    viewModel: StudentFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showCamera by remember { mutableStateOf(false) }
    var captureMode by remember { mutableStateOf("EMBEDDING") } // "EMBEDDING" or "PHOTO"
    var triggerCapture by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    // 🔥 AI Native: Collect and Handle UI Effects
    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                StudentFormUiEffect.NavigateBack -> {
                    onNavigateBack()
                }
                is StudentFormUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is StudentFormUiEffect.ShowToast -> {
                    context.showToast(effect.message)
                }
            }
        }
    }

    AzuraScreen(
        title = uiState.pageTitle,
        onBack = { viewModel.onEvent(StudentFormUiEvent.NavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(AzuraSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
            ) {
                AzuraAccountFormContent(
                    name = uiState.profile.name,
                    onNameChange = { viewModel.onEvent(StudentFormUiEvent.UpdateField("name", it)) },
                    faceId = uiState.profile.studentId,
                    onfaceIdChange = { viewModel.onEvent(StudentFormUiEvent.UpdateField("studentId", it)) },
                    embedding = uiState.profile.embedding,
                    capturedBitmap = uiState.capturedBitmap,
                    onCaptureEmbedding = {
                        captureMode = "EMBEDDING"
                        showCamera = true
                        triggerCapture = false
                    },
                    onCapturePhoto = {
                        captureMode = "PHOTO"
                        showCamera = true
                        triggerCapture = false
                    },
                    onUploadPhoto = { },
                    isSubmitting = uiState.isSubmitting,
                    isSubmitEnabled = uiState.isFormValid,
                    onSubmit = { viewModel.onEvent(StudentFormUiEvent.SubmitForm) },
                    submitText = if (uiState.isEditMode) "Save Changes" else "Register Student",
                    additionalFields = {
                        Text(
                            text = "Select Class *",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = AzuraSpacing.xs),
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            uiState.availableClasses.forEach { classModel ->
                                val isSelected = uiState.profile.classIds.contains(classModel.id)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onEvent(StudentFormUiEvent.ToggleClass(classModel.id)) },
                                    label = { Text(classModel.name) },
                                    modifier = Modifier.padding(end = AzuraSpacing.xs),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(AzuraSpacing.sm))

                        uiState.validationErrors["classId"]?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = AzuraSpacing.sm, top = AzuraSpacing.xs),
                            )
                        }
                    },
                )
            }

            if (showCamera) {
                PermissionsHandler(permissionState = cameraPermissionState) {
                    val analyzer = remember {
                        FaceAnalyzer(
                            isFrontCamera = true,
                            bypassLiveness = true,
                            onFaceEmbedding = { _, embedding ->
                                if (triggerCapture && captureMode == "EMBEDDING") {
                                    viewModel.onEvent(
                                        StudentFormUiEvent.FaceCaptured(
                                            uiState.capturedBitmap ?: android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888), // Placeholder if bitmap not yet captured
                                            embedding,
                                        ),
                                    )
                                    showCamera = false
                                }
                            },
                            onFaceCaptured = { bitmap ->
                                if (triggerCapture) {
                                    val config = bitmap.config ?: android.graphics.Bitmap.Config.ARGB_8888
                                    val bitmapCopy = bitmap.copy(config, false)

                                    if (captureMode == "PHOTO") {
                                        viewModel.onEvent(StudentFormUiEvent.PhotoCaptured(bitmapCopy))
                                        showCamera = false
                                    } else {
                                        // If EMBEDDING mode, we keep bitmap for FaceCaptured but wait for embedding
                                        // This is a bit tricky with current FaceAnalyzer callback structure
                                        // For now, let's assume embedding comes shortly after or we store it
                                    }
                                }
                            },
                            onLivenessStatus = { },
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        CoreFaceCamera(
                            analyzer = analyzer,
                            modifier = Modifier.fillMaxSize(),
                        )

                        Button(
                            onClick = { triggerCapture = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AzuraSpacing.lg)
                                .align(androidx.compose.ui.Alignment.BottomCenter),
                        ) {
                            Text("Capture ${if (captureMode == "EMBEDDING") "Face" else "Photo"}")
                        }

                        IconButton(
                            onClick = { showCamera = false },
                            modifier = Modifier.padding(AzuraSpacing.md),
                        ) {
                            Icon(imageVector = androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
            }
        }
    }
}
