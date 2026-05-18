# 🤖 AI-Native Feature Template (v3.2.0)

When creating or refactoring features using the AI-Native MVI architecture, strictly adhere to the following template:

## 1. Domain/Data Layer (`Result<T>`)
- Repository methods **must** return `com.azuratech.azuraengine.result.Result<T>`.
- Do **not** use `try/catch` in the ViewModel. Exceptions must be caught in the Data/Repository layer and mapped to `Result.Failure(AppError)`.

## 2. UI State (`*UiState.kt`)
- Define a single immutable `data class` representing the complete screen state.
- Include base properties: `isLoading: Boolean`, `error: String?`.

```kotlin
data class FeatureUiState(
    val isLoading: Boolean = false,
    val data: List<MyModel> = emptyList(),
    val error: String? = null
)
```

## 3. UI Events (`*UiEvent.kt`)
- Define user intentions using a `sealed class`.

```kotlin
sealed class FeatureUiEvent {
    data class LoadData(val param: String) : FeatureUiEvent()
    data object Retry : FeatureUiEvent()
}
```

## 4. ViewModel (`*ViewModel.kt`)
- MUST include the exact header: `* 🚀 FEATURE NAME VIEW MODEL (v3.2.0-ai-native)` in the KDoc class comment.
- Expose a single `val uiState: StateFlow<FeatureUiState>`.
- Provide a single `fun onEvent(event: FeatureUiEvent)` entry point with a `when(event)` block.
- Handle `Result` using `.onSuccess` and `.onFailure` explicitly.

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val repository: FeatureRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(FeatureUiState())
    val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()

    fun onEvent(event: FeatureUiEvent) {
        when (event) {
            is FeatureUiEvent.LoadData -> load(event.param)
            // ...
        }
    }

    private fun load(param: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getData(param)
                .onSuccess { data -> _uiState.update { it.copy(isLoading = false, data = data) } }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
        }
    }
}
```

## 5. UI Screen (`*Screen.kt`)
- Observe state via `collectAsStateWithLifecycle()`.
- Handle Loading, Error, Empty, and Success states explicitly.

## 6. Previews (`*PreviewMocks.kt`)
- Create mock state generators (`loading()`, `success()`, `error()`).
- Add `@Preview` annotations at the bottom of the Screen file for each state.