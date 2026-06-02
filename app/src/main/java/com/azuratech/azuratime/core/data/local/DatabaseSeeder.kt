package com.azuratech.azuratime.core.data.local

import android.content.Context
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.features.account.data.local.SchoolMembership
import com.azuratech.azuratime.features.school.data.local.ClassDao
import com.azuratech.azuratime.features.school.data.local.ClassEntity
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
            val accountDao = database.accountDao()
            val schoolDao = database.schoolDao() // 🔥 Added: Need schoolDao to seed schools
            val classDao = database.classDao() // 🔥 FIXED: Using classDao instead of optionDao

            val existingAccounts = accountDao.getAllAccountsOnce()

            if (existingAccounts.isEmpty()) {
                // 1. Create Default Workspace
                val defaultSchoolId = "AZURA-SCHOOL-${UUID.randomUUID().toString().take(8)}"
                val schoolName = "Azura Academy"

                // 1.5. Seed Default School Entity (REQUIRED for Foreign Key Constraints)
                val defaultSchool = com.azuratech.azuratime.features.school.data.local.SchoolEntity(
                    id = defaultSchoolId,
                    accountId = "SYSTEM", // Placeholder for seeded data
                    name = schoolName,
                    timezone = "Asia/Jakarta",
                    status = "ACTIVE",
                )
                schoolDao.insertSchool(defaultSchool)

                // 2. Seed Default Admin Account
                val defaultAdmin = AccountEntity(
                    accountId = UUID.randomUUID().toString(),
                    email = "admin@azuratech.com",
                    name = "Admin Azura",
                    memberships = mapOf(
                        defaultSchoolId to SchoolMembership(
                            schoolName = schoolName,
                            role = "ADMIN", // Pure-Class role
                        ),
                    ),
                    activeSchoolId = defaultSchoolId,
                    status = "ACTIVE",
                    activeClassId = null,
                    createdAt = System.currentTimeMillis(),
                )

                accountDao.upsertAccount(defaultAdmin)

                // 3. Seed Default Classes (Pure-Class 2.0 Logic)
                seedDefaultClasses(classDao, defaultSchoolId, defaultAdmin.accountId)
            }
        }
    }

    private suspend fun seedDefaultClasses(classDao: ClassDao, schoolId: String, accountId: String) {
        val existingClasses = classDao.getClassesBySchoolOnce(schoolId)

        if (existingClasses.isEmpty()) {
            val defaultClasses = listOf(
                ClassEntity(
                    id = "CLASS-${UUID.randomUUID()}",
                    ownerAccountId = accountId,
                    schoolId = schoolId,
                    name = "X IPA 1",
                ),
                ClassEntity(
                    id = "CLASS-${UUID.randomUUID()}",
                    ownerAccountId = accountId,
                    schoolId = schoolId,
                    name = "XI IPA 1",
                ),
                ClassEntity(
                    id = "CLASS-${UUID.randomUUID()}",
                    ownerAccountId = accountId,
                    schoolId = schoolId,
                    name = "XII IPA 1",
                ),
            )
            // 🔥 FIXED: Standard insertAll for ClassEntity
            classDao.insertAll(defaultClasses)
        }
    }
}
