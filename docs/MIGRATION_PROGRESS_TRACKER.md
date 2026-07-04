# Core Design System Migration - Project Tracker

**Project:** AzuraTime Modularization  
**Start Date:** 2026-07-03  
**Current Status:** 🟡 Phase 1 - Configuring Module Infrastructure  
**Last Updated:** 2026-07-03 (Phase 1 Initial Setup)

---

## 🗓️ Phase Roadmap

| Phase | Description | Status | Estimated Duration | Dependency |
|-------|-------------|--------|-------------------|------------|
| **Phase 1** | Module Infrastructure & Build Setup | 🟡 **IN PROGRESS** | 1 Day | None |
| **Phase 2** | Move Theme Foundation (Colors, Spacing, etc.) | ⏳ Pending | 1 Day | Phase 1 Complete |
| **Phase 3** | Move Base UI Components (Buttons, Cards, etc.) | ⏳ Pending | 2 Days | Phase 2 Complete |
| **Phase 4** | Move Extended Components & Cleanup | ⏳ Pending | 2 Days | Phase 3 Complete |
| **Phase 5** | Testing, Documentation & Final Verification | ⏳ Pending | 1 Day | Phase 4 Complete |

---

## 🚧 Phase 1: Module Infrastructure (IN PROGRESS)

### 📝 Objective
Create the `:core-designsystem` Gradle module structure, configure build files, and prepare for file migration.

### ✅ Completed Tasks

#### 1. Directory Structure Created
**Location:** `/home/max/azuratime/nusantara-main/core-designsystem/`

```
core-designsystem/
├── build.gradle.kts                ✅ Generated
├── proguard-rules.pro              ✅ Generated
├── README.md                       ✅ Generated
└── src/main/
    ├── AndroidManifest.xml         ✅ Generated
    ├── java/com/azuratech/azuratime/core/designsystem/
    │   ├── theme/                  ✅ Empty (ready for files)
    │   ├── components/             ✅ Empty (ready for files)
    │   └── preview/                ✅ Empty (ready for files)
    └── res/values/                 ✅ Empty (ready for strings)
```

#### 2. Build Configuration Files
- **`build.gradle.kts`**: Created with:
  - `android-library` and `kotlin-android` plugins
  - Compose BOM (Bill of Materials) for version management
  - Dependencies: `androidx.compose.ui`, `material3`, `material.icons.extended`
  - Java 17 compatibility configured
  - Debug/Release build types configured

- **`AndroidManifest.xml`**: Minimal manifest (required for library modules)

- **`proguard-rules.pro`**: Template for ProGuard rules

- **`README.md`**: Module documentation with usage examples

### ⏳ Pending Tasks (IMMEDIATE ACTION REQUIRED)

#### Task 1: Update Project Settings
**File:** `settings.gradle.kts`  
**Action:** Add module inclusion  
**Line Number:** After line 24

```kotlin
// ADD THIS LINE:
include(":core-designsystem")
```

**Why:** Gradle won't recognize the module until it's included in the settings file.

#### Task 2: Sync Project
**Tool:** Android Studio / CLI  
**Action:** Trigger Gradle sync  
**Methods:**
- **Android Studio:** Click ⚡ "Sync Project with Gradle Files" button
- **CLI:** Run `./gradlew :core-designsystem:build --refresh-dependencies`

#### Task 3: Add Dependencies to Feature Modules
**Files:** `app/build.gradle.kts`, `feature-*/build.gradle.kts`  
**Action:** Add implementation dependency  
**Code:**
```kotlin
implementation(project(":core-designsystem"))
```

**Priority:** Start with `:app` module first to test.

### 🔍 Verification Checklist
Before proceeding to Phase 2, verify:
- [ ] `settings.gradle.kts` includes `:core-designsystem`
- [ ] Gradle sync completed successfully (no errors)
- [ ] `./gradlew projects` shows `:core-designsystem`
- [ ] `:app` module compiles after adding dependency
- [ ] No "Unresolved reference" errors in existing code

---

## 📂 Phase 2: Move Theme Foundation (NEXT STEP)

### 🎯 Objective
Move all theme-related files from `app/src/main/java/com/azuratech/azuratime/core/ui/theme/` to `core-designsystem/src/main/java/com/azuratech/azuratime/core/designsystem/theme/`

### 📋 Files to Migrate

| Source File | Destination | Notes |
|-------------|-------------|-------|
| `AzuraGradients.kt` | `core/designsystem/theme/` | Merge: Split into `Colors.kt`, `Spacing.kt`, `Shapes.kt`, `Gradients.kt` |
| `Type.kt` | `core/designsystem/theme/` | Update package to `com.azuratech.azuratime.core.designsystem.theme` |

### 🔧 Post-Migration Tasks
1. Update package declarations in all moved files
2. Update imports in `app/src/main/java/...` files
3. Verify no circular dependencies
4. Run `./gradlew :core-designsystem:build`

### ⚠️ Potential Issues to Watch For
- **Import Errors:** All files referencing `com.azuratech.azuratime.core.ui.theme.*` need updates
- **Package Conflicts:** Ensure `Type.kt` references to `Typography` still resolve
- **Preview Tests:** Move `AzuraPreviews.kt` diagnostics if needed

