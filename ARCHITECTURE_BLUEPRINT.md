# AzuraTime Attendance System - Architecture Blueprint

**Last Updated:** 2026-07-03  
**Project:** Nusantara - AzuraTime Attendance Application  
**Architecture Version:** 3.0 (Unified Attendance with Vocabulary Flavors)

---

## 🎯 Project Vision

AzuraTime is a **unified attendance tracking system** deployed in two vocabulary variants:

### **Single Core System**
- **Core Functionality** (identical in both flavors):
  - Biometric attendance (face recognition, fingerprint)
  - Barcode/QR code check-in/out
  - Manual attendance entry
  - Session/class management
  - Attendance history & reporting
  - User authentication & profile management

### **Two Vocabulary Flavors**

| Aspect | `schoolAttendance` | `officeAttendance` |
|--------|-------------------|-------------------|
| **Target Users** | Schools, Students, Teachers | Corporates, Employees, Managers |
| **User Terminology** | Student, Teacher, Class | Staff, Employee, Department |
| **Session Terms** | Class, Period, Subject | Shift, Meeting, Task |
| **Attendance Types** | Present, Absent, Late | Checked In, Checked Out, Late |
| **Reports** | Student Reports, Parent View | Staff Reports, Manager Dashboard |
| **App ID** | `com.azuratech.azuratime.school` | `com.azuratech.azuratime.office` |
| **App Name** | "AzuraTime School" | "AzuraTime Office" |

**Key Principle**: Both flavors share **100% of theattendance logic**. Differences are purely cosmetic (strings, labels, some UI text). No functional divergence.

---

## 🏗️ Modular Structure

### **Core Modules** (Shared by Both Flavors)

| Module | Responsibility |
|--------|---------------|
| `:core-api` | Common data models, base utilities, shared interfaces |
| `:feature-attendance-core` | **Complete attendance system**: Biometric logic, attendance tracking, session management, user profiles, reporting |
| `:feature-navigation` | Unified navigation for both flavors (vocabulary-aware) |

### **External Modules** (NOT Used - Removed)

| Module | Status | Reason |
|--------|--------|--------|
| `:feature-ims` | ❌ **Removed** | Garment business logic - belongs to separate project |
| `:feature-store` | ❌ **Removed** | Point-of-sale - belongs to separate project |
| `:feature-audit` | ❌ **Removed** | Inventory audit - belongs to separate project |
| `:feature-inventory` | ❌ **Removed** | Inventory management - belongs to separate project |

### **Application Module**

| Module | Responsibility |
|--------|---------------|
| `:app` | Flavor-aware entry point with vocabulary switching |

---

## 🌿 Flavor Strategy

### **Product Flavors**

```kotlin
flavorDimensions += "appType"
productFlavors {
    create("schoolAttendance") {
        dimension = "appType"
        applicationId = "com.azuratech.azuratime.school"
        versionNameSuffix = "-school"
        manifestPlaceholders["appName"] = "AzuraTime School"
        buildConfigField("String", "VOCAB_USER_TYPE", "\"Student\"")
        buildConfigField("String", "VOCAB_SESSION_TYPE", "\"Class\"")
    }
    create("officeAttendance") {
        dimension = "appType"
        applicationId = "com.azuratech.azuratime.office"
        versionNameSuffix = "-office"
        manifestPlaceholders["appName"] = "AzuraTime Office"
        buildConfigField("String", "VOCAB_USER_TYPE", "\"Staff\"")
        buildConfigField("String", "VOCAB_SESSION_TYPE", "\"Shift\"")
    }
}
```

### **Dependency Rules**

#### **Both Flavors** (Identical Dependencies)
```kotlin
dependencies {
    // Core attendance system
    implementation(project(":feature-attendance-core"))
    implementation(project(":feature-navigation"))
    
    // ML/Engine
    implementation(project(":azura-engine-kmp"))
    implementation(project(":ml-engine"))
    
    // Base
    implementation(project(":core-api"))
    
    // Firebase, Compose, Hilt, Room, etc. (shared)
}
```

#### **NO Flavor-Specific Dependencies**
- ❌ No IMS, Store, Audit, or Inventory dependencies
- ❌ Both flavors have **identical** dependency graphs
- ✅ Any future vocabulary differences handled via BuildConfig fields or string resources

