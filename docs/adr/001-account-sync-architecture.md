# ADR 001: Account Sync Architecture

## Status
Accepted

## Context
Azura Time requires a robust, offline-first synchronization mechanism to ensure that account data, student profiles, and biometric information are consistent between the local Room database and the remote Firebase Firestore. 

Previously, the synchronization logic was fragmented across different workers (like `ProfileSyncWorker`) and repositories, leading to:
1. Race conditions during data updates.
2. Inconsistent error handling across different sync tasks.
3. Difficulty in testing complex multi-repository synchronization flows.
4. Lack of a unified entry point for background synchronization triggered by significant events (e.g., app launch, manual refresh, or data mutation).

The migration to `AccountSyncWorker` is part of the "v3.2.0-ai-native" architectural push to unify terminology (Account vs. Staff/Teacher) and enforce the Local-First Single Source of Truth (SSOT) pattern.

## Decision
We have implemented a unified `AccountSyncWorker` using Android's WorkManager. 

### Key Design Decisions:
1. **Unified Synchronization**: `AccountSyncWorker` orchestrates the synchronization of `AccountRepository`, `StudentRepository`, and `BiometricRepository` in a single transactional worker flow.
2. **Local-First SSOT**: The worker follows a Pull-then-Push strategy:
   - **Pull**: Fetch latest data from Cloud and update Room (Local SSOT).
   - **Push**: Upload any pending local changes (e.g., new student profiles) to the Cloud.
3. **Structured Result Handling**: Leveraging `com.azuratech.azuraengine.result.Result<T>`, the worker maps domain errors to WorkManager's `Result.success()`, `Result.retry()`, or `Result.failure()` based on the error type (e.g., network errors trigger retries).
4. **Exponential Backoff**: WorkManager is configured to use exponential backoff for retries to ensure system stability during intermittent connectivity.
5. **Robolectric Testing**: Integration tests for the worker are implemented in `src/test` using Robolectric and `TestListenableWorkerBuilder`. This allows for fast, reliable verification of complex sync logic without requiring a physical device or emulator.
6. **DI with Hilt**: The worker uses `@HiltWorker` and `@AssistedInject` for clean dependency injection, while tests use a `DelegatingWorkerFactory` to inject mocks.

## Consequences

### Pros:
- **Offline Resilience**: Users can continue working offline; changes are automatically synced when the connection is restored.
- **Improved Maintainability**: All sync logic is centralized in one place, following the same patterns.
- **Enhanced Testability**: Complex scenarios (network failures, partial successes) can be easily simulated and verified in unit tests.
- **Performance**: WorkManager handles background execution efficiently, respecting system constraints (battery, data usage).

### Cons:
- **Complexity**: Orchestrating multiple repositories in a single worker requires careful error handling to avoid partial sync states.
- **Testing Overhead**: Requires Robolectric and complex mocking of multiple repositories.

## Compliance
This architecture strictly aligns with the **AI-Native v3.2.0** standards:
- **Result<T> Engine**: Uses `asLocalResult()` and `asResult()` for standardized data mapping.
- **Clean Architecture**: Orchestration happens at the repository/worker layer, keeping the UI layer pure.
- **Terminology**: Uses the unified "Account" terminology as per Phase 12.
- **ArchUnit**: Verified via existing ArchUnit rules to ensure no layer violations.

## References
- [ARCHITECTURE_MIGRATION_COMPLETE.md](../ARCHITECTURE_MIGRATION_COMPLETE.md)
- [AI_NATIVE_TEMPLATE.md](../AI_NATIVE_TEMPLATE.md)
- [GEMINI.md](../../GEMINI.md)
