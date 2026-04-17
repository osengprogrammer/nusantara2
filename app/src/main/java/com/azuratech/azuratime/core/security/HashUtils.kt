package com.azuratech.azuratime.core.security

import java.security.MessageDigest

object HashUtils {

    fun sha256(input: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input)
    }

    fun sha256String(input: String): ByteArray {
        return sha256(input.toByteArray(Charsets.UTF_8))
    }

    fun sha256Hex(input: String): String {
        return sha256String(input)
            .joinToString("") { "%02x".format(it) }
    }
}