package com.azuratech.azuratime.ui.classes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen
import com.azuratech.azuratime.ui.core.designsystem.FaceAvatar
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.add.FaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    classId: String,
    className: String,
    @Suppress("UNUSED_PARAMETER") classViewModel: ClassViewModel,
    faceViewModel: FaceViewModel,
    onBack: () -> Unit,
    onAddStudent: (String) -> Unit // Navigates to student selection
) {
    // 1. Observe Students in this class
    val classStudents by remember(classId) {
        faceViewModel.getFacesInClassFlow(classId)
    }.collectAsStateWithLifecycle(emptyList())

    AzuraScreen(
        title = className,
        onBack = onBack,
        actions = {
            IconButton(onClick = { onAddStudent(classId) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Student")
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(AzuraSpacing.md)) {
            
            // Stats Header
            Text(
                text = "Total: ${classStudents.size} Siswa",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(AzuraSpacing.md))

            if (classStudents.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Belum ada siswa di kelas ini.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)
                ) {
                    items(classStudents, key = { it.faceId }) { student ->
                        StudentRow(student = student)
                    }
                }
            }
        }
    }
}

@Composable
fun StudentRow(student: FaceEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AzuraShapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FaceAvatar(photoPath = student.photoUrl, size = 48)
            Spacer(modifier = Modifier.width(AzuraSpacing.md))
            Column {
                Text(text = student.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(text = "ID: ${student.faceId}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}