#include <jni.h>
#include <string.h>

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_azuratech_azuratime_utils_ModelGuard_decryptTfliteModel(
        JNIEnv *env, jobject thiz, jbyteArray encrypted_bytes) {

    // 1. Ambil array byte dari Kotlin
    jsize length = env->GetArrayLength(encrypted_bytes);
    jbyte *buffer = env->GetByteArrayElements(encrypted_bytes, nullptr);

    // 2. Siapkan array kosong untuk menampung hasil decrypt
    jbyteArray decrypted_bytes = env->NewByteArray(length);
    jbyte *decrypted_buffer = env->GetByteArrayElements(decrypted_bytes, nullptr);

    // =======================================================
    // 🔥 LOGIKA DECRYPT XOR (Bisa diganti dengan AES nanti jika mau)
    // =======================================================
    const char key[] = "KunciAzuraSuperRahasia"; 
    int key_len = strlen(key);

    for (int i = 0; i < length; i++) {
        // Operasi XOR: Membalikkan byte yang terenkripsi menjadi byte asli
        decrypted_buffer[i] = buffer[i] ^ key[i % key_len]; 
    }
    // =======================================================

    // 3. Bebaskan memori agar tidak RAM bocor (Penting!)
    env->ReleaseByteArrayElements(encrypted_bytes, buffer, JNI_ABORT);
    env->ReleaseByteArrayElements(decrypted_bytes, decrypted_buffer, 0);

    // 4. Kirim model yang sudah bersih kembali ke Kotlin
    return decrypted_bytes;
}