### **Vocabulary Abstraction**

UI vocabulary is abstracted via:
1. **BuildConfig fields** for code-level terminology
2. **String resources** with flavor-specific values:
   - `res/values/strings.xml` (default/shared)
   - `res/values-schoolAttendance/strings.xml` (student/teacher terms)
   - `res/values-officeAttendance/strings.xml` (staff/employee terms)
3. **VocabularyProvider** class centralized in `feature-attendance-core`

---

## 📋 Execution Roadmap

### **Step 1: Remove Garment Business Dependencies** 🔴 *Critical*
**Goal**: Eliminate all references to IMS, Store, Audit from the attendance app

- [ ] Remove IMS/Store/Audit from `app/build.gradle.kts`:
  ```kotlin
  // REMOVE THESE LINES:
  // implementation(project(":feature-ims"))
  // implementation(project(":feature-store"))
  // implementation(project(":feature-audit"))
  // implementation(project(":feature-inventory"))
  ```

- [ ] Remove from `settings.gradle.kts`:
  ```kotlin
  // REMOVE THESE LINES:
  // include(":feature-ims")
  // include(":feature-store")
  // include(":feature-audit")
  // include(":feature-inventory")
  ```

- [ ] Delete or move these modules:
  - Move `feature-ims/` to parent Garment project directory
  - Move `feature-store/` to parent Garment project directory  
  - Move `feature-audit/` to parent Garment project directory
  - OR comment out in `settings.gradle.kts` temporarily

**Success Criteria**: 
- No IMS/Store/Audit dependencies in `app/build.gradle.kts`
- Gradle sync succeeds without those modules
- Dependencies shown are attendance-only

---

### **Step 2: Clean Up Navigation** 🔴 *Critical*
**Goal**: Remove all IMS/Store/Audit routes from navigation

- [ ] Review `feature-navigation/src/main/kotlin/.../AppNavigation.kt`
- [ ] Remove any conditional logic for `officeAttendance` showing IMS/Store/Audit
- [ ] Ensure single unified route set for both flavors
- [ ] Update route names to be vocabulary-neutral or flavor-aware:
  ```kotlin
  // Example: Use vocabulary abstraction
  @Composable
  fun AppNavigation() {
      val navController = rememberNavController()
      NavHost(navController, startDestination = "attendance_home") {
          composable("attendance_home") { AttendanceHomeScreen() }
          composable("biometric_enrollment") { BiometricEnrollmentScreen() }
          composable("attendance_history") { AttendanceHistoryScreen() }
          composable("profile") { ProfileScreen() }
          // NO IMS/Store/Audit routes
      }
  }
  ```

- [ ] Remove `BuildConfig.FLAVOR` conditional route logic (not needed anymore)

**Success Criteria**: 
- Navigation shows only attendance routes
- Both flavors share identical route structure
- No conditional compilation for routes

---

### **Step 3: Update Manifest Configuration** ⏳ *Pending*
**Goal**: Set up flavor-specific app names and vocabulary

- [ ] Update `app/src/main/AndroidManifest.xml`:
  - Remove hardcoded app label
  - Use `${appName}` placeholder

- [ ] Create flavor-specific manifests (if needed):
  - `app/src/schoolAttendance/AndroidManifest.xml` - label: "AzuraTime School"
  - `app/src/officeAttendance/AndroidManifest.xml` - label: "AzuraTime Office"

- [ ] Verify permissions are identical for both flavors

**Success Criteria**: 
- Correct app names in launcher for each flavor
- No manifest conflicts
- Same permissions approved for both

---

### **Step 4: Implement Vocabulary Abstraction** ⏳ *Pending*
**Goal**: Create centralized vocabulary system for UI text

- [ ] Create `VocabularyProvider` class in `feature-attendance-core`:
  ```kotlin
  object VocabularyProvider {
      val userType: String
      val sessionType: String
      val checkInLabel: String
      val checkOutLabel: String
      // etc.
      
      init {
          when (BuildConfig.FLAVOR) {
              "schoolAttendance" -> {
                  userType = "Student"
                  sessionType = "Class"
                  // ...
              }
              "officeAttendance" -> {
                  userType = "Staff"
                  sessionType = "Shift"
                  // ...
              }
          }
      }
  }
  ```

