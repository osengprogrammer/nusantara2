package com.azuratech.azuratime.ui.add

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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showFaceCapture by remember { mutableStateOf(false) }
    var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var isClassExpanded by remember { mutableStateOf(false) }

    // Handle submission feedback
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
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
        onNameChange = { viewModel.onNameChange(it) },
        onStudentIdChange = { viewModel.onStudentIdChange(it) },
        onClassSelected = { viewModel.onClassSelected(it) },
        onCaptureEmbedding = { 
            captureMode = CaptureMode.EMBEDDING
            showFaceCapture = true 
        },
        onCapturePhoto = { 
            captureMode = CaptureMode.PHOTO
            showFaceCapture = true 
        },
        onUploadPhoto = { /* TODO */ },
        onSubmit = {
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
            onClose = { showFaceCapture = false },
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
