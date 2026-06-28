package com.azuratech.azuratime.features.dashboard.domain.repository

import com.azuratech.azuratime.features.account.domain.model.Account

/**
 * 🏠 DASHBOARD REPOSITORY (v3.9.0)
 * Domain interface to decouple Dashboard feature from Room DB details.
 */
interface DashboardRepository {
    suspend fun updateAccount(account: Account)
}
