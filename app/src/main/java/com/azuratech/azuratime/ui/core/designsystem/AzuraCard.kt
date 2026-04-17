package com.azuratech.azuratime.ui.core.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.ui.core.preview.AzuraPreviews
import com.azuratech.azuratime.ui.theme.AzuraShapes
import com.azuratech.azuratime.ui.theme.AzuraSpacing
import com.azuratech.azuratime.ui.theme.AzuraTheme


@Composable
fun AzuraCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AzuraShapes.large,
        colors = colors,
        elevation = elevation
    ) {
        Column(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
            content = {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(AzuraSpacing.xs))
                }
                content()
            }
        )
    }
}


@AzuraPreviews
@Composable
fun PreviewAzuraCard() {
    AzuraTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AzuraCard(title = "User Info") {
                    Text("Name: John Doe")
                    Text("Status: Active")
                }
                AzuraCard(title = "No Content Card") {}
            }
        }
    }
}