---

## 📝 Phase 3: Move Base UI Components

### 🎯 Objective
Move foundational UI components used across 8+ screens.

### 📋 Files to Migrate (Priority Order)

1. **AppTopBar.kt** (No dependencies - move first)
2. **AzuraScreen.kt** (Depends on AppTopBar)
3. **AzuraButton.kt**
4. **AzuraLoadingButton.kt**
5. **AzuraCard.kt**
6. **AzuraTextField.kt**
7. **AzuraSnackbar.kt**

### 🔧 Post-Migration Tasks
- Update imports in all feature modules
- Run full build: `./gradlew :app:assembleSchoolAttendanceDebug`
- Fix any compilation errors

---

## 📝 Phase 4: Extended Components & Cleanup

### 🎯 Objective
Move remaining components and remove duplicates from old locations.

### 📋 Files to Migrate
- `AzuraDropdownField.kt`
- `AzuraAccountRow.kt`
- `AzuraAuditTrail.kt`
- `AzuraDatePickerButton.kt` (Verify no feature deps first)
- `WorkspaceSelector.kt` (Verify no feature deps first)

### 🗑️ Cleanup Tasks
- Delete old `Azura*` files from `app/src/main/java/.../core/ui/designsystem/`
- Update `AzuraPreviews.kt` to point to new locations
- Remove any unused imports across project

---

## 🧪 Phase 5: Testing & Finalization

### 🎯 Objective
Ensure everything works correctly and document the new architecture.

### ✅ Testing Tasks
- [ ] Unit tests for all moved components
- [ ] Integration tests across flavors (`schoolAttendance`, `officeAttendance`)
- [ ] Run `./gradlew test` for both flavors
- [ ] Manual smoke test on emulator/device

### 📄 Documentation Tasks
- [ ] Update `docs/CORE_DESIGNSYSTEM_EXTRACTION_REPORT.md` with final status
- [ ] Create `USAGE_GUIDE.md` with component examples
- [ ] Add component screenshot gallery (optional)
- [ ] Update team wiki/confluence if applicable

### 🚀 Delivery Tasks
- [ ] Create Git tag: `v1.0.0-core-designsystem`
- [ ] Publish artifact to internal repository (if applicable)
- [ ] Notify team of migration completion

---

## 🐛 Known Issues & Notes

### Issues
- None currently (Phase 1 just initialized)

### Important Notes
- **Backup Point:** All code is in Git before migrations began
- **Rollback Strategy:** If issues arise, revert to commit `BEFORE_PH1_START`
- **Critical Files:** Do NOT move these to design system:
  - `AttendanceActionSheet.kt` (feature-specific logic)
  - `CoreBarcodeCamera.kt` (hardware dependencies)
  - `CoreFaceCamera.kt` (ML dependencies)

---

## 🚦 Current Progress Dashboard

```
Phase 1: [████████████████████] 70% Complete
  ✅ Directory structure created
  ✅ build.gradle.kts configured
  ✅ AndroidManifest.xml created
  ✅ ProGuard rules created
  ✅ README.md created
  ⏳ Pending: Update settings.gradle.kts
  ⏳ Pending: Sync project with Gradle
  ⏳ Pending: Add dependencies to app module

Phase 2: [                    ] 0% Complete
  ⏳ Waiting for Phase 1 completion

Phase 3: [                    ] 0% Complete
  ⏳ Waiting for Phase 2 completion

Phase 4: [                    ] 0% Complete
  ⏳ Waiting for Phase 3 completion

Phase 5: [                    ] 0% Complete
  ⏳ Waiting for Phase 4 completion

OVERALL PROGRESS: [████████░░░░░░░░░░░░] 14% Complete
```

---

## 📞 Quick Links

- **Previous Report:** `docs/CORE_DESIGNSYSTEM_EXTRACTION_REPORT.md` (Full audit)
- **UI Refactoring Summary:** `docs/UI_REFATORING_SUMMARY.md` (Flavor context)
- **Module Location:** `core-designsystem/`
- **Source Files to Move:** `app/src/main/java/com/azuratech/azuratime/core/ui/`
- **Gradle Command:** `./gradlew :core-designsystem:build`

---

## 🔄 Resume Instructions

**If you lost context and need to resume:**

1. **Check Current Phase:** Look at "Current Progress Dashboard" above
2. **Read "Next Steps"** for that phase (e.g., Phase 1 Pending Tasks)
3. **Execute Pending Tasks** in order
4. **Verify** using the checklist for that phase
5. **Update this document** with completion status
6. **Move to next phase**

**Current Immediate Action:**
```bash
# 1. Open settings.gradle.kts
# 2. Add this line after line 24:
include(":core-designsystem")

# 3. Sync Gradle in Android Studio or run:
./gradlew :core-designsystem:build

# 4. If successful, start Phase 2 (move theme files)
```

---

**Last Modified By:** AI Architect Assistant  
**Timestamp:** 2026-07-03  
**Status:** Awaiting User Action on Phase 1 Pending Tasks