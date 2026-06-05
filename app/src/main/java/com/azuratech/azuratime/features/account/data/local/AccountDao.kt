package com.azuratech.azuratime.features.account.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Upsert
    suspend fun upsertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE accountId = :id LIMIT 1")
    suspend fun getAccountById(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE email = :email LIMIT 1")
    suspend fun getAccountByEmail(email: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE accountId = :id")
    fun observeAccountByIdFlow(id: String): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts")
    fun observeAllAccountsFlow(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccountsOnce(): List<AccountEntity>

    @Query("DELETE FROM accounts WHERE accountId = :id")
    suspend fun deleteAccountById(id: String)
}
