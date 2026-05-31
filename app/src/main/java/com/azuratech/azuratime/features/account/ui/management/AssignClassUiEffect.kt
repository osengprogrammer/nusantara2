package com.azuratech.azuratime.features.account.ui.management

sealed class AssignClassUiEffect {
    data class ShowSnackbar(val message: String) : AssignClassUiEffect()
    data object NavigateBack : AssignClassUiEffect()
}
