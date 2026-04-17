package com.azuratech.azuratime.ui.core.designsystem

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.theme.AzuraShapes

@Composable
fun AzuraUserFormContent(
    name: String,
    onNameChange: (String) -> Unit,
    faceId: String,
    onfaceIdChange: (String) -> Unit,
    isfaceIdReadOnly: Boolean = false,
    nameError: String? = null,
    embedding: FloatArray?,
    capturedBitmap: Bitmap?,
    onCaptureEmbedding: () -> Unit,
    onCapturePhoto: () -> Unit,
    onUploadPhoto: () -> Unit,
    isSubmitting: Boolean,
    isSubmitEnabled: Boolean,
    onSubmit: () -> Unit,
    submitText: String,
    submitIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AzuraSpacing.md)
    ) {
        OutlinedTextField(
            value = faceId,
            onValueChange = onfaceIdChange,
            label = { Text("Student ID") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = isfaceIdReadOnly,
            enabled = !isfaceIdReadOnly,
            shape = AzuraShapes.medium,
            colors = if (isfaceIdReadOnly) {
                OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                OutlinedTextFieldDefaults.colors()
            },
            supportingText = if (isfaceIdReadOnly) {
                { Text("Student ID cannot be changed", style = MaterialTheme.typography.bodySmall) }
            } else null
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name *") },
            modifier = Modifier.fillMaxWidth(),
            isError = nameError != null,
            shape = AzuraShapes.medium,
            supportingText = nameError?.let { error ->
                { Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
        )

        DualFacePhotoCaptureCard(
            embedding = embedding,
            capturedBitmap = capturedBitmap,
            onCaptureEmbeddingClick = onCaptureEmbedding,
            onCapturePhotoClick = onCapturePhoto,
            onUploadPhotoClick = onUploadPhoto
        )

        Spacer(modifier = Modifier.height(AzuraSpacing.sm))

        AzuraLoadingButton(
            text = submitText,
            isLoading = isSubmitting,
            icon = submitIcon,
            enabled = isSubmitEnabled,
            modifier = Modifier.fillMaxWidth(),
            onClick = onSubmit
        )
    }
}

@Composable
fun DualFacePhotoCaptureCard(
    embedding: FloatArray?,
    capturedBitmap: Bitmap?,
    onCaptureEmbeddingClick: () -> Unit,
    onCapturePhotoClick: () -> Unit,
    onUploadPhotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AzuraShapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm)
        ) {
            Text("Face & Photo", style = MaterialTheme.typography.titleMedium)

            Button(
                onClick = onCaptureEmbeddingClick,
                modifier = Modifier.fillMaxWidth(),
                shape = AzuraShapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (embedding == null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(if (embedding == null) "Scan Face for Embedding" else "Embedding Captured! ✅")
            }

            Button(
                onClick = onCapturePhotoClick,
                modifier = Modifier.fillMaxWidth(),
                shape = AzuraShapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (capturedBitmap == null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(if (capturedBitmap == null) "Capture Live Photo" else "Live Photo Captured! ✅")
            }

            OutlinedButton(
                onClick = onUploadPhotoClick,
                modifier = Modifier.fillMaxWidth(),
                shape = AzuraShapes.medium
            ) {
                Icon(Icons.Default.Upload, contentDescription = "Upload from Gallery")
                Spacer(Modifier.width(AzuraSpacing.sm))
                Text("Or Upload from Gallery")
            }

            if (capturedBitmap != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        bitmap = capturedBitmap.asImageBitmap(),
                        contentDescription = "User photo",
                        modifier = Modifier
                            .size(80.dp)
                            .padding(end = AzuraSpacing.sm)
                            .clip(AzuraShapes.medium)
                    )
                    Text(
                        text = "✅ Photo ready",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
