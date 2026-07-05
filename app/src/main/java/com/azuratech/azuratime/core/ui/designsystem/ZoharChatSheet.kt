package com.azuratech.azuratime.core.ui.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.core.designsystem.theme.AzuraShapes
import com.azuratech.azuratime.core.designsystem.theme.AzuraSpacing
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.features.ai.ui.ZoharAssistantViewModel
import com.azuratech.azuratime.features.ai.ui.ZoharUiEvent
import com.azuratech.azuratime.features.ai.ui.ChatMessage
import com.azuratech.azuratime.features.ai.ui.ChatRole

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
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
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
                .fillMaxHeight(0.9f)
                .padding(AzuraSpacing.md)
                .navigationBarsPadding()
                .imePadding(),
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
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
            ) {
                LazyColumn(
                    modifier = Modifier.padding(AzuraSpacing.md).fillMaxSize(),
                    reverseLayout = false, // Latest message at bottom
                ) {
                    items(uiState.conversationHistory.size) { index ->
                        val message = uiState.conversationHistory[index]
                        ChatBubble(message)
                        Spacer(Modifier.height(AzuraSpacing.sm))
                    }

                    if (uiState.isLoading) {
                        item {
                            Column {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Text("Zohar sedang mengetik...", style = MaterialTheme.typography.bodySmall)
                            }
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
                leadingIcon = {
                    IconButton(onClick = { /* Voice logic placeholder */ }) {
                        Icon(Icons.Default.MicNone, contentDescription = "Voice", tint = MaterialTheme.colorScheme.primary)
                    }
                },
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

@Composable
fun ChatBubble(message: ChatMessage) {
    val isZohar = message.role == ChatRole.ZOHAR
    val alignment = if (isZohar) Alignment.Start else Alignment.End
    val containerColor = if (isZohar) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isZohar) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Card(
            shape = AzuraShapes.medium,
            colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(AzuraSpacing.sm),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = if (isZohar) "Zohar" else "Me",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
