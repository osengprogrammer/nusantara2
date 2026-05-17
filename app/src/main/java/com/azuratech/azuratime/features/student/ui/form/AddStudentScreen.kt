package com.azuratech.azuratime.features.student.ui.form

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.azuratech.azuratime.core.ui.designsystem.AzuraDropdownField
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.designsystem.AzuraUserFormContent
import com.azuratech.azuratime.core.ui.designsystem.CoreFaceCamera
import com.azuratech.azuratime.core.ui.designsystem.PermissionsHandler
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ml.detector.FaceAnalyzer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AddStudentScreen(
    onNavigateBack: () -> Unit,
    viewModel: StudentFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiStateStateFlow.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showCamera by remember { mutableStateOf(false) }
    var captureMode by remember { mutableStateOf("EMBEDDING") } // "EMBEDDING" or "PHOTO"
    var triggerCapture by remember { mutableStateOf(false) }
    var embeddingReceived by remember { mutableStateOf(false) }
    var photoReceived by remember { mutableStateOf(false) }

    var isClassExpanded by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(key1 = true) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                is com.azuratech.azuratime.core.ui.UiEvent.NavigateUp -> {
                    onNavigateBack()
                }
                is com.azuratech.azuratime.core.ui.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
        }
    }

    AzuraScreen(
        title = "Tambah Siswa Baru",
        onBack = onNavigateBack,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AzuraSpacing.md)
            ) {
                AzuraUserFormContent(
                    name = uiState.name,
                    onNameChange = { viewModel.onNameChange(it) },
                    faceId = uiState.studentId,
                    onfaceIdChange = { viewModel.onStudentIdChange(it) },
                    embedding = uiState.embedding,
                    capturedBitmap = uiState.capturedBitmap,
                    onCaptureEmbedding = { 
                        captureMode = "EMBEDDING"
                        showCamera = true 
                        triggerCapture = false
                        embeddingReceived = false
                    },
                    onCapturePhoto = { 
                        captureMode = "PHOTO"
                        showCamera = true 
                        triggerCapture = false
                        photoReceived = false
                    },
                    onUploadPhoto = { },
                    isSubmitting = uiState.isSubmitting,
                    isSubmitEnabled = uiState.isFormValid,
                    onSubmit = { viewModel.saveStudent() },
                    submitText = "Daftarkan Siswa",
                    additionalFields = {
                        val selectedClassName = uiState.availableClasses.find { it.id == uiState.selectedClassId }?.name ?: "Pilih Kelas *"
                        AzuraDropdownField(
                            label = "Pilih Kelas *",
                            selectedValue = selectedClassName,
                            options = uiState.availableClasses,
                            isExpanded = isClassExpanded,
                            onExpandedChange = { isClassExpanded = it },
                            onOptionSelected = { classModel ->
                                viewModel.onClassSelected(classModel.id, classModel.name)
                            },
                            getOptionLabel = { it.name }
                        )
                    }
                )
            }

            if (showCamera) {
                PermissionsHandler(permissionState = cameraPermissionState) {
                    val analyzer = remember {
                        FaceAnalyzer(
                            isFrontCamera = true,
                            bypassLiveness = true,
                            onFaceEmbedding = { _, embedding ->
                                if (triggerCapture && captureMode == "EMBEDDING" && !embeddingReceived) {
                                    viewModel.onEmbeddingCaptured(embedding)
                                    embeddingReceived = true
                                    showCamera = false
                                }
                            },
                            onFaceCaptured = { bitmap ->
                                if (triggerCapture && captureMode == "PHOTO" && !photoReceived) {
                                    // SANGAT PENTING: Harus di-copy karena analyzer akan me-recycle bitmap aslinya
                                    val config = bitmap.config ?: android.graphics.Bitmap.Config.ARGB_8888
                                    val bitmapCopy = bitmap.copy(config, false)
                                    viewModel.onPhotoCaptured(bitmapCopy)
                                    photoReceived = true
                                    showCamera = false
                                }
                            },
                            onLivenessStatus = { }
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        CoreFaceCamera(
                            analyzer = analyzer,
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        Button(
                            onClick = { triggerCapture = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AzuraSpacing.lg)
                                .align(androidx.compose.ui.Alignment.BottomCenter)
                        ) {
                            Text("Ambil ${if (captureMode == "EMBEDDING") "Wajah" else "Foto"}")
                        }
                    }
                }
            }
        }
    }
}
