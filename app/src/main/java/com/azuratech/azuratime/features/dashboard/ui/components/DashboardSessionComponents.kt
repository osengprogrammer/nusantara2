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
            text = "Daftar Siswa Sesi Ini (${students.size})",
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
 * Shows the classes the teacher is actually responsible for.
 */
@Composable
fun MyAssignedClassesSection(myClasses: List<ClassModel>, onNavigateToAll: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = AzuraSpacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Kelas Saya",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
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
                        shape = AzuraShapes.medium,
                    )
                }
            }
        }
    }
}
