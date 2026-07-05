package com.azuratech.azuratime.core.session

/**
 * Minimal SessionManager stub for core-data module.
 * Provides basic session management functions required by AuthRepositoryImpl.
 */
class SessionManager {
    private var _isLoggingOut = false
    private var _currentAccountId: String? = null
    private var _currentAccountEmail: String? = null
    private var _currentAccountStatus: String = ""

    fun setLoggingOut(isLoggingOut: Boolean) {
        _isLoggingOut = isLoggingOut
    }

    fun isLoggingOut(): Boolean = _isLoggingOut

    fun setCurrentAccountId(uid: String) {
        _currentAccountId = uid
    }

    fun getCurrentAccountId(): String? = _currentAccountId

    fun saveCurrentAccountId(uid: String) {
        setCurrentAccountId(uid)
    }

    fun saveAccountEmail(email: String) {
        // No-op stub
    }

    fun getAccountEmail(): String? = _currentAccountEmail

    fun saveAccountStatus(status: String) {
        _currentAccountStatus = status
    }

    fun getAccountStatus(): String = _currentAccountStatus

    fun clearSession() {
        _currentAccountId = null
        _currentAccountEmail = null
        _currentAccountStatus = ""
        setLoggingOut(false)
    }

    fun setActiveSchoolId(schoolId: String) {
        // No-op stub
    }

    fun getActiveSchoolId(): String? = null
}
