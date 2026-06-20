---
name: state-driven-error-handling
description: Guide to implementing state‑driven error handling for addSubject/saveSubject in a Jetpack Compose Android app
source: auto-skill
extracted_at: '2026-06-20T04:07:23.331Z'
---

## Goal
Provide a reusable pattern for propagating backend errors (e.g. duplicate subject name) from the repository up to the UI using a **state‑driven** approach in a Clean‑Architecture Android app built with Jetpack Compose.

## Context
- `SessionManagementViewModel` exposes a `StateFlow<SessionManagementUiState>`.
- `SessionManagementUiState` originally lacked an error field, making UI unaware of failures.
- Errors were emitted via a one‑off `SharedFlow` of UI effects (toast), but this does not survive configuration changes and mixes UI concerns with business‑logic handling.
- The repository returns `Result.Failure(AppError.Conflict)` when a duplicate subject name is detected (via `SQLiteConstraintException`).

## Solution steps
### 1️⃣ Extend the UI state
```kotlin
data class SessionManagementUiState(
    val isLoading: Boolean = false,
    val subjects: List<SubjectEntity> = emptyList(),
    // … other UI fields …
    val error: String? = null,          // ← new field for error messages
)
```
- Keep the field nullable; `null` means “no error”.
- Add a corresponding `ClearError` event in `SessionManagementUiEvent`.

### 2️⃣ Map repository failures to UI state in the ViewModel
```kotlin
private fun addSubject(name: String, description: String?) {
    // loading flag for debounce
    _uiStateFlow.update { it.copy(isLoading = true) }
    // … validation (school, template) …
    viewModelScope.launch {
        // clear any previous error before the call
        _uiStateFlow.update { it.copy(error = null) }
        val result = sessionRepository.saveSubject(subject)
        if (result is Result.Success) {
            _uiEffectFlow.emit(SessionManagementUiEffect.ShowToast("Subject added"))
        } else if (result is Result.Failure) {
            val msg = if (result.error is AppError.Conflict) {
                "Mata pelajaran dengan nama tersebut sudah terdaftar di sekolah ini"
            } else {
                result.error.message ?: "Failed to save subject"
            }
            _uiStateFlow.update { it.copy(error = msg) }
        }
        // always clear loading flag
        _uiStateFlow.update { it.copy(isLoading = false) }
    }
}
```
- Errors are now stored inside `uiState.error`.
- `isLoading` is toggled to allow UI debouncing.
- `ClearError` simply sets `error = null`.

### 3️⃣ Observe the error in Compose with `LaunchedEffect`
```kotlin
val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()

LaunchedEffect(uiState.error) {
    uiState.error?.let { msg ->
        context.showToast(msg)               // or scaffoldState.snackbarHostState.showSnackbar(msg)
        viewModel.onEvent(SessionManagementUiEvent.ClearError)
    }
}
```
- The effect runs only when `error` changes from `null` → non‑null.
- After displaying the message we immediately clear it to avoid repeats on recomposition or device rotation.

### 4️⃣ Debounce UI actions
```kotlin
Button(
    onClick = { viewModel.onEvent(SessionManagementUiEvent.AddSubject(name, desc)) },
    enabled = isNameValid && !uiState.isLoading, // disables while loading
) { … }
```
- Prevents rapid duplicate clicks that could cause multiple repository calls.

## Why this is idiomatic and safe
- **Clean Architecture**: UI only observes immutable state; business logic stays in the ViewModel; repository stays in the domain layer.
- **Thread‑safety**: All repository calls happen inside `viewModelScope` (Dispatcher.Main) and Room internally uses its own I/O thread. No race conditions introduced.
- **State‑driven** errors survive configuration changes (e.g., rotation) because they are part of the `StateFlow`.
- **One‑off UI effects** (`uiEffectFlow`) remain for transient actions like success toasts, while persistent errors are modelled as state.

## Edge‑case handling
- If the user navigates away before the repository response arrives, the loading flag will be cleared when the coroutine finishes; the UI will simply hide the button.
- Multiple error sources (no school, missing template, duplicate name) are all funneled through the same `error` field, providing a single UI entry point.
- The `ClearError` event guarantees the message is shown **once**, even if the composable recomposes many times.

---

**Usage**: Add this skill to `.qwen/skills/auto-skill-state-driven-error-handling/SKILL.md`. Future agents can reference it to replicate the pattern for any other CRUD operation that needs systematic error propagation.
