package com.azuratech.azuratime.utils

import android.content.Context
import com.azuratech.azuratime.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 🌱 DATABASE SEEDER (Pure-Class 2.0)
 * Ensures the workspace and default classes exist on the first run.
 */
object DatabaseSeeder {

    suspend fun seedIfNeeded(context: Context) {
        withContext(Dispatchers.IO) {
            val database = AppDatabase.getInstance(context)
            val userDao = database.userDao()
            val classDao = database.classDao() // 🔥 FIXED: Using classDao instead of optionDao

            val existingUsers = userDao.getAllUsersOnce()
            
            if (existingUsers.isEmpty()) {
                
                // 1. Create Default Workspace
                val defaultSchoolId = "AZURA-SCHOOL-${UUID.randomUUID().toString().take(8)}"
                val schoolName = "Azura Academy"

                // 2. Seed Default Admin User
                val defaultAdmin = UserEntity(
                    userId = UUID.randomUUID().toString(),
                    email = "admin@azuratech.com",
                    name = "Admin Azura",
                    memberships = mapOf(
                        defaultSchoolId to Membership(
                            schoolName = schoolName,
                            role = "ADMIN" // Pure-Class role
                        )
                    ),
                    activeSchoolId = defaultSchoolId,
                    isActive = true,
                    activeClassId = null,
                    deviceId = null,
                    createdAt = System.currentTimeMillis()
                )
                
                userDao.insertUser(defaultAdmin)
                
                // 3. Seed Default Classes (Pure-Class 2.0 Logic)
                seedDefaultClasses(classDao, defaultSchoolId, defaultAdmin.userId)
            }
        }
    }

    private suspend fun seedDefaultClasses(classDao: ClassDao, schoolId: String, accountId: String) {
        val existingClasses = classDao.getClassesBySchoolOnce(schoolId)
        
        if (existingClasses.isEmpty()) {
            val defaultClasses = listOf(
                ClassEntity(
                    id = "CLASS-${UUID.randomUUID()}",
                    accountId = accountId,
                    schoolId = schoolId,
                    name = "X IPA 1"
                ),
                ClassEntity(
                    id = "CLASS-${UUID.randomUUID()}",
                    accountId = accountId,
                    schoolId = schoolId,
                    name = "XI IPA 1"
                ),
                ClassEntity(
                    id = "CLASS-${UUID.randomUUID()}",
                    accountId = accountId,
                    schoolId = schoolId,
                    name = "XII IPA 1"
                )
            )
            // 🔥 FIXED: Standard insertAll for ClassEntity
            classDao.insertAll(defaultClasses)
        }
    }
}