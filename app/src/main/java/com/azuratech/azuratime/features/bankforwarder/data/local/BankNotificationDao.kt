package com.azuratech.azuratime.features.bankforwarder.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BankNotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: BankNotificationEntity)

    @Query("SELECT * FROM bank_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<BankNotificationEntity>>

    @Query("SELECT * FROM bank_notifications WHERE isProcessed = 0 ORDER BY timestamp DESC")
    suspend fun getUnprocessedNotifications(): List<BankNotificationEntity>

    @Query("UPDATE bank_notifications SET isProcessed = 1 WHERE id = :id")
    suspend fun markAsProcessed(id: String)

    @Query("SELECT * FROM bank_notifications WHERE isSynced = 0")
    suspend fun getUnsyncedNotifications(): List<BankNotificationEntity>

    @Query("UPDATE bank_notifications SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)
}
