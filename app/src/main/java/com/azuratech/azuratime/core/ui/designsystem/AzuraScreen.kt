package com.azuratech.azuratime.core.ui.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzuraScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    snackbarHost: @Composable () -> Unit = { SnackbarHost(hostState = snackbarHostState) },
    floatingActionButton: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = title,
                onBack = onBack,
                actions = actions
            )
        },
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = AzuraSpacing.md)
        ) {
            content()
        }
    }
}
