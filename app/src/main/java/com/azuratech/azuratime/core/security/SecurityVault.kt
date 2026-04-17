package com.azuratech.azuratime.security

class SecurityVault {

    init {
        // Make sure the parenthesis and quotes are exactly like this:
        System.loadLibrary("azura_security_vault")
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
        isoKey: String
    ): Int
}