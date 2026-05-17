package com.azuratech.azuratime.features.school.ui.list

import androidx.compose.ui.Modifier
import com.azuratech.azuratime.core.ui.designsystem.AzuraTextField
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing

@Composable
fun AddSchoolDialog(
    @Suppress("UNUSED_PARAMETER") availableClasses: List<com.azuratech.azuraengine.model.ClassModel> = emptyList(),
    onDismissRequest: () -> Unit,
    onConfirmClick: (String, String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var timezone by remember { mutableStateOf("Asia/Jakarta") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Tambah Sekolah Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)) {
                AzuraTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nama Sekolah",
                    modifier = Modifier.fillMaxWidth()
                )
                AzuraTextField(
                    value = timezone,
                    onValueChange = { timezone = it },
                    label = "Timezone",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmClick(name, timezone, emptyList()) }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Batal")
            }
        }
    )
}
