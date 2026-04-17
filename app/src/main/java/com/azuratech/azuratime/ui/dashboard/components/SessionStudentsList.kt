package com.azuratech.azuratime.ui.dashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.azuratech.azuratime.data.local.FaceEntity
import com.azuratech.azuratime.ui.theme.AzuraSpacing

@Composable
fun SessionStudentsList(students: List<FaceEntity>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = AzuraSpacing.md)) {
        Text(text = "Siswa Terpindai", style = MaterialTheme.typography.titleMedium)
        // You can add a list of students here
        students.forEach { student ->
            Text(text = student.name, style = MaterialTheme.typography.bodyMedium)
        }
    }
}