package com.azuratech.azuratime.core.ui.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.azuratech.azuratime.core.ui.preview.AzuraPreviews
import com.azuratech.azuratime.core.ui.theme.AzuraShapes
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.ui.theme.AzuraTheme

@Composable
fun AzuraCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ),
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AzuraShapes.large,
        colors = colors,
        elevation = elevation,
    ) {
        Column(
            modifier = Modifier.padding(AzuraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AzuraSpacing.sm),
            content = {
                if (title != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row { actions() }
                    }
                    Spacer(modifier = Modifier.height(AzuraSpacing.xs))
                }
                content()
            },
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AzuraCard(title = "Account Info") {
                    Text("Name: John Doe")
                    Text("Status: Active")
                }
                AzuraCard(title = "No Content Card") {}
            }
        }
    }
}
