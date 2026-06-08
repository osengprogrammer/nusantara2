package com.azuratech.azuratime.features.school.ui.geofence

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraButton
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.util.showToast
import com.azuratech.azuratime.features.school.ui.list.SchoolUiEffect
import com.azuratech.azuratime.features.school.ui.list.SchoolUiEvent
import com.azuratech.azuratime.features.school.ui.list.SchoolViewModel

/**
 * 📍 GPS MANAGEMENT SCREEN (v3.2.1-ai-native)
 * Allows admins to configure geofencing rules for the school.
 */
@Composable
fun GpsManagementScreen(
    viewModel: SchoolViewModel,
    onNavigateToMapPicker: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Local state for form editing
    var isActive by remember(uiState.geofence) { mutableStateOf(uiState.geofence?.isActive ?: false) }
    var radius by remember(uiState.geofence) { mutableStateOf((uiState.geofence?.radiusMeters ?: 100).toFloat()) }
    var latitude by remember(uiState.geofence, uiState.pickedLocation) {
        mutableStateOf(uiState.pickedLocation?.latitude ?: uiState.geofence?.latitude ?: 0.0)
    }
    var longitude by remember(uiState.geofence, uiState.pickedLocation) {
        mutableStateOf(uiState.pickedLocation?.longitude ?: uiState.geofence?.longitude ?: 0.0)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            if (effect is SchoolUiEffect.ShowSnackbar) {
                context.showToast(effect.message)
            }
        }
    }

    AzuraScreen(
        title = "GPS Geofence",
        onBack = onNavigateBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AzuraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.lg),
        ) {
            // Status Toggle
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AzuraSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enforce Geofence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Restrict attendance to school area",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }

            // Radius Slider
            Column {
                Text(
                    "Geofence Radius: ${radius.toInt()}m",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 10f..500f,
                    steps = 49,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("10m", style = MaterialTheme.typography.labelSmall)
                    Text("500m", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Location Picker
            Column(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                Text("Center Point", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                OutlinedCard {
                    Column(modifier = Modifier.padding(AzuraSpacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(AzuraSpacing.sm))
                            Column {
                                Text("Lat: $latitude", style = MaterialTheme.typography.bodyMedium)
                                Text("Lng: $longitude", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Button(
                    onClick = onNavigateToMapPicker,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                ) {
                    Text("Pick Location on Map")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AzuraButton(
                text = "Save Configuration",
                onClick = {
                    uiState.activeSchoolId?.let { schoolId ->
                        viewModel.onEvent(
                            SchoolUiEvent.SaveGeofence(
                                schoolId = schoolId,
                                latitude = latitude,
                                longitude = longitude,
                                radius = radius.toInt(),
                                isActive = isActive,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                isLoading = uiState.isLoading,
            )
        }
    }
}
