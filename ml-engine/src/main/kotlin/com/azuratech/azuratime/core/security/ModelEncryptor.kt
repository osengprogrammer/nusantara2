package com.azuratech.azuratime.core.security

import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 🛠️ AZURA MODEL ENCRYPTOR UTILITY (v1.0.0)
 * Standalone Kotlin script / Main class to encrypt standard .tflite models into secured .azr assets.
 * Uses the exact same AES-GCM 256-bit key and 12-byte IV parameters as CryptoEngine.
 *
 * HOW TO RUN IN ANDROID STUDIO / INTELLIJ:
 * 1. Open this file in your editor.
 * 2. Click the green "Play" icon next to the "fun main()" function declaration.
 * 3. Or run it via command line inside the project root:
 *    ./gradlew :app:run (if configured) or compile and execute using kotlinc/java.
 */
fun main() {
    // Modify these paths as necessary for your local setup!
    val inputTfLitePath = "model/facenet_azura_512.tflite" // Your raw .tflite input file
    val outputAzrPath = "ml-engine/src/main/assets/facenet_azura_512.azr" // Secure output asset

    println("==================================================")
    println("🔐 Azura Time Model Encryptor")
    println("==================================================")

    val inputFile = File(inputTfLitePath)
    val outputFile = File(outputAzrPath)

    if (!inputFile.exists()) {
        println("❌ Error: Source file not found at: ${inputFile.absolutePath}")
        println("👉 Please copy your raw .tflite model to that location or update inputTfLitePath in this script.")
        return
    }

    println("Reading raw model: ${inputFile.absolutePath} (${inputFile.length()} bytes)")

    try {
        val rawBytes = inputFile.readBytes()

        // 1. Static Configuration (Must align 100% with CryptoEngine.kt)
        val staticKeyBytes = byteArrayOf(
            0x41.toByte(), 0x7a.toByte(), 0x75.toByte(), 0x72.toByte(), 0x61.toByte(), 0x54.toByte(), 0x69.toByte(), 0x6d.toByte(),
            0x65.toByte(), 0x53.toByte(), 0x65.toByte(), 0x63.toByte(), 0x75.toByte(), 0x72.toByte(), 0x65.toByte(), 0x4b.toByte(),
            0x65.toByte(), 0x79.toByte(), 0x32.toByte(), 0x30.toByte(), 0x32.toByte(), 0x36.toByte(), 0x4d.toByte(), 0x6f.toByte(),
            0x64.toByte(), 0x65.toByte(), 0x6c.toByte(), 0x41.toByte(), 0x45.toByte(), 0x53.toByte(), 0x32.toByte(), 0x35.toByte(),
        )

        val staticIvBytes = byteArrayOf(
            0x41.toByte(), 0x7a.toByte(), 0x75.toByte(), 0x72.toByte(), 0x61.toByte(), 0x4e.toByte(),
            0x6f.toByte(), 0x6e.toByte(), 0x63.toByte(), 0x65.toByte(), 0x39.toByte(), 0x36.toByte(),
        )

        val transformation = "AES/GCM/NoPadding"
        val gcmTagLength = 128

        println("Initializing Cipher: $transformation...")
        val cipher = Cipher.getInstance(transformation)
        val keySpec = SecretKeySpec(staticKeyBytes, "AES")
        val spec = GCMParameterSpec(gcmTagLength, staticIvBytes)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec)

        println("Encrypting data...")
        val encryptedBytes = cipher.doFinal(rawBytes)

        // Create parent directories if they don't exist
        outputFile.parentFile?.mkdirs()
        outputFile.writeBytes(encryptedBytes)

        println("✅ Success! Encrypted model written to: ${outputFile.absolutePath}")
        println("📝 Original size: ${rawBytes.size} bytes")
        println("📝 Encrypted size: ${encryptedBytes.size} bytes (includes 16-byte GCM authentication tag)")
        println("==================================================")

        // Quick self-verification test to ensure perfect match
        println("🔄 Running self-verification decryption test...")
        val decryptCipher = Cipher.getInstance(transformation)
        decryptCipher.init(Cipher.DECRYPT_MODE, keySpec, spec)
        val decryptedBytes = decryptCipher.doFinal(encryptedBytes)

        if (decryptedBytes.contentEquals(rawBytes)) {
            println("✅ VERIFICATION SUCCESS: Decrypted bytes match original bytes 100%!")

            // Check FlatBuffer Signature of decrypted bytes
            val sigBytes = decryptedBytes.sliceArray(4..7)
            val sig = String(sigBytes, Charsets.US_ASCII)
            val first4Bytes = decryptedBytes.sliceArray(0..3)
            val first4Sig = String(first4Bytes, Charsets.US_ASCII)
            println("🔍 Decrypted FlatBuffer Identifier (Bytes 4-7): '$sig' (Hex: ${sigBytes.joinToString(" ") { "0x%02X".format(it) }})")
            println("🔍 Decrypted Fallback Identifier (Bytes 0-3): '$first4Sig' (Hex: ${first4Bytes.joinToString(" ") { "0x%02X".format(it) }})")
        } else {
            println("❌ VERIFICATION FAILED: Decrypted bytes do not match original bytes!")
        }
        println("==================================================")
    } catch (e: Exception) {
        println("❌ Encryption failed: ${e.message}")
        e.printStackTrace()
    }
}
