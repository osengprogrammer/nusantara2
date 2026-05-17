package com.azuratech.azuratime.features.student.ui.form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun EditStudentScreen(
    faceId: String,
    onNavigateBack: () -> Unit,
    viewModel: StudentFormViewModel = hiltViewModel(),
) {
    LaunchedEffect(faceId) {
        viewModel.loadStudentForEdit(faceId)
    }

    // Reuse AddStudentScreen as they share the same form logic
    AddStudentScreen(
        onNavigateBack = onNavigateBack,
        viewModel = viewModel,
    )
}
