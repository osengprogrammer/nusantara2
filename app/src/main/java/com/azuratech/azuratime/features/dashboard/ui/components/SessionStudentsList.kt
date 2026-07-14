package com.azuratech.azuratime.features.dashboard.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.azuratech.azuratime.core.data.local.StudentBiometricEntity
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing

@Composable
fun SessionStudentsList(students: List<StudentBiometricEntity>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = AzuraSpacing.md)) {
        Text(text = "Scanned Students", style = MaterialTheme.typography.titleMedium)
        // You can add a list of students here
        students.forEach { student ->
            Text(text = student.name, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
