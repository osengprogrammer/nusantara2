package com.azuratech.azuratime.core.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentWalletDao {
    @Upsert
    suspend fun upsertWallet(wallet: StudentWalletEntity)

    @Query("SELECT currentBalance FROM student_wallets WHERE studentId = :studentId")
    fun getBalanceFlow(studentId: String): Flow<Double?>

    @Query("SELECT * FROM student_wallets WHERE schoolId = :schoolId")
    fun getAllWalletsBySchool(schoolId: String): Flow<List<StudentWalletEntity>>
}
