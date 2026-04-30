package com.azuratech.azuratime.data.repo

import com.azuratech.azuratime.data.local.*
import com.azuratech.azuratime.domain.checkin.model.AttendanceConflict
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏰 USER REPOSITORY
 * Thin wrapper for User Data Sources.
 */
@Singleton
class UserRepository @Inject constructor(
    private val database: AppDatabase
) {
    private val userDao = database.userDao()
    private val userClassAccessDao = database.userClassAccessDao()

    // Delegation
    fun getUserDao() = userDao
    fun getUserClassAccessDao() = userClassAccessDao

    // State flow for conflicts
    private val _conflicts = MutableStateFlow<List<AttendanceConflict>>(emptyList())
    val conflicts: StateFlow<List<AttendanceConflict>> = _conflicts.asStateFlow()
    
    fun setConflicts(list: List<AttendanceConflict>) { _conflicts.value = list }
}
