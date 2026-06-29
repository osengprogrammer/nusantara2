package com.azuratech.azuratime.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 🔐 CRYPTO ENGINE (v4.0.0-ai-native)
 * Single Source of Truth for cryptographic operations in AzuraTime.
 * Implements hardware-backed AES-GCM 256-bit encryption using Android Keystore.
 */
object CryptoEngine {
    private const val TAG = "AZURA_CRYPTO"
    private const val KEY_ALIAS = "AzuraSecureKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128 // 128-bit authentication tag

    /**
     * Generates a new AES key in Android KeyStore.
     * Attempts to back the key with StrongBox, falling back to TEE if unavailable.
     */
    private fun generateKey(useStrongBox: Boolean = true) {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore",
            )
            val builder = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)

            if (useStrongBox) {
                try {
                    builder.setIsStrongBoxBacked(true)
                } catch (e: NoSuchMethodError) {
                    Log.w(TAG, "StrongBox API not supported on this platform version.")
                }
            }

            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
            Log.d(TAG, "Key generated successfully (StrongBox=$useStrongBox)")
        } catch (e: Exception) {
            if (useStrongBox) {
                Log.w(TAG, "StrongBox key generation failed, falling back to TEE...", e)
                generateKey(useStrongBox = false)
            } else {
                Log.e(TAG, "Critical: Key generation in Android Keystore failed.", e)
                throw e
            }
        }
    }

    /**
     * Retrieves the secret key from KeyStore, generating it if it doesn't exist.
     */
    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            Log.d(TAG, "Key alias '$KEY_ALIAS' not found. Generating new key...")
            generateKey()
        }

        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey ?: throw IllegalStateException("Key alias exists but entry is not a SecretKeyEntry")
    }

    /**
     * Encrypts the raw byte array using AES-GCM-256.
     * @return Pair of (IV, EncryptedData)
     */
    // Static Key & IV for Pre-encrypted Assets (e.g., TFLite model)
    // 32-byte key (256-bit AES) and 12-byte IV (96-bit GCM standard)
    private val STATIC_KEY_BYTES = byteArrayOf(
        0x41.toByte(), 0x7a.toByte(), 0x75.toByte(), 0x72.toByte(), 0x61.toByte(), 0x54.toByte(), 0x69.toByte(), 0x6d.toByte(),
        0x65.toByte(), 0x53.toByte(), 0x65.toByte(), 0x63.toByte(), 0x75.toByte(), 0x72.toByte(), 0x65.toByte(), 0x4b.toByte(),
        0x65.toByte(), 0x79.toByte(), 0x32.toByte(), 0x30.toByte(), 0x32.toByte(), 0x36.toByte(), 0x4d.toByte(), 0x6f.toByte(),
        0x64.toByte(), 0x65.toByte(), 0x6c.toByte(), 0x41.toByte(), 0x45.toByte(), 0x53.toByte(), 0x32.toByte(), 0x35.toByte(),
    )

    private val STATIC_IV_BYTES = byteArrayOf(
        0x41.toByte(), 0x7a.toByte(), 0x75.toByte(), 0x72.toByte(), 0x61.toByte(), 0x4e.toByte(),
        0x6f.toByte(), 0x6e.toByte(), 0x63.toByte(), 0x65.toByte(), 0x39.toByte(), 0x36.toByte(),
    )

    /**
     * Decrypts pre-packaged assets (such as the TFLite model) using a static pre-shared key.
     */
    fun decryptModel(encryptedData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val keySpec = javax.crypto.spec.SecretKeySpec(STATIC_KEY_BYTES, "AES")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, STATIC_IV_BYTES)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec)
        return cipher.doFinal(encryptedData)
    }

    /**
     * Encrypts the raw byte array using AES-GCM-256.
     * @return Pair of (IV, EncryptedData)
     */
    fun encrypt(data: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val encryptedData = cipher.doFinal(data)
        return Pair(cipher.iv, encryptedData)
    }

    /**
     * Decrypts the encrypted data using the provided IV.
     * @return Decrypted raw byte array
     */
    fun decrypt(encryptedData: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        return cipher.doFinal(encryptedData)
    }
}
