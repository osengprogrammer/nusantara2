package com.azuratech.azuratime.ui.core.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.ui.core.preview.AzuraPreviews
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraTheme


@Composable
fun AzuraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    errorText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    placeholder: String? = null,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = readOnly,
            enabled = enabled,
            isError = errorText != null,
            shape = AzuraShapes.medium,
            singleLine = singleLine,
            placeholder = placeholder?.let { { Text(it) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            supportingText = errorText?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } }
        )
    }
}


@AzuraPreviews
@Composable
fun PreviewAzuraTextField() {
    AzuraTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AzuraTextField(
                    value = "John Doe",
                    onValueChange = {},
                    label = "Full Name"
                )
                AzuraTextField(
                    value = "invalid-email",
                    onValueChange = {},
                    label = "Email Address",
                    errorText = "Please enter a valid email address"
                )
                AzuraTextField(
                    value = "Search here...",
                    onValueChange = {},
                    label = "Search",
                    leadingIcon = { Icon(androidx.compose.material.icons.Icons.Default.Search, contentDescription = null) }
                )
            }
        }
    }
}
