package com.azuratech.azuratime.ui.core.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Standardized Multi-Preview annotation for the Azura Design System.
 * Applies both Light and Dark mode previews simultaneously to ensure
 * UI components are tested against both themes without duplicating code.
 */
@Preview(
    name = "Light Mode",
    group = "Azura System",
    showBackground = true,
    backgroundColor = 0xFFFFFFFF
)
@Preview(
    name = "Dark Mode",
    group = "Azura System",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF121212
)
annotation class AzuraPreviews
