package com.azuratech.azuratime.features.dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.data.local.SessionWithDetails
import com.azuratech.azuratime.core.ui.components.TierBadge

/**
 * 🚀 SMART SESSION CARD (v3.7.0)
 * Automatically displays the active session based on time/day.
 * Supports Tiered Sessions (Global, Class, Academic).
 */
@Composable
fun ActiveSessionCard(
    activeSession: SessionWithDetails?,
    allSessionsToday: List<SessionWithDetails>,
    onStartAttendance: (String) -> Unit,
    onManualPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AzuraShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (activeSession != null) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
        ),
    ) {
        Column(modifier = Modifier.padding(AzuraSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (activeSession != null) MaterialTheme.colorScheme.primary else Color.Gray,
                    )
                    Spacer(Modifier.width(AzuraSpacing.sm))
                    Text(
                        text = if (activeSession != null) "Active Session Now" else "No Scheduled Session",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // 🔥 AI Native: Always allow manual picking for Enterprise flexibility
                TextButton(onClick = onManualPick) {
                    Text("Pick Manually")
                }
            }

            Spacer(modifier = Modifier.height(AzuraSpacing.md))

            if (activeSession != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.5f), AzuraShapes.medium)
                        .padding(AzuraSpacing.md),
                ) {
                    Text(
                        text = activeSession.subjectName ?: "Unnamed Session",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "Time: ${activeSession.startTime} - ${activeSession.endTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }

                Spacer(modifier = Modifier.height(AzuraSpacing.md))

                Button(
                    onClick = { onStartAttendance(activeSession.sessionId) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AzuraShapes.medium,
                    contentPadding = PaddingValues(AzuraSpacing.md),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(AzuraSpacing.sm))
                    Text("Start Attendance", fontWeight = FontWeight.Bold)
                }
            } else if (allSessionsToday.isNotEmpty()) {
                Text(
                    text = "Pick a session for today:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                // 🔥 AI Native: Sort by Priority (GLOBAL first) for manual selection
                val sortedSessions = allSessionsToday.sortedBy { it.sessionType.ordinal }

                sortedSessions.take(3).forEach { session ->
                    OutlinedCard(
                        onClick = { onStartAttendance(session.sessionId) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = AzuraShapes.medium,
                    ) {
                        Row(
                            modifier = Modifier.padding(AzuraSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = session.subjectName ?: session.sessionType.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.width(AzuraSpacing.sm))
                                    TierBadge(session.sessionType)
                                }
                                Text("${session.startTime} - ${session.endTime}", style = MaterialTheme.typography.labelSmall)
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                if (allSessionsToday.size > 3) {
                    TextButton(
                        onClick = onManualPick,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text("See all sessions")
                    }
                }
            } else {
                Text(
                    text = "There is no session scheduled for today. Go to Session Management to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
