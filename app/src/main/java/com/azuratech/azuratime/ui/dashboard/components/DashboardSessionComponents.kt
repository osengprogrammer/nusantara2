package com.azuratech.azuratime.ui.dashboard.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import com.azuratech.azuratime.ui.core.designsystem.AzuraButton
import com.azuratech.azuratime.ui.core.designsystem.AzuraCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.data.local.ClassEntity
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.checkin.CheckInViewModel
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 🏰 ACTIVE SESSION CARD (PURE-CLASS 2.0)
 * The main "Switchboard" for daily attendance sessions.
 */
@Composable
fun ActiveSessionCard(
    user: UserEntity?,
    allClasses: List<ClassEntity>, // 🔥 Changed from OptionEntity
    checkInViewModel: CheckInViewModel?,
    onSelectClass: (String?) -> Unit
) {
    val activeClass = allClasses.find { it.id == user?.activeClassId }
    val hasActiveSession = activeClass != null
    
    // Summary Data for progress bar
    val sessionSummary by (checkInViewModel?.sessionSummary ?: MutableStateFlow(0 to 0)).collectAsStateWithLifecycle()
    val (presentCount, totalCount) = sessionSummary

    var showPicker by remember { mutableStateOf(false) }

    val cardColor = if (hasActiveSession) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val onCard = if (hasActiveSession) MaterialTheme.colorScheme.onPrimaryContainer
                 else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = AzuraShapes.large,
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(modifier = Modifier.padding(AzuraSpacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                
                // Status Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (hasActiveSession) "SESI AKTIF" else "MODE SCAN BEBAS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = onCard.copy(alpha = 0.7f)
                    )
                }

                // Class Name
                Text(
                    text = activeClass?.name ?: "Pintu Gerbang",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = onCard
                )

                // 📊 Progress Indicator (Only if session is active)
                if (hasActiveSession && totalCount > 0) {
                    Spacer(Modifier.height(AzuraSpacing.sm))
                    val progress = (presentCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
                    
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = onCard.copy(alpha = 0.1f)
                    )
                    
                    Text(
                        text = "$presentCount dari $totalCount siswa telah discan",
                        style = MaterialTheme.typography.labelSmall,
                        color = onCard,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Picker Button
                Box {
                    Button(
                        onClick = { showPicker = true },
                        shape = AzuraShapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasActiveSession) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.SyncAlt, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (hasActiveSession) "Ganti Kelas" else "Mulai Sesi Kelas")
                    }
                    
                    DropdownMenu(expanded = showPicker, onDismissRequest = { showPicker = false }) {
                        allClasses.forEach { classEntity ->
                            DropdownMenuItem(
                                text = { Text(classEntity.name) },
                                onClick = { onSelectClass(classEntity.id); showPicker = false },
                                leadingIcon = { Icon(Icons.Default.School, null) }
                            )
                        }
                        if (allClasses.isNotEmpty()) HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Hapus Sesi (Scan Bebas)", color = MaterialTheme.colorScheme.error) },
                            onClick = { onSelectClass(null); showPicker = false },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
            
            // Icon Background
            Icon(
                if (hasActiveSession) Icons.Default.Class else Icons.Default.MeetingRoom,
                null,
                Modifier.size(72.dp).alpha(0.1f),
                tint = onCard
            )
        }
    }
}

/**
 * 📜 SESSION STUDENTS STRIP
 * Shows horizontal chips of students expected in this session.
 */
@Composable
fun SessionStudentsList(students: List<FaceEntity>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = AzuraSpacing.md)) {
        Text(
            text = "Daftar Siswa Sesi Ini (${students.size})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(AzuraSpacing.sm))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(students) { student ->
                SuggestionChip(
                    onClick = {},
                    label = { Text(student.name, style = MaterialTheme.typography.labelSmall) },
                    shape = CircleShape
                )
            }
        }
    }
}

/**
 * 🔑 MY ASSIGNED CLASSES
 * Shows the classes the teacher is actually responsible for.
 */
@Composable
fun MyAssignedClassesSection(myClasses: List<ClassEntity>, onNavigateToAll: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = AzuraSpacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kelas Saya",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = onNavigateToAll) { Text("Kelola") }
        }
        
        if (myClasses.isEmpty()) {
            Text("Belum ada kelas yang ditugaskan.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(myClasses) { classItem ->
                    AssistChip(
                        onClick = onNavigateToAll,
                        label = { Text(classItem.name) },
                        leadingIcon = { Icon(Icons.Default.Bookmark, null, Modifier.size(16.dp)) },
                        shape = AzuraShapes.medium
                    )
                }
            }
        }
    }
}