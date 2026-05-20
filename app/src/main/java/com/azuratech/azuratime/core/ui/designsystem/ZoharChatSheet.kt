package com.azuratech.azuratime.core.ui.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.features.ai.ui.ZoharAssistantViewModel
import com.azuratech.azuratime.features.ai.ui.ZoharUiEvent

import androidx.compose.ui.platform.LocalContext
import com.azuratech.azuratime.core.util.showToast
import com.azuratech.azuratime.features.ai.ui.ZoharUiEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoharChatSheet(
    viewModel: ZoharAssistantViewModel,
    onDismiss: () -> Unit,
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    // 🔥 AI Native: Collect and Handle UI Effects
    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect: ZoharUiEffect ->
            when (effect) {
                is ZoharUiEffect.ShowToast -> context.showToast(effect.message)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = AzuraShapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AzuraSpacing.md)
                .navigationBarsPadding(), // Agar tidak tertutup tombol navigasi HP
        ) {
            // Header Zohar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, "Zohar", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(AzuraSpacing.sm))
                Text("Zohar Intelligence", style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }

            Spacer(Modifier.height(AzuraSpacing.md))

            // Box Respon Zohar (Scrollable)
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 300.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
            ) {
                LazyColumn(modifier = Modifier.padding(AzuraSpacing.md)) {
                    item {
                        if (uiState.isLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text("Zohar sedang menganalisis data Azura...", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text(text = uiState.response, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(AzuraSpacing.md))

            // Input Field
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tanya Zohar tentang data...") },
                shape = AzuraShapes.medium,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (query.isNotBlank()) {
                                viewModel.onEvent(ZoharUiEvent.AskZohar(query))
                                query = ""
                            }
                        },
                        enabled = !uiState.isLoading,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                    }
                },
            )
            Spacer(Modifier.height(AzuraSpacing.lg))
        }
    }
}
