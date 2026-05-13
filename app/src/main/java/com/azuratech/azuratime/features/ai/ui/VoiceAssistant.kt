package com.azuratech.azuratime.ui.ai

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

class VoiceAssistant(context: Context) {
    private var tts: TextToSpeech? = null

    init {
        // 1. Maximize the media volume automatically
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)

        // 2. Initialize the TTS Engine
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(1.0f)
            }
        }
    }

    fun speak(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}

/**
 * A Compose helper that automatically manages the lifecycle of the VoiceAssistant.
 * It initializes when the screen opens and shuts down when the screen closes.
 */
@Composable
fun rememberVoiceAssistant(): VoiceAssistant {
    val context = LocalContext.current
    val voiceAssistant = remember { VoiceAssistant(context) }

    DisposableEffect(Unit) {
        onDispose {
            voiceAssistant.shutdown()
        }
    }

    return voiceAssistant
}