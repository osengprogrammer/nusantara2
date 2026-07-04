# Theme Migration Guide

This guide walks you through migrating your theme files from `app` to the new `:core-designsystem` module.

## 📋 Overview

**Migration Path:**
- **From:** `app/src/main/java/com/azuratech/azuratime/core/ui/theme/`
- **To:** `core-designsystem/src/main/java/com/azuratech/azuratime/core/designsystem/theme/`

**Package Change:**
```kotlin
// Before
package com.azuratech.azuratime.core.ui.theme
import com.azuratech.azuratime.core.ui.theme.AzuraTheme
import com.azuratech.azuratime.core.ui.theme.AzuraGradients

// After
package com.azuratech.azuratime.core.designsystem.theme
import com.azuratech.azuratime.core.designsystem.theme.AzuraTheme
import com.azuratech.azuratime.core.designsystem.theme.AzuraGradients
```

## 🛠️ Scripts Available

### 1. `preview-migration.sh` - Preview Changes
```bash
./preview-migration.sh
```
Shows what will be changed without making any modifications.

### 2. `refactor-theme-migration.sh` - Execute Migration
```bash
./refactor-theme-migration.sh
```
Performs the actual migration:
- ✅ Moves theme files to new location
- ✅ Updates package declarations
- ✅ Updates all imports across 79 files
- ✅ Creates automatic backup

### 3. `validate-migration.sh` - Verify Results
```bash
./validate-migration.sh
```
Checks if migration completed successfully and builds the project.

### 4. Revert Changes
```bash
./refactor-theme-migration.sh --revert <backup_directory>
```
Example:
```bash
./refactor-theme-migration.sh --revert .refactor_backup_20260704_143022
```

## 📝 Step-by-Step Execution

### Step 1: Preview (Recommended)
```bash
cd /home/max/azuratime/nusantara-main
./preview-migration.sh
```
Review the output to understand what will change.

### Step 2: Execute Migration
```bash
./refactor-theme-migration.sh
```
**Important:** Note the backup directory printed at the end!

### Step 3: Validate
```bash
./validate-migration.sh
```
This will:
- Check for remaining old imports
- Verify new theme files exist
- Attempt to build the `:core-designsystem` module
- Report any errors or warnings

### Step 4: Manual Gradle Build (Extra Safety)
```bash
./gradlew clean :core-designsystem:assembleDebug
./gradlew :app:assembleDebug
```

### Step 5: Run and Test
```bash
./gradlew installDebug
# Test the app thoroughly
```

## 🔄 Reverting Changes

If the build fails or something goes wrong:

1. **Find your backup directory** from the migration output
2. **Run revert command:**
   ```bash
   ./refactor-theme-migration.sh --revert .refactor_backup_YYYYMMDD_HHMMSS
   ```
3. **Clean and rebuild:**
   ```bash
   ./gradlew clean
   ./gradlew :app:assembleDebug
   ```

## 📊 What Gets Changed

### Files Moved (2 files):
- `AzuraGradients.kt`
- `Type.kt`

### Files Updated (79 files):
All files across your project that import from the theme package, including:
- `app/src/main/java/.../core/ui/designsystem/*.kt` (17 files)
- `app/src/main/java/.../features/*/ui/*.kt` (62 files)

### Import Changes:
```kotlin
// Old
import com.azuratech.azuratime.core.ui.theme.AzuraSpacing
import com.azuratech.azuratime.core.ui.theme.AzuraTheme
import com.azuratech.azuratime.core.ui.theme.AzuraGradients

// New
import com.azuratech.azuratime.core.designsystem.theme.AzuraSpacing
import com.azuratech.azuratime.core.designsystem.theme.AzuraTheme
import com.azuratech.azuratime.core.designsystem.theme.AzuraGradients
```

## ⚠️ Important Notes

1. **Backup is Automatic**: The script creates a timestamped backup before any changes
2. **No Data Loss**: All original files are preserved in the backup
3. **Incremental Safety**: You can validate after each step before proceeding
4. **Full Revert**: One command to undo everything if needed

## 🐛 Troubleshooting

### Build Fails After Migration
1. Run validation script: `./validate-migration.sh`
2. Check for specific error messages
3. If critical errors, revert: `./refactor-theme-migration.sh --revert <backup>`
4. Check that `:core-designsystem` module is properly included in `settings.gradle.kts`

### Missing Dependencies
Make sure `:core-designsystem` is included in your modules that use the theme:
```kotlin
// In feature modules' build.gradle.kts
dependencies {
    implementation(project(":core-designsystem"))
}
```

### IDE Shows Errors
1. Invalidate caches: File → Invalidate Caches → Restart
2. Sync Gradle files
3. Run: `./gradlew clean build`

## ✅ Success Criteria

✅ No old imports remain in any `.kt` file  
✅ All theme files exist in new location  
✅ `:core-designsystem:assembleDebug` builds successfully  
✅ `:app:assembleDebug` builds successfully  
✅ App runs without crashes  
✅ UI components display correctly  

## 📞 Support

If you encounter issues not covered here:
1. Check the backup directory for original files
2. Review validation script output
3. Check Gradle build error messages
4. Compare with backup to identify differences

---

**Last Updated:** 2026-07-04  
**Migration Script Version:** 1.0