package com.azuratech.azuratime.features.dashboard.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.features.biometric.data.local.StudentBiometricEntity
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.ui.theme.AzuraShapes

/**
 * 📜 SESSION STUDENTS STRIP
 * Shows horizontal chips of students expected in this session.
 */
@Composable
fun SessionStudentsList(students: List<StudentBiometricEntity>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = AzuraSpacing.md)) {
        Text(
            text = "Students in this session (${students.size})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(AzuraSpacing.sm))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(students) { student ->
                SuggestionChip(
                    onClick = {},
                    label = { Text(student.name, style = MaterialTheme.typography.labelSmall) },
                    shape = CircleShape,
                )
            }
        }
    }
}

/**
 * 🔑 MY ASSIGNED CLASSES
 * Shows the classes the account is actually responsible for.
 * Enhanced for Supervisors with quick actions.
 */
@Composable
fun MyAssignedClassesSection(
    myClasses: List<ClassModel>,
    onNavigateToAll: () -> Unit,
    onAttendanceClick: (String) -> Unit = {},
    onRosterClick: (String) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = AzuraSpacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "My Classes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
            TextButton(onClick = onNavigateToAll) { Text("Manage") }
        }

        if (myClasses.isEmpty()) {
            Text("No classes assigned yet.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(myClasses) { classItem ->
                    ClassActionCard(
                        classItem = classItem,
                        onAttendanceClick = { onAttendanceClick(classItem.id) },
                        onRosterClick = { onRosterClick(classItem.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun ClassActionCard(
    classItem: ClassModel,
    onAttendanceClick: () -> Unit,
    onRosterClick: () -> Unit,
) {
    Card(
        modifier = Modifier.width(160.dp),
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(AzuraSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.xs),
        ) {
            Text(
                text = classItem.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = "${classItem.studentCount} Students",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = onAttendanceClick,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = onRosterClick,
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                ) {
                    Icon(Icons.Default.People, null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
