package com.gamehub.shared.graphics.animation

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

sealed class SequenceState {
    object Idle : SequenceState()
    object Playing : SequenceState()
    object Paused : SequenceState()
    object Finished : SequenceState()
}

class SequenceAnimation(
    private val scope: CoroutineScope
) {
    private var _currentStep by mutableIntStateOf(0)
    val currentStep: Int get() = _currentStep

    private var _state by mutableStateOf<SequenceState>(SequenceState.Idle)
    val state: SequenceState get() = _state

    private var currentJob: Job? = null
    private var pauseSignal = CompletableDeferred<Unit>()
    private var shouldStop = false

    fun playSequence(steps: List<suspend () -> Unit>) {
        currentJob?.cancel()
        _state = SequenceState.Playing
        pauseSignal = CompletableDeferred()
        pauseSignal.complete(Unit) // Start not paused
        shouldStop = false

        currentJob = scope.launch {
            for ((index, step) in steps.withIndex()) {
                if (shouldStop) break
                pauseSignal.await()
                _currentStep = index
                step()
            }
            _state = SequenceState.Finished
        }
    }

    fun pause() {
        if (_state == SequenceState.Playing) {
            _state = SequenceState.Paused
            pauseSignal = CompletableDeferred()
        }
    }

    fun resume() {
        if (_state == SequenceState.Paused) {
            _state = SequenceState.Playing
            pauseSignal.complete(Unit)
        }
    }

    fun stop() {
        shouldStop = true
        currentJob?.cancel()
        currentJob = null
        _state = SequenceState.Idle
        _currentStep = 0
    }

    fun reset() {
        stop()
    }
}

@Composable
fun rememberSequenceAnimation(): SequenceAnimation {
    val scope = rememberCoroutineScope()
    return remember { SequenceAnimation(scope) }
}
