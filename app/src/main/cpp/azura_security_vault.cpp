#include <jni.h>
#include <string>
#include <chrono>

extern "C"
JNIEXPORT jint JNICALL
Java_com_azuratech_azuratime_utils_SecurityVault_checkAccessStatus(
        JNIEnv *env, jobject thiz, 
        jlong last_sync, jlong expire_date, 
        jstring status, jstring hardware_id, jstring iso_key) {

    const char *nativeStatus = env->GetStringUTFChars(status, nullptr);
    std::string statusStr(nativeStatus);
    
    // 1. Check if Status is PENDING
    if (statusStr == "PENDING") {
        env->ReleaseStringUTFChars(status, nativeStatus);
        return -3;
    }

    // 2. Check if Status is not ACTIVE
    if (statusStr != "ACTIVE") {
        env->ReleaseStringUTFChars(status, nativeStatus);
        return -2;
    }

    // 3. Time Validation (Check if license expired)
    auto now = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()).count();

    if (now > expire_date) {
        env->ReleaseStringUTFChars(status, nativeStatus);
        return -4;
    }

    // 4. Clock Tamper Check (Current time shouldn't be before last sync)
    if (now < last_sync) {
        env->ReleaseStringUTFChars(status, nativeStatus);
        return -1; // System clock manipulation detected
    }

    env->ReleaseStringUTFChars(status, nativeStatus);
    return 1; // Success
}