#include <jni.h>
#include <string.h>
#include <android/log.h>

#define LOG_TAG "AZURA_NATIVE_GUARD"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C" {

    /**
     * 🔐 SECURE PROCESS MODEL (v4.1.0-ai-native)
     * Receives decrypted model bytes from Kotlin, duplicates them for return,
     * performs boundary log checks, and executes strict memory scrubbing.
     */
    JNIEXPORT jbyteArray JNICALL
    Java_com_azuratech_azuratime_core_domain_model_ModelGuard_secureProcessModel(
            JNIEnv *env, jobject thiz, jbyteArray decrypted_bytes) {

        jsize length = env->GetArrayLength(decrypted_bytes);
        jbyte *buffer = env->GetByteArrayElements(decrypted_bytes, nullptr);

        jbyteArray result_bytes = env->NewByteArray(length);
        jbyte *result_buffer = env->GetByteArrayElements(result_bytes, nullptr);

        if (buffer != nullptr && result_buffer != nullptr) {
            // Log verification for debugging byte alignment and offsets
            LOGD("Native JNI - Model size received: %d bytes", length);
            if (length > 4) {
                LOGD("Native JNI - First 4 bytes: 0x%02X 0x%02X 0x%02X 0x%02X",
                     (unsigned char)buffer[0], (unsigned char)buffer[1],
                     (unsigned char)buffer[2], (unsigned char)buffer[3]);
                LOGD("Native JNI - Last 4 bytes: 0x%02X 0x%02X 0x%02X 0x%02X",
                     (unsigned char)buffer[length-4], (unsigned char)buffer[length-3],
                     (unsigned char)buffer[length-2], (unsigned char)buffer[length-1]);
            }

            // Copy data to the safe output buffer
            memcpy(result_buffer, buffer, length);

            // Memory Scrubbing: Zero out the temporary decrypted buffer in RAM
            memset(buffer, 0, length);
        }

        // Release array elements (0 writes back zeroed bytes to Kotlin stack/heap memory)
        env->ReleaseByteArrayElements(decrypted_bytes, buffer, 0);
        env->ReleaseByteArrayElements(result_bytes, result_buffer, 0);

        return result_bytes;
    }
}