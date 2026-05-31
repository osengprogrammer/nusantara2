package com.azuratech.azuratime.features.aimusic.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.features.aimusic.domain.model.TraditionalMusicTrack

import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.ui.theme.AzuraShapes

/**
 * 🚀 AiMusicScreen.kt (v3.2.1-ai-native)
 */
@Composable
fun AiMusicScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AiMusicViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is AiMusicUiEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    AzuraScreen(
        title = "Traditional Music AI",
        onBack = onNavigateBack,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(vertical = AzuraSpacing.md)) {
            Text(
                "AI Musik Tradisional",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(AzuraSpacing.lg))

            OutlinedTextField(
                value = uiState.mood,
                onValueChange = { viewModel.onEvent(AiMusicUiEvent.MoodChanged(it)) },
                label = { Text("Mood (e.g., Happy, Mystical)") },
                modifier = Modifier.fillMaxWidth(),
                shape = AzuraShapes.medium,
            )

            Spacer(modifier = Modifier.height(AzuraSpacing.md))

            OutlinedTextField(
                value = uiState.region,
                onValueChange = { viewModel.onEvent(AiMusicUiEvent.RegionChanged(it)) },
                label = { Text("Region (e.g., Bali, Java)") },
                modifier = Modifier.fillMaxWidth(),
                shape = AzuraShapes.medium,
            )

            Button(
                onClick = { viewModel.onEvent(AiMusicUiEvent.GenerateSuggestions) },
                modifier = Modifier.fillMaxWidth().padding(top = AzuraSpacing.lg),
                enabled = !uiState.isLoading,
                shape = AzuraShapes.medium,
            ) {
                Text("Generate Aransemen")
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = AzuraSpacing.md))
            }

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = AzuraSpacing.sm))
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(top = AzuraSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
            ) {
                items(uiState.suggestions) { track ->
                    MusicTrackItem(
                        track = track,
                        onPlayClick = { viewModel.onEvent(AiMusicUiEvent.PlayPreview(track.name)) },
                    )
                }
            }
        }
    }
}

@Composable
fun MusicTrackItem(
    track: TraditionalMusicTrack,
    onPlayClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(track.name, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text("${track.region} | ${track.mood}", style = MaterialTheme.typography.bodySmall)
                Text("Instrument: ${track.instrument}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onPlayClick) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Preview",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
