package com.azuratech.azuratime.features.school.ui.geofence

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.features.school.ui.list.SchoolUiEffect
import com.azuratech.azuratime.features.school.ui.list.SchoolUiEvent
import com.azuratech.azuratime.features.school.ui.list.SchoolViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.flow.collectLatest

/**
 * 🗺️ MAP PICKER SCREEN (v3.2.1-ai-native)
 * Bulletproof implementation to resolve blank/gray rendering and add location awareness.
 * Added runtime permission request and reactive location updates.
 */
@Composable
fun MapPickerScreen(
    viewModel: SchoolViewModel,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiStateFlow.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 🛡️ AI Native: Reactive permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            viewModel.onEvent(SchoolUiEvent.FetchCurrentLocation)
        }
    }

    // 1. Bulletproof CameraPositionState
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-6.2088, 106.8456), 15f)
    }

    // Reactive marker position
    var markerPosition by remember {
        mutableStateOf<LatLng?>(null)
    }

    // 🛡️ AI Native: persistent MarkerState for draggable support
    val markerState = rememberMarkerState()

    // Sync markerState when markerPosition (tap or GPS) changes
    LaunchedEffect(markerPosition) {
        markerPosition?.let {
            markerState.position = it
        }
    }

    // Sync back markerPosition when markerState is dragged
    LaunchedEffect(markerState.position) {
        markerPosition = markerState.position
    }

    // Handle ViewModel Effects (Snackbars)
    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collectLatest { effect ->
            if (effect is SchoolUiEffect.ShowSnackbar) {
                snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    // 🔥 AI Native: Logic on start
    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            val currentGeofence = uiState.geofence
            if (currentGeofence == null) {
                viewModel.onEvent(SchoolUiEvent.FetchCurrentLocation)
            } else {
                val initialLatLng = LatLng(currentGeofence.latitude, currentGeofence.longitude)
                cameraPositionState.position = CameraPosition.fromLatLngZoom(initialLatLng, 15f)
                markerPosition = initialLatLng
            }
        }
    }

    // 🔥 AI Native: Sync camera when pickedLocation changes from ViewModel
    LaunchedEffect(uiState.pickedLocation) {
        uiState.pickedLocation?.let { location ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(location, 15f),
                durationMs = 1000,
            )
            markerPosition = location
        }
    }

    AzuraScreen(
        title = "Pick School Location",
        onBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 2. GoogleMap with fillMaxSize modifier
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { markerPosition = it },
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                uiSettings = MapUiSettings(myLocationButtonEnabled = false),
            ) {
                if (markerPosition != null) {
                    Marker(
                        state = markerState,
                        title = "Selected School Center",
                        draggable = true,
                    )
                }
            }

            // 📍 My Location Button
            if (hasLocationPermission) {
                FloatingActionButton(
                    onClick = { viewModel.onEvent(SchoolUiEvent.FetchCurrentLocation) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                }
            }

            // 3. Confirm Button Overlay (Box Alignment.BottomCenter)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Button(
                    onClick = {
                        markerPosition?.let {
                            viewModel.onEvent(SchoolUiEvent.PickLocation(it))
                            onNavigateBack()
                        }
                    },
                    enabled = markerPosition != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = if (markerPosition != null) "Confirm Location" else "Select Location on Map",
                    )
                }
            }
        }
    }
}
