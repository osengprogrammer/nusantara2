package com.azuratech.azuratime.features.session.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.features.session.data.local.SessionWithDetails

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.select_session), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(SessionPickerUiEvent.Refresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.sessions.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_sessions_available),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(AzuraSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md),
                ) {
                    items(uiState.sessions) { sessionWithDetails ->
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

@Composable
fun SessionItem(
    session: SessionWithDetails,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(AzuraSpacing.md)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (session.session.sessionId.startsWith("ADHOC_")) Icons.Default.FlashOn else Icons.Default.Schedule,
                contentDescription = null,
                tint = if (session.session.sessionId.startsWith("ADHOC_")) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.width(AzuraSpacing.md))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (session.session.sessionId.startsWith("ADHOC_")) {
                            stringResource(R.string.adhoc_session_title)
                        } else {
                            (session.subjectName ?: session.session.sessionType.name)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(AzuraSpacing.sm))
                    TierBadge(session.session.sessionType)
                }
                Text(
                    text = if (session.session.sessionId.startsWith("ADHOC_")) {
                        stringResource(R.string.adhoc_session_subtitle)
                    } else {
                        "${getDayName(session.session.dayOfWeek)} | ${session.session.startTime} - ${session.session.endTime}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val className = session.className
                if (className != null) {
                    Text(
                        text = stringResource(R.string.class_label, className),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
