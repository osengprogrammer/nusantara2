package com.azuratech.azuratime.features.reporting.ui.daily

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.core.ui.designsystem.AzuraScreen
import com.azuratech.azuratime.core.designsystem.theme.AzuraSpacing
import com.azuratech.azuratime.features.attendance.ui.history.AttendanceHistoryCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalContext
import com.azuratech.azuratime.core.util.showToast

/**
 * 📊 DAILY DETAIL SCREEN (v3.2.0-ai-native)
 */
@Composable
fun DailyDetailScreen(
    faceId: String,
    studentName: String,
    dateString: String,
    onNavigateBack: () -> Unit,
    onNavigateToManual: (String, String) -> Unit,
    viewModel: DailyDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 🔥 AI Native: Collect and Handle UI Effects
    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is DailyDetailUiEffect.ShowToast -> context.showToast(effect.message)
            }
        }
    }

    LaunchedEffect(faceId, dateString) {
        viewModel.onEvent(DailyDetailUiEvent.LoadData(faceId, dateString))
    }

    AzuraScreen(
        title = "Detail Presensi",
        onBack = onNavigateBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AzuraSpacing.md),
        ) {
            // Header Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(AzuraSpacing.md)) {
                    Text(
                        text = studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(AzuraSpacing.xs))

                    val formattedDate = try {
                        val parsed = LocalDate.parse(dateString)
                        parsed.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy"))
                    } catch (e: Exception) {
                        dateString
                    }
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(AzuraSpacing.lg))

            // Content
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.records.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Tidak ada data presensi",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(AzuraSpacing.md))
                            Button(onClick = { onNavigateToManual(faceId, dateString) }) {
                                Text("Input Manual")
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                            contentPadding = PaddingValues(bottom = AzuraSpacing.xl),
                        ) {
                            items(uiState.records) { record ->
                                AttendanceHistoryCard(
                                    record = record,
                                    onEditRequested = { onNavigateToManual(faceId, dateString) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
