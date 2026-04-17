package com.azuratech.azuratime.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import androidx.compose.foundation.layout.Column


/**
 * A stateless overlay that displays the "slow" UI state (Processing, Success, Error)
 * from the ViewModel. It also contains the header controls.
 */
@Composable
fun CheckInStatusOverlay(
    uiState: CheckInUiState,
    activeClassName: String,
    onFlipCamera: () -> Unit,
    onSwitchToBarcode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        HeaderOverlay(
            activeClass = activeClassName,
            onFlipCamera = onFlipCamera,
            onSwitchToBarcode = onSwitchToBarcode
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
        ) {
            when (uiState) {
                is CheckInUiState.Success -> {
                    MatchResultLabel(
                        name = uiState.name,
                        isAlreadyIn = uiState.alreadyCheckedIn,
                        primaryColor = MaterialTheme.colorScheme.primary
                    )
                }
                is CheckInUiState.Error -> {
                    StatusLabel(text = "⛔ ${uiState.message}", color = MaterialTheme.colorScheme.error)
                }
                else -> {
                    // Idle and Processing states do not show a bottom label here.
                }
            }
        }

        if (uiState is CheckInUiState.Processing) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f))) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
}
