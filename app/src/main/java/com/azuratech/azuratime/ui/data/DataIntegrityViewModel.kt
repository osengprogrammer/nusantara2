package com.azuratech.azuratime.ui.data

import androidx.lifecycle.ViewModel
import com.azuratech.azuratime.data.repository.DataIntegrityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class DataIntegrityViewModel @Inject constructor(
    private val repository: DataIntegrityRepository
) : ViewModel() {

    val totalFaces: Flow<Int> = repository.totalFaces
    val missingAssignment: Flow<Int> = repository.missingAssignment
    val brokenAssignments: Flow<Int> = repository.brokenAssignments
    val unsyncedCount: Flow<Int> = repository.globalUnsyncedCount

    fun getIncompleteProfiles(type: String) = repository.getIncompleteProfiles(type)
}
