# Migration Plan: Extracting ML & Security to `ml-engine`

## Phase 0 – Audit & Preparation

- **0.1** Pre‑migration audit: list custom Gradle tasks that reference `com.azuratech.azuratime.core.security` (e.g., `encryptModel`).
- **0.2** Document the current classpath construction of the `encryptModel` task.

*Result of audit:* only `encryptModel` is present (see `app/build.gradle.kts` line 238).

---

## Phase 1 – Foundation

- **1.1** Scaffold a new Android library module **`ml-engine`** with the `com.android.library` and Kotlin plugins.
- **1.2** Match the library’s `compileSdk`, `minSdk`, and `targetSdk` to the app module.
- **1.3** Verify the module builds independently: `./gradlew :ml-engine:assembleDebug`.

---

## Phase 2 – Dependency Migration

- **2.1** In `ml-engine/build.gradle.kts` add all ML Kit, TensorFlow Lite, and security‑related dependencies that currently live in `app/build.gradle.kts`.
- **2.2** Declare the security packages as **`api`** (instead of `implementation`) so they are exposed to the consuming project and to custom tasks.
- **2.3** Verify with `./gradlew :ml-engine:dependencies` that the libraries appear.

---

## Phase 3 – Source & Asset Migration

- **3.1** Move Kotlin sources under `com.azuratech.azuratime.core.security` and any ML‑related packages (`com.azuratech.azuratime.ml*`) from `app/src/main/kotlin` to `ml-engine/src/main/kotlin`, preserving the exact package names.
- **3.2** Move ML assets (e.g., `.tflite` models) from `app/src/main/assets` to `ml-engine/src/main/assets` and adjust the code to load them via `context.assets`.
- **3.3** Ensure the library compiles after the move.

---

## Phase 4 – Integration & Task Patching (The "Solid" Part)

- **4.1** In `app/build.gradle.kts` add the library dependency:
  ```kotlin
  implementation(project(":ml-engine"))
  ```
- **4.2** **Patch `encryptModel`** to use the library’s compiled output and add an explicit dependency so the library builds first:
  ```kotlin
  tasks.register<JavaExec>("encryptModel") {
      // … existing configuration …
      // Ensure the library is compiled before the task runs
      dependsOn(":ml-engine:compileDebugKotlin")

      // Build the classpath from the library output + runtime classpath
      val compileLib = project(":ml-engine")
          .tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileDebugKotlin")
      classpath = compileLib.get().outputs.files + configurations.runtimeClasspath
      // … rest of the task …
  }
  ```
- **4.3** Verify that `./gradlew encryptModel` succeeds and can locate `ModelEncryptorKt`.

---

## Phase 5 – Dependency Cleanup

- **5.1** Remove the now‑redundant ML Kit, TensorFlow, and security dependencies from `app/build.gradle.kts`.
- **5.2** Run `./gradlew :app:dependencies` to confirm they are no longer present.

---

## Phase 6 – Import Reconciliation

- **6.1** Run a project‑wide search for moved imports (e.g., `grep -R "core.security"` and `grep -R "ml"`).
- **6.2** Update any stale imports if the package name changed (we kept the original package names, so most imports stay unchanged).
- **6.3** Ensure the code compiles with no "Unresolved reference" errors.

---

## Phase 7 – Verification

- **7.1** Perform a **no‑clean** build of a single flavor:
  ```bash
  ./gradlew assembleSchoolDebug
  ```
- **7.2** Run unit/instrumentation tests (`./gradlew test` / `./gradlew connectedAndroidTest`).
- **7.3** Manually launch the app on a device/emulator and verify that all ML features (face detection, barcode scanning, TensorFlow inference) and security‑related functionality (model encryption/decryption) work as expected.

---

## Phase 8 – Finalization

- **8.1** Stage all changes, commit with a clear message:
  
  ```text
  refactor: extract ML Kit, TensorFlow, and Security logic to ml-engine library
  ```
- **8.2** Push the commit and open a PR for review.

---

### Summary
This plan keeps the build working after every step, handles the `encryptModel` task’s class‑path dependency by adding an explicit `dependsOn`, and isolates heavy ML/Security code in a dedicated library for faster incremental builds.
