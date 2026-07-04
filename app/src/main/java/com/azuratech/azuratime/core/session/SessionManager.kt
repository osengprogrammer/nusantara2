package com.azuratech.azuratime.core.session

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // ---------------------------------------------------------------------
    // Ready‑check flow
    // ---------------------------------------------------------------------
    // Starts as false and flips to true once the encrypted SharedPreferences
    // (or its fallback) have been accessed and any initial data has been read.
    // This is the "green light" that BootViewModel will observe before it
    // touches any session‑dependent logic.
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    init {
        // Load prefs on a background thread and then mark ready.
        // Using a dedicated CoroutineScope avoids tying SessionManager to a
        // ViewModelScope; it lives for the lifetime of the process.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                // Trigger a read of a known key to force EncryptedSharedPreferences
                // initialization. Any exception is logged but does not prevent the
                // ready flag from being set – we still want the app to continue.
                sharedPreferences.getString(KEY_ACCOUNT_ID, null)
                Log.d(TAG, "SessionManager: preferences loaded, ready flag set")
            } catch (e: Exception) {
                Log.e(TAG, "SessionManager: error loading preferences", e)
            } finally {
                _ready.value = true
            }
        }
    }

    init {
        synchronized(SessionManager::class.java) {
            INSTANCE = this
        }
    }

    companion object {
        private const val TAG = "AZURA_SESSION"
        const val STATUS_GUEST = "GUEST"
        const val STATUS_PENDING = "PENDING"
        const val STATUS_ACTIVE = "ACTIVE"

        private const val PREF_NAME = "azura_secure_session"
        private const val KEY_DB_CLOUD = "db_cloud_key"
        private const val KEY_ACCOUNT_STATUS = "account_status"
        private const val KEY_EXPIRE_DATE = "expire_date"
        private const val KEY_LAST_SYNC = "last_sync_time"
        private const val KEY_LAST_FACES_SYNC = "last_faces_sync_time"
        private const val KEY_LAST_CLASSES_SYNC = "last_classes_sync_time"
        private const val KEY_LAST_RECORDS_SYNC = "last_records_sync_time"
        private const val KEY_ACCOUNT_EMAIL = "account_email"
        private const val KEY_ACCOUNT_ID = "current_account_id"
        private const val KEY_ACTIVE_SCHOOL_ID = "active_school_id"
        private const val KEY_ACCOUNT_ROLE = "account_role"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val sharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            Log.e(TAG, "FATAL: EncryptedSharedPreferences failed. Falling back to regular prefs.", e)
            context.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE)
        }
    }

    private val _activeSchoolIdFlow = MutableStateFlow<String?>(getActiveSchoolId())
    val activeSchoolIdFlow: StateFlow<String?> = _activeSchoolIdFlow.asStateFlow()

    private val _currentAccountIdFlow = MutableStateFlow<String?>(
        try { getCurrentAccountId() } catch (e: Exception) { null },
    )
    val currentAccountIdFlow: StateFlow<String?> = _currentAccountIdFlow.asStateFlow()

    private val _isLoggingOutFlow = MutableStateFlow(false)
    val isLoggingOutFlow: StateFlow<Boolean> = _isLoggingOutFlow.asStateFlow()

    private val _isSessionClearingFlow = MutableStateFlow(false)
    val isSessionClearingFlow: StateFlow<Boolean> = _isSessionClearingFlow.asStateFlow()

    fun setLoggingOut(isLoggingOut: Boolean) {
        _isLoggingOutFlow.value = isLoggingOut
    }

    fun initializeEncryption() {
        try {
            sharedPreferences
            Log.d(TAG, "Encryption initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Encryption initialization failed.", e)
        }
    }

    fun saveActiveSchoolId(schoolId: String) {
        sharedPreferences.edit().putString(KEY_ACTIVE_SCHOOL_ID, schoolId).apply()
        _activeSchoolIdFlow.value = schoolId
    }

    fun getActiveSchoolId(): String? {
        val id = sharedPreferences.getString(KEY_ACTIVE_SCHOOL_ID, null)
        return if (id.isNullOrBlank()) null else id
    }

    fun saveCurrentAccountId(accountId: String) {
        sharedPreferences.edit().putString(KEY_ACCOUNT_ID, accountId).apply()
        _currentAccountIdFlow.value = accountId
    }

    fun getCurrentAccountId(): String? = sharedPreferences.getString(KEY_ACCOUNT_ID, null)

    fun saveAccountEmail(email: String) {
        sharedPreferences.edit().putString(KEY_ACCOUNT_EMAIL, email).apply()
    }

    fun getAccountEmail(): String = sharedPreferences.getString(KEY_ACCOUNT_EMAIL, "") ?: ""

    fun saveAccountStatus(status: String) {
        sharedPreferences.edit().putString(KEY_ACCOUNT_STATUS, status).apply()
    }

    fun getAccountStatus(): String = sharedPreferences.getString(KEY_ACCOUNT_STATUS, STATUS_PENDING) ?: STATUS_PENDING

    fun saveAccountRole(role: String) {
        sharedPreferences.edit().putString(KEY_ACCOUNT_ROLE, role.trim()).apply()
    }

    fun getAccountRole(): String = sharedPreferences.getString(KEY_ACCOUNT_ROLE, "USER")?.trim() ?: "USER"

    fun getExpireDate(): Long = sharedPreferences.getLong(KEY_EXPIRE_DATE, 0L)

    fun getCloudKey(): String = sharedPreferences.getString(KEY_DB_CLOUD, "") ?: ""

    fun saveLastSyncTime(millis: Long = System.currentTimeMillis()) {
        sharedPreferences.edit().putLong(KEY_LAST_SYNC, millis).apply()
    }

    fun getLastSyncTime(): Long = sharedPreferences.getLong(KEY_LAST_SYNC, 0L)

    fun saveLastFacesSyncTime(millis: Long = System.currentTimeMillis()) {
        sharedPreferences.edit().putLong(KEY_LAST_FACES_SYNC, millis).apply()
    }

    fun getLastFacesSyncTime(): Long = sharedPreferences.getLong(KEY_LAST_FACES_SYNC, 0L)

    fun saveLastClassesSyncTime(millis: Long = System.currentTimeMillis()) {
        sharedPreferences.edit().putLong(KEY_LAST_CLASSES_SYNC, millis).apply()
    }

    fun getLastClassesSyncTime(): Long = sharedPreferences.getLong(KEY_LAST_CLASSES_SYNC, 0L)

    fun saveLastRecordsSyncTime(millis: Long = System.currentTimeMillis()) {
        sharedPreferences.edit().putLong(KEY_LAST_RECORDS_SYNC, millis).apply()
    }

    fun getLastRecordsSyncTime(): Long = sharedPreferences.getLong(KEY_LAST_RECORDS_SYNC, 0L)

    fun getHardwareId(): String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"

    fun injectSecurityEnvelope(isoKey: String, expireDateMillis: Long, role: String? = null) {
        val finalRole = role ?: getAccountRole()
        sharedPreferences.edit().apply {
            putString(KEY_DB_CLOUD, isoKey)
            putLong(KEY_EXPIRE_DATE, expireDateMillis)
            putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            putString(KEY_ACCOUNT_STATUS, STATUS_ACTIVE)
            putString(KEY_ACCOUNT_ROLE, finalRole.trim())
        }.apply()
        Log.d(TAG, "Security envelope injected successfully.")
    }

    suspend fun clearSession() {
        _isLoggingOutFlow.value = true
        _isSessionClearingFlow.value = true

        withContext(Dispatchers.IO) {
            sharedPreferences.edit().clear().commit()
            Log.d(TAG, "Local session cleared successfully.")
        }

        _activeSchoolIdFlow.value = null
        _currentAccountIdFlow.value = null

        _isLoggingOutFlow.value = false
        _isSessionClearingFlow.value = false
    }
}
