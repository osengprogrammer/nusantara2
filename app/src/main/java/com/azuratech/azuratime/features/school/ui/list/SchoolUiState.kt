package com.azuratech.azuratime.features.school.ui.list
import com.azuratech.azuratime.core.data.local.GpsGeofenceEntity
import com.azuratech.azuraengine.model.ClassModel
import com.azuratech.azuraengine.model.School
import com.azuratech.azuratime.core.domain.model.AccountRole



data class SchoolUiState(
    val isLoading: Boolean = false,
    val schools: List<School> = emptyList(),
    val availableClasses: List<ClassModel> = emptyList(),
    val activeSchoolId: String? = null,
    val error: String? = null,
    val accountId: String = "",
    val currentAccountRole: AccountRole = AccountRole.USER,
    val geofence: com.azuratech.azuratime.core.data.local.GpsGeofenceEntity? = null,
    val pickedLocation: com.google.android.gms.maps.model.LatLng? = null,
)
