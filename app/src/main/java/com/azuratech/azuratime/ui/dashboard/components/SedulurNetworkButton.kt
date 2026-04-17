package com.azuratech.azuratime.ui.dashboard.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun SedulurNetworkButton(pendingRequests: Int, onClick: () -> Unit) {
    BadgedBox(
        badge = {
            if (pendingRequests > 0) {
                Badge(containerColor = MaterialTheme.colorScheme.error) { Text(pendingRequests.toString()) }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AzuraSpacing.md)
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = AzuraShapes.medium
        ) {
            Icon(Icons.Default.People, contentDescription = "Jaringan Sedulur")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Jaringan Sedulur (Teman)")
        }
    }
}
