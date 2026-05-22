package com.azuratech.azuratime.features.student.ui.roster

/**
 * 🎓 STUDENT ROSTER BARCODE UI EVENT
 */
sealed class StudentRosterBarcodeUiEvent {
    object LoadData : StudentRosterBarcodeUiEvent()
    data class ToggleSelection(val studentId: String) : StudentRosterBarcodeUiEvent()
    object SelectAll : StudentRosterBarcodeUiEvent()
    object DeselectAll : StudentRosterBarcodeUiEvent()
    object ExportSelected : StudentRosterBarcodeUiEvent()
}
