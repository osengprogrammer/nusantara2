package com.azuratech.azuratime.domain.school.usecase

import com.azuratech.azuratime.data.local.AppDatabase
import com.azuratech.azuratime.data.repo.SchoolRepository
import com.azuratech.azuratime.data.repo.UserRepository
import javax.inject.Inject

class ArchiveInactiveSchoolsUseCase @Inject constructor(
    private val database: AppDatabase,
    private val schoolRepository: SchoolRepository,
    private val userRepository: UserRepository
) {
    suspend fun execute() {
        val allSchools = database.schoolClassDao().getAllSchoolsOnce()
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        
        val allUsers = userRepository.getUserDao().getAllUsersOnce()
        
        allSchools.forEach { school ->
            if (school.status == "ACTIVE" && school.createdAt < thirtyDaysAgo) {
                // Check if any user has this school in their memberships
                val userCount = allUsers.count { it.memberships.containsKey(school.id) }
                
                if (userCount == 0) {
                    println("🍂 Archiving inactive school: ${school.name} (id=${school.id})")
                    val updatedSchool = school.toDomain().copy(status = "ARCHIVED")
                    schoolRepository.saveSchool(updatedSchool)
                }
            }
        }
    }
}
