package com.azuratech.azuratime.features.update.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🚀 UPDATE EVENT BUS (v3.2.0-ai-native)
 * Bridges FCM/Intents to the AppUpdate MVI flow.
 */
@Singleton
class UpdateEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun triggerUpdateCheck() {
        _events.tryEmit(Unit)
    }
}
