package com.azuratech.azuratime.feature.inventory.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.api.models.Item

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onNavigateToAddItem: () -> Unit,
    onNavigateToCheckout: (String) -> Unit
) {
    val items by viewModel.items.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddItem,
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Item",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(items) { item ->
                ItemRow(
                    item = item,
                    onItemClick = { onNavigateToCheckout(item.id) }
                )
            }
        }
    }
}

@Composable
fun ItemRow(item: Item, onItemClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onItemClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Name: ${item.name}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Category: ${item.category.name}")
            Text(text = "Stock: ${item.stockQuantity}")
        }
    }
}
