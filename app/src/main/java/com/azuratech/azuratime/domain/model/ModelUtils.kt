package com.azuratech.azuratime.domain.model

import android.content.Context
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object ModelUtils {
    /**
     * Memuat model TFLite menggunakan Memory Mapping (mmap).
     * Sangat efisien karena tidak membebani RAM aplikasi secara langsung.
     */
    fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        
        // Memetakan file model ke memori virtual
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        
        // Catatan: Kita tidak menutup stream secara manual di sini karena 
        // Interpreter TFLite membutuhkan akses ke pemetaan ini selama masa hidupnya.
    }
}