package com.gamehub.shared.graphics.architecture

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

// ==================== Event Base Class ====================
open class GameEvent

// ==================== Event Bus ====================
class EventBus {
    internal val _events = MutableSharedFlow<GameEvent>(
        extraBufferCapacity = 64
    )
    val events = _events.asSharedFlow()

    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Send an event
     */
    fun send(event: GameEvent) {
        _events.tryEmit(event)
    }

    /**
     * Subscribe to specific event type
     */
    inline fun <reified T : GameEvent> subscribe(
        scope: CoroutineScope = this.scope,
        noinline onEvent: (T) -> Unit
    ): Job {
        return events
            .filterIsInstance<T>()
            .onEach { onEvent(it) }
            .launchIn(scope)
    }

    /**
     * Subscribe to all events
     */
    fun subscribeAll(
        scope: CoroutineScope = this.scope,
        onEvent: (GameEvent) -> Unit
    ): Job {
        return events
            .onEach(onEvent)
            .launchIn(scope)
    }
}

// ==================== Example Game Events ====================
class InputTapEvent(val x: Float, val y: Float) : GameEvent()
class GameStateChangeEvent(val newState: String) : GameEvent()
class CollisionEvent(val bodyA: Any, val bodyB: Any) : GameEvent()
