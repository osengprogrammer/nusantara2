package com.azuratech.azuratime.ui.core.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.ui.theme.AzuraShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AzuraDropdownField(
    label: String,
    selectedValue: String,
    options: List<T>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOptionSelected: (T) -> Unit,
    onEditClicked: () -> Unit,
    getOptionLabel: (T) -> String,
    enabled: Boolean = true
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = { },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = enabled,
            shape = AzuraShapes.medium,
            trailingIcon = {
                Row {
                    IconButton(onClick = { if (enabled) onExpandedChange(true) }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
                    }
                    IconButton(onClick = onEditClicked) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Edit, contentDescription = "Edit $label")
                    }
                }
            }
        )
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(getOptionLabel(option)) },
                    onClick = {
                        onOptionSelected(option)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}
