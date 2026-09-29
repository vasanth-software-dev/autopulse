package com.autopulse.automation.event

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NotificationEventBus {

    private val _events = MutableSharedFlow<NotificationEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val events: SharedFlow<NotificationEvent> = _events.asSharedFlow()

    suspend fun publish(event: NotificationEvent) {
        _events.emit(event)
    }

    fun tryPublish(event: NotificationEvent): Boolean {
        return _events.tryEmit(event)
    }
}
