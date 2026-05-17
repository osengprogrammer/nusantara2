package com.azuratech.azuratime.core.session

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AZURA_SESSION"
        const val STATUS_GUEST = "GUEST"
        const val STATUS_PENDING = "PENDING"
        const val STATUS_ACTIVE = "ACTIVE"

        private const val PREF_NAME = "azura_secure_session"
        private const val KEY_DB_CLOUD = "db_cloud_key"
        private const val KEY_USER_STATUS = "user_status"
        private const val KEY_EXPIRE_DATE = "expire_date"
        private const val KEY_LAST_SYNC = "last_sync_time"
        private const val KEY_LAST_FACES_SYNC = "last_faces_sync_time"
        private const val KEY_LAST_CLASSES_SYNC = "last_classes_sync_time"
        private const val KEY_LAST_RECORDS_SYNC = "last_records_sync_time"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ID = "current_user_id"
        private const val KEY_ACTIVE_SCHOOL_ID = "active_school_id"

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
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "FATAL: EncryptedSharedPreferences failed. Falling back to regular prefs.", e)
            context.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE)
        }
    }

    // 🔥 REACTIVE TRIGGER: SSOT flows for user and school context
    private val _activeSchoolIdStateFlow = MutableStateFlow<String?>(getActiveSchoolId())
    val activeSchoolIdStateFlow: StateFlow<String?> = _activeSchoolIdStateFlow.asStateFlow()

    private val _currentUserIdStateFlow = MutableStateFlow<String?>(
        try { getCurrentUserId() } catch (e: Exception) { null }
    )
    val currentUserIdStateFlow: StateFlow<String?> = _currentUserIdStateFlow.asStateFlow()

    // =====================================================
    // IDENTITAS & TENANT
    // =====================================================

    fun saveActiveSchoolId(schoolId: String) {
        sharedPreferences.edit().putString(KEY_ACTIVE_SCHOOL_ID, schoolId).apply()
        _activeSchoolIdStateFlow.value = schoolId
    }

    fun getActiveSchoolId(): String? {
        val id = sharedPreferences.getString(KEY_ACTIVE_SCHOOL_ID, null)
        return if (id.isNullOrBlank()) null else id
    }

    fun saveCurrentUserId(userId: String) {
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply()
        _currentUserIdStateFlow.value = userId
    }

    fun getCurrentUserId(): String? = sharedPreferences.getString(KEY_USER_ID, null)

    // 🔥 RESTORED MISSING FUNCTIONS
    fun saveUserEmail(email: String) {
        sharedPreferences.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String = sharedPreferences.getString(KEY_USER_EMAIL, "") ?: ""

    fun saveUserStatus(status: String) {
        sharedPreferences.edit().putString(KEY_USER_STATUS, status).apply()
    }

    fun getUserStatus(): String = sharedPreferences.getString(KEY_USER_STATUS, STATUS_PENDING) ?: STATUS_PENDING

    fun getExpireDate(): Long = sharedPreferences.getLong(KEY_EXPIRE_DATE, 0L)

    fun getCloudKey(): String = sharedPreferences.getString(KEY_DB_CLOUD, "") ?: ""

    // =====================================================
    // SYNC & SECURITY
    // =====================================================

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

    fun injectSecurityEnvelope(isoKey: String, expireDateMillis: Long) {
        sharedPreferences.edit().apply {
            putString(KEY_DB_CLOUD, isoKey)
            putLong(KEY_EXPIRE_DATE, expireDateMillis)
            putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            putString(KEY_USER_STATUS, STATUS_ACTIVE)
        }.apply()
        Log.d(TAG, "Security envelope injected successfully.")
    }

    // =====================================================
    // LOGOUT & CLEANUP
    // =====================================================

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
        Log.d(TAG, "Sesi lokal berhasil dibersihkan.")
    }
}