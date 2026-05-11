package com.azuratech.azuratime.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffAccountDao {

    // =====================================================
    // 📖 READ OPERATIONS
    // =====================================================

    // THE NEW GOLD STANDARD: Get user by UUID
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): StaffAccountEntity?

    // Legacy / Search (Used for invites and login flow)
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): StaffAccountEntity?

    // REQUIRED FOR SEEDER: Check if any users exist in the DB
    @Query("SELECT * FROM users")
    suspend fun getAllUsersOnce(): List<StaffAccountEntity>

    // Real-time UI: Observe the logged-in user's data
    @Query("SELECT * FROM users WHERE userId = :userId")
    fun observeUserById(userId: String): Flow<StaffAccountEntity?>

    // 🔥 Observe ALL users. 
    // The Repository handles the multi-tenant filtering using Kotlin to parse the JSON memberships map.
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun observeAllUsers(): Flow<List<StaffAccountEntity>>

    // =====================================================
    // ✍️ WRITE OPERATIONS
    // =====================================================

    // Initial Registration or Sync from Firestore
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: StaffAccountEntity)

    @Update
    suspend fun updateUser(user: StaffAccountEntity)

    // Phase 1 Security: Hardware Binding
    @Query("UPDATE users SET deviceId = :deviceId WHERE userId = :userId")
    suspend fun bindDevice(userId: String, deviceId: String)

    // Admin Control: Activate/Deactivate accounts
    @Query("UPDATE users SET isActive = :status WHERE userId = :userId")
    suspend fun setUserStatus(userId: String, status: Boolean)

    // Set Active Class (For the scanner session)
    @Query("UPDATE users SET activeClassId = :classId WHERE userId = :userId")
    suspend fun setActiveClass(userId: String, classId: String?)

    // =====================================================
    // 🗑️ DELETE OPERATIONS
    // =====================================================

    @Delete
    suspend fun deleteUser(user: StaffAccountEntity)

    @Query("DELETE FROM users")
    suspend fun clearAllUsers()
}

typealias UserDao = StaffAccountDao