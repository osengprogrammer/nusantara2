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
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuraengine.model.User
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.checkin.CheckInViewModel
import kotlinx.coroutines.flow.MutableStateFlow

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
fun MyAssignedClassesSection(myClasses: List<ClassModel>, onNavigateToAll: () -> Unit) {
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