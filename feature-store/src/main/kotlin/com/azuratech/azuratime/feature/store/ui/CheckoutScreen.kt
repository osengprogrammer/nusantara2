package com.azuratech.azuratime.feature.store.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: CheckoutViewModel,
    itemId: String,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var quantity by remember { mutableStateOf("1") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout: $itemId") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: 0
                    if (qty > 0) {
                        viewModel.processCheckout(itemId, qty)
                    }
                },
                containerColor = if (quantity.toIntOrNull() ?: 0 > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                }
            ) {
                Text("Process")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Preparing checkout for:", style = MaterialTheme.typography.labelLarge)
            Text("Item ID: $itemId", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Quantity") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            when (val state = uiState) {
                is CheckoutUiState.Loading -> Text("Processing...", color = MaterialTheme.colorScheme.primary)
                is CheckoutUiState.Success -> Text(state.message, color = MaterialTheme.colorScheme.primary)
                is CheckoutUiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
