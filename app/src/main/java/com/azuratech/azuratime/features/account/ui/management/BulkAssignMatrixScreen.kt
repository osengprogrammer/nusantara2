package com.azuratech.azuratime.features.account.ui.management

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.*
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.util.showToast
import com.azuratech.azuratime.features.account.domain.usecase.MatrixImportPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkAssignMatrixScreen(
    onNavigateBack: () -> Unit,
    viewModel: BulkAssignMatrixViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()

    var fileName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is BulkAssignMatrixUiEffect.ShowToast -> context.showToast(effect.message)
                BulkAssignMatrixUiEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val resolver = context.contentResolver
            resolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) fileName = cursor.getString(nameIndex)
            }
            viewModel.onEvent(BulkAssignMatrixUiEvent.ProcessFile(it))
        }
    }

    AzuraScreen(
        title = "Bulk Matrix Assignment",
        onBack = onNavigateBack,
        actions = {
            if (uiState.previews.isNotEmpty() && uiState.previews.any { it.isSuccess }) {
                IconButton(
                    onClick = { viewModel.onEvent(BulkAssignMatrixUiEvent.Commit) },
                    enabled = !uiState.isCommitting,
                ) {
                    if (uiState.isCommitting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.DoneAll, contentDescription = "Commit", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(AzuraSpacing.md)) {
            // Instructions
            AzuraCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(AzuraSpacing.md)) {
                    Text("CSV Import Format", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Columns: teacher_email (Must end with @gmail.com), class_name, subject_name (optional)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(AzuraSpacing.md))

            // File Selection
            OutlinedButton(
                onClick = { fileLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = AzuraShapes.medium,
                enabled = !uiState.isLoading && !uiState.isCommitting,
            ) {
                Icon(Icons.Default.AttachFile, null)
                Spacer(Modifier.width(8.dp))
                Text(fileName ?: "Select CSV File")
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(AzuraSpacing.xl), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            Spacer(modifier = Modifier.height(AzuraSpacing.md))

            // Preview List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
            ) {
                items(uiState.previews) { preview ->
                    PreviewRow(preview)
                }
            }

            if (uiState.previews.isNotEmpty()) {
                val validCount = uiState.previews.count { it.isSuccess }
                Text(
                    text = "$validCount valid rows ready to import.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = AzuraSpacing.sm).align(Alignment.CenterHorizontally),
                )
            }
        }

        if (uiState.error != null) {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(BulkAssignMatrixUiEvent.ClearError) },
                title = { Text("Import Error") },
                text = { Text(uiState.error!!) },
                confirmButton = {
                    TextButton(onClick = { viewModel.onEvent(BulkAssignMatrixUiEvent.ClearError) }) {
                        Text("OK")
                    }
                },
            )
        }
    }
}

@Composable
fun PreviewRow(preview: MatrixImportPreview) {
    val color = if (preview.isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AzuraShapes.small,
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.5f)),
    ) {
        Row(modifier = Modifier.padding(AzuraSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (preview.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = color,
            )
            Spacer(modifier = Modifier.width(AzuraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(preview.teacherEmail, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "${preview.className} ${if (preview.subjectName != null) "| ${preview.subjectName}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!preview.isSuccess) {
                Text(preview.status, style = MaterialTheme.typography.labelSmall, color = color)
            }
        }
    }
}
