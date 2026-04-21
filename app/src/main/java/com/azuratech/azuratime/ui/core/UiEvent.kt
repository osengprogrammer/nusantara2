package com.azuratech.azuratime.ui.core

sealed class UiEvent {
    data class ShowSnackbar(val message: String, val action: String? = null) : UiEvent()
    object NavigateUp : UiEvent()
    data class NavigateTo(val route: String) : UiEvent()
}
