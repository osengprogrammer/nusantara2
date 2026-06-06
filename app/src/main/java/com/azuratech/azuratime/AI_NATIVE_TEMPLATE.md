# 🤖 AI-Native Feature Template (v3.2.1)

When creating or refactoring features using the AI-Native MVI architecture, strictly adhere to the following template:

## 1. Domain/Data Layer (`Result<T>`)
- Repository methods **must** return `com.azuratech.azuraengine.result.Result<T>`.
- Flow methods **must** return `Flow<Result<T>>` and end with the `Flow` suffix.
- Exceptions must be caught in the Data/Repository layer and mapped to `Result.Failure(AppError)`.

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

## 3. UI Events & Effects (`*UiEvent.kt` & `*UiEffect.kt`)
- **Events**: Define account intentions (User Actions) using a `sealed class`.
- **Effects**: Define transient actions (Snackbars, Navigation, Toasts) using a `sealed class`.

```kotlin
sealed class FeatureUiEvent {
    data class LoadData(val param: String) : FeatureUiEvent()
    data object Retry : FeatureUiEvent()
}

sealed class FeatureUiEffect {
    data class ShowSnackbar(val message: String) : FeatureUiEffect()
    data class NavigateTo(val route: String) : FeatureUiEffect()
}
```

## 4. ViewModel (`*ViewModel.kt`)
- Expose `val uiStateFlow: StateFlow<FeatureUiState>`.
- Expose `val uiEffectFlow: SharedFlow<FeatureUiEffect>`.
- Provide a single `fun onEvent(event: FeatureUiEvent)` entry point.

```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val repository: FeatureRepository
) : ViewModel() {
    private val _uiStateFlow = MutableStateFlow(FeatureUiState())
    val uiStateFlow: StateFlow<FeatureUiState> = _uiStateFlow.asStateFlow()

    private val _uiEffectFlow = MutableSharedFlow<FeatureUiEffect>()
    val uiEffectFlow = _uiEffectFlow.asSharedFlow()

    fun onEvent(event: FeatureUiEvent) {
        when (event) {
            is FeatureUiEvent.LoadData -> load(event.param)
            // ...
        }
    }

    private fun load(param: String) {
        _uiStateFlow.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getData(param)
                .onSuccess { data -> _uiStateFlow.update { it.copy(isLoading = false, data = data) } }
                .onFailure { error -> 
                    _uiStateFlow.update { it.copy(isLoading = false, error = error.message) }
                    _uiEffectFlow.emit(FeatureUiEffect.ShowSnackbar(error.message ?: "Failed"))
                }
        }
    }
}
```

## 5. UI Screen (`*Screen.kt`)
- Observe state via `collectAsStateWithLifecycle()`.
- Collect effects via `LaunchedEffect(Unit)`.
- Handle Loading, Error, Empty, and Success states explicitly.

## 6. Previews (`*PreviewMocks.kt`)
- Create mock state generators (`loading()`, `success()`, `error()`).
- Add `@Preview` annotations at the bottom of the Screen file for each state.
