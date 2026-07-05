package com.azuratech.azuratime.features.session.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.azuratech.azuratime.R
import com.azuratech.azuratime.core.designsystem.theme.AzuraSpacing
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails

import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.Color
import com.azuratech.azuratime.core.ui.designsystem.*
import com.azuratech.azuratime.core.designsystem.theme.AzuraShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionPickerScreen(
    viewModel: SessionPickerViewModel,
    onNavigateToScanner: (String) -> Unit,
    onShowSnackbar: (String) -> Unit,
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffectFlow.collect { effect ->
            when (effect) {
                is SessionPickerUiEffect.NavigateToScanner -> onNavigateToScanner(effect.sessionId)
                is SessionPickerUiEffect.ShowError -> onShowSnackbar(effect.message)
            }
        }
    }

    AzuraScreen(
        title = stringResource(R.string.select_session),
        onBack = { /* Handled by graph */ },
        actions = {
            IconButton(onClick = { viewModel.onEvent(SessionPickerUiEvent.Refresh) }) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = AzuraSpacing.md),
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Enterprise Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onEvent(SessionPickerUiEvent.UpdateSearchQuery(it)) },
                    placeholder = { Text("Search by subject or class...", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = AzuraSpacing.md),
                    shape = AzuraShapes.medium,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    ),
                )

                if (uiState.filteredSessions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (uiState.searchQuery.isEmpty()) stringResource(R.string.no_sessions_available) else "No sessions match your search.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = AzuraSpacing.xl),
                        verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
                    ) {
                        val adhocSessions = uiState.filteredSessions.filter { it.session.sessionId.startsWith("ADHOC_") }
                        val regularSessions = uiState.filteredSessions.filter { !it.session.sessionId.startsWith("ADHOC_") }

                        if (regularSessions.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Scheduled Today", subtitle = "Official timetable sessions")
                            }
                            items(regularSessions) { sessionWithDetails ->
                                SessionItem(
                                    session = sessionWithDetails,
                                    onClick = { viewModel.onEvent(SessionPickerUiEvent.SelectSession(sessionWithDetails.session.sessionId)) },
                                )
                            }
                        }

                        if (adhocSessions.isNotEmpty()) {
                            item {
                                SectionHeader(title = "Matrix Assignments", subtitle = "Start attendance manually from your assigned classes")
                            }
                            items(adhocSessions) { sessionWithDetails ->
                                SessionItem(
                                    session = sessionWithDetails,
                                    onClick = { viewModel.onEvent(SessionPickerUiEvent.SelectSession(sessionWithDetails.session.sessionId)) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = AzuraSpacing.md, bottom = AzuraSpacing.xs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = AzuraSpacing.xs), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun SessionItem(
    session: SessionWithDetails,
    onClick: () -> Unit,
) {
    val isAdhoc = session.session.sessionId.startsWith("ADHOC_")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AzuraShapes.medium,
        border = BorderStroke(1.dp, if (isAdhoc) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = if (isAdhoc) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = if (isAdhoc) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = AzuraShapes.small,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = if (isAdhoc) Icons.Default.FlashOn else Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (isAdhoc) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp),
                )
            }

            Spacer(modifier = Modifier.width(AzuraSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isAdhoc) {
                            stringResource(R.string.adhoc_session_title)
                        } else {
                            (session.subjectName ?: session.session.sessionType.name)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(AzuraSpacing.sm))
                    TierBadge(session.session.sessionType)
                }

                Text(
                    text = if (isAdhoc) {
                        stringResource(R.string.adhoc_session_subtitle)
                    } else {
                        "${getDayName(session.session.dayOfWeek)} | ${session.session.startTime} - ${session.session.endTime}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val className = session.className
                if (className != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.class_label, className),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                contentDescription = null,
                tint = Color.LightGray,
            )
        }
    }
}