- [ ] Create flavor-specific string resources:
  - `app/src/schoolAttendance/res/values/strings.xml`
  - `app/src/officeAttendance/res/values/strings.xml`

- [ ] Update UI components to use vocabulary abstraction

**Success Criteria**: 
- All user-facing text is vocabulary-aware
- Adding new flavors requires only string resource updates
- No hardcoded "Student" or "Staff" in code

---

### **Step 5: Build Validation** ⏳ *Pending*
**Goal**: Verify both flavors compile and run correctly

- [ ] Run `./gradlew :app:assembleSchoolAttendanceDebug`
  - [ ] Build succeeds
  - [ ] APK contains only attendance features
  - [ ] App name: "AzuraTime School"
  - [ ] UI shows "Student", "Class", "Teacher" terminology

- [ ] Run `./gradlew :app:assembleOfficeAttendanceDebug`
  - [ ] Build succeeds
  - [ ] APK contains only attendance features (same as school)
  - [ ] App name: "AzuraTime Office"
  - [ ] UI shows "Staff", "Shift", "Employee" terminology

- [ ] Manual testing:
  - [ ] Both flavors have identical functionality
  - [ ] Only vocabulary differs
  - [ ] No IMS/Store/Audit features visible in either

**Success Criteria**: Both flavors build successfully with identical attendance features and vocabulary-appropriate UI.

---

## 🧭 Navigation Rules

### **Unified Navigation (Both Flavors)**

Both flavors share the **exact same navigation structure**:

```kotlin
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(navController, startDestination = "attendance_home") {
        // Core attendance routes
        composable("attendance_home") { 
            AttendanceHomeScreen() 
        }
        composable("biometric_enrollment") { 
            BiometricEnrollmentScreen() 
        }
        composable("attendance_history") { 
            AttendanceHistoryScreen() 
        }
        composable("attendance_capture") { 
            AttendanceCaptureScreen() 
        }
        composable("profile") { 
            ProfileScreen() 
        }
        composable("settings") { 
            SettingsScreen() 
        }
        
        // No conditional routes based on flavor
        // Vocabulary is handled inside each screen
    }
}
```

### **Vocabulary in UI**

Each screen uses vocabulary abstraction:

```kotlin
@Composable
fun AttendanceHomeScreen() {
    val vocabulary = VocabularyProvider
    
    Column {
        Text(text = "Welcome, ${vocabulary.userType}")
        Button(onClick = { /* check in */ }) {
            Text(text = vocabulary.checkInLabel) // "Check In" or "Start Shift"
        }
        // Screen logic identical, text changes based on flavor
    }
}
```

---

## 🔍 Current Status Summary

| Step | Status | Progress |
|------|--------|----------|
| 1. Remove Garment Dependencies | 🔴 *Critical - START HERE* | 0% |
| 2. Clean Up Navigation | 🔴 *Critical* | 0% |
| 3. Update Manifest Configuration | ⏳ Pending | 0% |
| 4. Implement Vocabulary Abstraction | ⏳ Pending | 0% |
| 5. Build Validation | ⏳ Pending | 0% |

**Blockers**: None
**Next Action**: **IMMEDIATE** - Remove IMS/Store/Audit dependencies from `app/build.gradle.kts` and `settings.gradle.kts`.

---

## 📝 Important Notes

- **Garment Business Modules**: The `:feature-ims`, `:feature-store`, `:feature-audit` modules are for a **completely separate project** (Garment/Textile management). They should be:
  - Removed from this repository entirely, OR
  - Moved to a parent "Garment" project directory, OR
  - Commented out in `settings.gradle.kts` if maintaining in same repo

- **Single Codebase**: Both flavors share 100% of the attendance code. Vocabulary differences are 100% UI/text.

- **Future Maintainers**: Do NOT add business logic (IMS, Store, Audit) to this project. Those belong in separate repositories.

- **Test Strategy**: Write tests once, run against both flavors to verify vocabulary switching works correctly.

---

## 🚨 IMMEDIATE ACTION REQUIRED

**STOP** any work on IMS/Store/Audit integration. These are **NOT part of this attendance system**.

**NEXT**: Execute Step 1 to remove all Garment business module references from the build configuration.

---

**Document Owner**: Architecture Team  
**Review Cycle**: Every major sprint  
**Status**: Active - Critical Refactoring Phase