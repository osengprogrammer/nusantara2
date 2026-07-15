package com.azuratech.azuratime.features.school.ui.list

import com.azuratech.azuratime.features.school.domain.model.School

sealed class SchoolUiEvent {
    data class LoadSchools(val accountId: String) : SchoolUiEvent()
    data class SelectSchool(val school: School) : SchoolUiEvent()
    data class CreateSchool(val name: String, val timezone: String, val selectedClassIds: List<String>) : SchoolUiEvent()
    data class DeleteSchool(val id: String) : SchoolUiEvent()
    data class UpdateSchoolName(val schoolId: String, val newName: String) : SchoolUiEvent()
    data object Retry : SchoolUiEvent()

    // 📍 GPS GEOFENCE
    data class SaveGeofence(
        val schoolId: String,
        val latitude: Double,
        val longitude: Double,
        val radius: Int,
        val isActive: Boolean,
    ) : SchoolUiEvent()

    data class PickLocation(val location: com.google.android.gms.maps.model.LatLng) : SchoolUiEvent()
    data object FetchCurrentLocation : SchoolUiEvent()
}
