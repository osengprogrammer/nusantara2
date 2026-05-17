package com.azuratech.azuratime.core.security

class SecurityVault {

    companion object {
        var isNativeReady = false
            private set

        init {
            try {
                // Make sure the parenthesis and quotes are exactly like this:
                System.loadLibrary("azura_security_vault")
                isNativeReady = true
            } catch (e: UnsatisfiedLinkError) {
                android.util.Log.e("SecurityVault", "CRITICAL: Native library azura_security_vault not found!", e)
            }
        }
    }

    /**
     * Native implementation to prevent tampering with logic in Java.
     * @return 1 (OK), -1 (Clock Mismatch), -2 (Revoked), -3 (Pending), -4 (Expired)
     */
    external fun checkAccessStatus(
        lastSync: Long,
        expireDate: Long,
        status: String,
        hardwareId: String,
        isoKey: String,
    ): Int
}
