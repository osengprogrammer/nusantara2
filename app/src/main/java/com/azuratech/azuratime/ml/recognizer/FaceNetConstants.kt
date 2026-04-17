package com.azuratech.azuratime.ml.recognizer

object FaceNetConstants {
    const val MODEL_NAME = "facenet_azura_512.azr"
    
    const val INPUT_SIZE = 160         
    const val EMBEDDING_SIZE = 512
    
    const val BYTES_PER_CHANNEL = 4
    const val BUFFER_CAPACITY = INPUT_SIZE * INPUT_SIZE * 3 * BYTES_PER_CHANNEL
    
    // Test these later. If model expects [0, 1], change to Mean=0f, Std=255f
    const val IMAGE_MEAN = 127.5f
    const val IMAGE_STD = 128f
    
    // 🔥 FIXED: Safer thresholds for high-dimensional (512) Cosine Distance
    const val DUPLICATE_THRESHOLD = 0.25f 
    const val RECOGNITION_THRESHOLD = 0.45f
    
    const val DEFAULT_FACE_PADDING = 0.1f 

    // 🔥 NEW: Toggle this if distances are unstable (PyTorch/OpenCV models often use BGR)
    const val USE_BGR_COLOR_FORMAT = false
}