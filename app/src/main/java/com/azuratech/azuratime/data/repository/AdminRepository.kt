package com.azuratech.azuratime.data.repository

import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.local.UserEntity
import com.azuratech.azuratime.data.local.UserClassAccessEntity
import com.azuratech.azuratime.data.local.Membership
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏰 ADMIN REPOSITORY
 * Thin wrapper for Admin Data Sources.
 */
@Singleton
class AdminRepository @Inject constructor(
    private val database: AppDatabase,
    private val db: FirebaseFirestore
) {
    private val userDao = database.userDao()
    private val userClassAccessDao = database.userClassAccessDao()

    // Simple Delegation
    fun getUserDao() = userDao
    fun getUserClassAccessDao() = userClassAccessDao
    
    fun observeUsersForSchool(schoolId: String): Flow<List<UserEntity>> =
        userDao.observeAllUsers().map { users ->
            users.filter { it.memberships.containsKey(schoolId) }
        }
        
    // Logic here should be moved to UseCases in a later phase if it gets complex, 
    // but for now we've removed the legacy watchers.
}
