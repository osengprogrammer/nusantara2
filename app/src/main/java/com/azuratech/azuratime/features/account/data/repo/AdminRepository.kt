package com.azuratech.azuratime.features.account.data.repo

import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.features.account.data.local.AccountEntity
import com.azuratech.azuratime.core.data.local.AccountClassAccessEntity
import com.azuratech.azuratime.features.account.data.local.Membership
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
    private val accountDao = database.accountDao()
    private val accountClassAccessDao = database.accountClassAccessDao()

    // Simple Delegation
    fun getAccountDao() = accountDao
    fun getAccountClassAccessDao() = accountClassAccessDao
    
    fun observeAccountsForSchool(schoolId: String): Flow<List<AccountEntity>> =
        accountDao.observeAllAccounts().map { accounts ->
            accounts.filter { it.memberships.containsKey(schoolId) }
        }
}
