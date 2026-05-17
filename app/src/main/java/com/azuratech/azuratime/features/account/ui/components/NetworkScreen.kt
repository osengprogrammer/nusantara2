package com.azuratech.azuratime.features.account.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen

@Composable
fun NetworkScreen(
    onNavigateBack: () -> Unit = {},
) {
    AzuraScreen(title = "Jaringan", onBack = onNavigateBack) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Fitur Jaringan sedang dalam perbaikan.")
        }
    }
}
