# :core-designsystem

**AzuraTime Design System Module**

This module contains all shared UI components, themes, and design tokens used across all AzuraTime app flavors.

## 📦 What's Included

### Theme System
- `AzuraTheme` - Main theme wrapper (light/dark mode support)
- `AzuraColors` - Brand color palette (14+ colors)
- `AzuraSpacing` - Spacing tokens (XS to XL)
- `AzuraShapes` - Corner radius definitions
- `AzuraGradients` - Gradient brushes
- `Typography` - Material3 type scale

### Base UI Components
- `AppTopBar` - Standardized TopAppBar with back navigation
- `AzuraScreen` - Screen scaffold with consistent padding
- `AzuraButton` - Primary action button with loading state
- `AzuraLoadingButton` - Button with built-in progress indicator
- `AzuraCard` - Reusable card container
- `AzuraTextField` - Standardized text input
- `AzuraDropdownField` - Generic dropdown menu
- `AzuraSnackbar` - Custom snackbar styling
- `AzuraAccountRow` - Account/user display row
- `AzuraAuditTrail` - Audit log display component

## 🏗️ Usage in Feature Modules

Add this dependency to your feature module's `build.gradle.kts`:

```kotlin
implementation(project(":core-designsystem"))
```

Then import components:

```kotlin
import com.azuratech.azuratime.core.designsystem.theme.AzuraTheme
import com.azuratech.azuratime.core.designsystem.components.AzuraButton
import com.azuratech.azuratime.core.designsystem.components.AzuraCard
// ... other components
```

## 📝 Adding New Components

1. Create the component file in `src/main/java/com/.../designsystem/components/`
2. Follow the naming convention: `Azura<Name>.kt`
3. Include `@Composable` annotation
4. Add previews using `@AzuraPreviews` (from preview package)
5. Update this README

## 🎨 Design Tokens

All design tokens are defined in the `theme` package:
- Colors: `AzuraPrimary`, `AzuraSecondary`, etc.
- Spacing: `AzuraSpacing.xs`, `AzuraSpacing.sm`, etc.
- Shapes: `AzuraShapes.small`, `AzuraShapes.medium`, etc.

## 🧪 Testing

Components can be tested using:
```kotlin
// In :core-designsystem module
@get:Rule
val composeTestRule = createComposeRule()

// In feature modules
@get:Rule
val composeTestRule = createComposeRule()
composeTestRule.setContent {
    AzuraTheme {
        AzuraButton("Test", onClick = {})
    }
}
```

---

**Version:** 1.0.0  
**Last Updated:** 2026-07-03  
**Maintained by:** AzuraTime Team