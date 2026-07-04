package com.azuratech.azuratime.feature.inventory.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    viewModel: AddItemViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var categoryName by remember { mutableStateOf("General") }
    var stockQuantityStr by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Item") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = stockQuantityStr,
                onValueChange = { stockQuantityStr = it },
                label = { Text("Stock Quantity") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = size,
                onValueChange = { size = it },
                label = { Text("Size (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = color,
                onValueChange = { color = it },
                label = { Text("Color (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    val qty = stockQuantityStr.toIntOrNull() ?: 0
                    viewModel.addItem(name, categoryName, qty, size.takeIf { it.isNotEmpty() }, color.takeIf { it.isNotEmpty() })
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotEmpty() && stockQuantityStr.isNotEmpty()
            ) {
                Text("Save Item")
            }
        }
    }
}