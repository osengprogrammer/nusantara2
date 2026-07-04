# Modularization Plan: Scaling Features (`store`, `ims`, `payment`, `booking`)

This plan outlines the roadmap to decouple `store`, `ims`, `payment`, and `booking` from the monolithic `:app` module using a contract-first, modular architecture.

## Phase 1: Core Contract Definition
Define the "rules of engagement" in a new `:core-api` module.

- **Step 1.1**: Create a `:core-api` library module.
- **Step 1.2**: Define clean Kotlin interface contracts for each feature (e.g., `StoreManager`, `ImsService`, `PaymentProcessor`, `BookingEngine`).
- **Step 1.3**: Move shared data models (DTOs) used by these features into `:core-api`.

*Verification: :core-api must have zero dependencies on Android or Hilt.*

## Phase 2: Feature Modularization
Follow the pattern successfully established by the `:ml-engine` extraction.

- **Step 2.1**: Create `:feature-store`, `:feature-ims`, `:feature-payment`, and `:feature-booking` library modules.
- **Step 2.2**: Migrate existing code for each feature into its respective module.
- **Step 2.3**: Implement the interfaces defined in `:core-api`.

*Verification: Ensure each module builds independently (`./gradlew :feature-name:assembleDebug`).*

## Phase 3: Dependency Injection "Glue"
Implement flavor-swapping logic using Hilt.

- **Step 3.1**: Create flavor-specific Hilt modules (e.g., `SchoolFeatureModule`, `OfficeFeatureModule`).
- **Step 3.2**: Use `@Provides` or `@Binds` in these modules to swap implementations based on `BuildConfig.FLAVOR`.
- **Step 3.3**: Use Hilt's `@InstallIn` to ensure these modules are only included in the appropriate flavor builds.

*Verification: Perform a clean build for both `school` and `office` flavors and verify the correct implementation is injected.*

## Phase 4: App Module Slimming
- **Step 4.1**: Strip out the moved logic from the `:app` module.
- **Step 4.2**: Implement an interface-based "Navigation Router" to handle cross-feature navigation without direct dependencies.

---
### Key Architectural Principles
- **Circular Dependency Prevention**: Features only depend on `:core-api`, never on each other.
- **Flavor Independence**: DI swaps implementations; the app code remains agnostic.
- **Scalability**: Adding a new flavor (e.g., `hospital`) requires only a new Hilt module, not code modification.
