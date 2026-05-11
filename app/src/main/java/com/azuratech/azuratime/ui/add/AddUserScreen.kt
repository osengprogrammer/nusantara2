package com.azuratech.azuratime.ui.add

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import com.azuratech.azuratime.core.util.showToast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.ui.core.UiEvent
import com.azuratech.azuratime.ui.core.designsystem.AzuraDropdownField
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.core.designsystem.AzuraUserFormContent
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun AddUserScreen(
    onNavigateBack: () -> Unit,
    viewModel: StudentFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiStateStateFlow.collectAsStateWithLifecycle()
    val classes by viewModel.classesStateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showFaceCapture by remember { mutableStateOf(false) }
    var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var isClassExpanded by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source)
                }
                viewModel.onPhotoUploaded(bitmap)
            } catch (e: Exception) {
                context.showToast("Gagal memuat gambar")
            }
        }
    }

    // Handle submission feedback
    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.NavigateUp -> onNavigateBack()
                is UiEvent.NavigateTo -> {} // Handle if needed
            }
        }
    }

    LaunchedEffect(uiState.formError) {
        uiState.formError?.let {
            snackbarHostState.showSnackbar("Error: $it")
        }
    }

    AddUserContent(
        uiState = uiState,
        classes = classes,
        onNameChange = { viewModel.onNameChange(it) },
        onStudentIdChange = { viewModel.onStudentIdChange(it) },
        onClassSelected = { id, name -> viewModel.onClassSelected(id, name) },
        onCaptureEmbedding = { 
            captureMode = CaptureMode.EMBEDDING
            showFaceCapture = true 
        },
        onCapturePhoto = { 
            captureMode = CaptureMode.PHOTO
            showFaceCapture = true 
        },
        onUploadPhoto = { galleryLauncher.launch("image/*") },
        onSubmitClick = {
            viewModel.saveStudent()
        },
        onFlipCamera = { /* Camera logic managed in capture screen */ },
        isClassExpanded = isClassExpanded,
        onExpandedChange = { isClassExpanded = it },
        snackbarHostState = snackbarHostState
    )

    if (showFaceCapture) {
        FaceCaptureScreen(
            mode = captureMode,
            onCloseClick = { showFaceCapture = false },
            onEmbeddingCaptured = { embedding ->
                if (uiState.capturedBitmap != null) {
                    viewModel.onFaceCaptured(uiState.capturedBitmap!!, embedding)
                } else {
                    viewModel.onEmbeddingCaptured(embedding)
                }
                showFaceCapture = false
            },
            onPhotoCaptured = { bitmap ->
                viewModel.onPhotoCaptured(bitmap)
                showFaceCapture = false
            }
        )
    }
}
