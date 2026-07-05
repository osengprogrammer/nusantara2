package com.azuratech.azuratime.features.student.ui.bulk

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File

// 🔥 Utils & ViewModels
import com.azuratech.azuratime.R
import com.azuratech.azuratime.core.util.showToast
import com.azuratech.azuraengine.model.ProcessResult

// 🔥 Azura Design System Imports
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.designsystem.theme.AzuraSpacing
import com.azuratech.azuratime.core.designsystem.theme.AzuraShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkRegistrationScreen(
    onNavigateBack: () -> Unit,
    bulkViewModel: RegisterViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val bulkState by bulkViewModel.uiStateFlow.collectAsStateWithLifecycle()

    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }
    var showCsvFormat by remember { mutableStateOf(false) }

    val csvTemplateExample = stringResource(id = R.string.bulk_csv_template_example_data)
    val templateChooserTitle = stringResource(id = R.string.bulk_save_template_chooser)
    val unsupportedFileMsg = stringResource(id = R.string.bulk_error_unsupported_file)

    // 🔥 AI Native: Collect and Handle UI Effects
    LaunchedEffect(Unit) {
        bulkViewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is RegisterUiEffect.ShowToast -> context.showToast(effect.message)
            }
        }
    }

    fun downloadCsvTemplate() {
        val header = "student_id,full_name,class_name,photo_url"
        val csvContent = "$header\n$csvTemplateExample\n"
        try {
            val file = File(context.cacheDir, "Azura_Bulk_Template.csv")
            file.writeText(csvContent)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, templateChooserTitle))
        } catch (e: Exception) {
            context.showToast(context.getString(R.string.bulk_template_error_prefix, e.message ?: ""))
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(it)?.lowercase() ?: ""
            var pickedName: String? = null
            resolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) pickedName = cursor.getString(nameIndex)
            }

            if (mimeType.contains("csv") || pickedName?.endsWith(".csv") == true) {
                fileUri = it
                fileName = pickedName ?: "data.csv"
                bulkViewModel.onEvent(RegisterUiEvent.ResetState)
            } else {
                context.showToast(unsupportedFileMsg)
            }
        }
    }

    AzuraScreen(title = stringResource(id = R.string.bulk_screen_title), onBack = onNavigateBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
        ) {
            // --- INSTRUCTIONS ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(start = AzuraSpacing.md, end = AzuraSpacing.md, top = AzuraSpacing.md),
                    shape = AzuraShapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    onClick = { showCsvFormat = !showCsvFormat },
                ) {
                    Column(modifier = Modifier.padding(AzuraSpacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Help, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(id = R.string.bulk_help_format_title), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Icon(if (showCsvFormat) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                        }
                        if (showCsvFormat) {
                            Text(
                                text = stringResource(id = R.string.bulk_help_format_desc),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { downloadCsvTemplate() }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Description, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(id = R.string.bulk_action_download_template))
                            }
                        }
                    }
                }
            }

            // --- FILE SELECTION ---
            item {
                Column(modifier = Modifier.padding(horizontal = AzuraSpacing.md)) {
                    OutlinedButton(
                        onClick = { fileLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = AzuraShapes.medium,
                        enabled = !bulkState.isProcessing,
                    ) {
                        Icon(Icons.Default.AttachFile, null)
                        Spacer(Modifier.width(8.dp))
                        Text(fileName ?: stringResource(id = R.string.bulk_action_select_file))
                    }

                    if (fileUri != null) {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                bulkViewModel.onEvent(RegisterUiEvent.ProcessCsv(fileUri!!))
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = AzuraShapes.medium,
                            enabled = !bulkState.isProcessing,
                        ) {
                            Icon(Icons.Default.CloudUpload, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (bulkState.isProcessing) stringResource(id = R.string.bulk_processing_state) else stringResource(id = R.string.bulk_action_process_import))
                        }
                    }
                }
            }

            // --- PROGRESS BAR ---
            if (bulkState.isProcessing || bulkState.status.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = AzuraSpacing.md)) {
                        if (bulkState.isProcessing) {
                            LinearProgressIndicator(progress = { bulkState.progress }, modifier = Modifier.fillMaxWidth())
                        }
                        Text(bulkState.status, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // --- RESULT LOGS ---
            items(bulkState.results) { result ->
                ResultRow(result)
            }
        }
    }
}

@Composable
fun ResultRow(result: ProcessResult) {
    val isSuccess = result.status.contains("Registered") || result.status.contains("Updated")
    val color = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AzuraSpacing.md, vertical = 2.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.5f)),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error, null, tint = color)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(result.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(result.status, color = color, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
