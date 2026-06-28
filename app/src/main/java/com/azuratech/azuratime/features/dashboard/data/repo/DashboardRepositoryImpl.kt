package com.azuratech.azuratime.features.dashboard.data.repo

import com.azuratech.azuratime.features.account.data.local.AccountDao
import com.azuratech.azuratime.features.account.domain.model.Account
import com.azuratech.azuratime.features.dashboard.domain.repository.DashboardRepository
import javax.inject.Inject

/**
 * 🏠 DASHBOARD REPOSITORY IMPLEMENTATION (v3.9.0)
 * Concrete repository implementation managing Account Room operations for Dashboard.
 */
class DashboardRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
) : DashboardRepository {

    override suspend fun updateAccount(account: Account) {
        val existing = accountDao.getAccountById(account.accountId)
        if (existing != null) {
            val updated = existing.copy(
                email = account.email,
                name = account.name,
                photoUrl = account.photoUrl,
                status = account.status,
                role = account.role.name,
                activeSchoolId = account.activeSchoolId,
                activeClassId = account.activeClassId,
                memberships = account.memberships.mapValues { (_, membership) ->
                    com.azuratech.azuratime.features.account.data.local.SchoolMembership(
                        schoolName = membership.schoolName,
                        role = membership.role,
                        status = membership.status,
                        assignments = membership.assignments,
                    )
                },
                syncStatus = account.syncStatus,
            )
            accountDao.updateAccount(updated)
        }
    }
}
