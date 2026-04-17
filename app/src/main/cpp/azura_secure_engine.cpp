#include <jni.h>

extern "C" {

    JNIEXPORT jfloat JNICALL
    Java_com_azuratech_azuratime_ml_matcher_NativeSecurityVault_calculateDistanceNative(
            JNIEnv* env,
            jobject /* this */,
            jfloatArray liveEmbedding,
            jfloatArray registeredEmbedding) {

        // Use Critical array access for ZERO-COPY memory speed
        jfloat* live = (jfloat*) env->GetPrimitiveArrayCritical(liveEmbedding, nullptr);
        jfloat* registered = (jfloat*) env->GetPrimitiveArrayCritical(registeredEmbedding, nullptr);
        jsize length = env->GetArrayLength(liveEmbedding);

        float dotProduct = 0.0f;

        // Embeddings are ALREADY L2-Normalized in Kotlin. 
        // We only need the dot product!
        for (int i = 0; i < length; i++) {
            dotProduct += live[i] * registered[i];
        }

        // Release critical arrays instantly
        env->ReleasePrimitiveArrayCritical(liveEmbedding, live, JNI_ABORT);
        env->ReleasePrimitiveArrayCritical(registeredEmbedding, registered, JNI_ABORT);

        return 1.0f - dotProduct;
    }

    JNIEXPORT jboolean JNICALL
    Java_com_azuratech_azuratime_ml_matcher_NativeSecurityVault_verifyMatchNative(
            JNIEnv* env,
            jobject /* this */,
            jfloat distance,
            jfloat threshold) {
            
        return (distance < threshold) ? JNI_TRUE : JNI_FALSE;
    }
}