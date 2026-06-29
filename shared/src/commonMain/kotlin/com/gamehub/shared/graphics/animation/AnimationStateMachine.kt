package com.gamehub.shared.graphics.animation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class StateTransition<S, E>(
    val from: S,
    val event: E,
    val to: S,
    val onEnter: (suspend () -> Unit)? = null
)

class AnimationStateMachine<S, E>(
    initialState: S,
    private val transitions: List<StateTransition<S, E>>,
    private val coroutineScope: CoroutineScope
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state

    private var currentJob: Job? = null

    fun transition(event: E) {
        val transition = transitions.find { it.from == _state.value && it.event == event }
        transition?.let { t ->
            currentJob?.cancel()
            _state.value = t.to
            t.onEnter?.let { onEnter ->
                currentJob = coroutineScope.launch {
                    onEnter()
                }
            }
        }
    }

    fun forceState(state: S) {
        currentJob?.cancel()
        _state.value = state
    }
}
