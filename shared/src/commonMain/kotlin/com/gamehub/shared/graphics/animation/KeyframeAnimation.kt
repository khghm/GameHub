package com.gamehub.shared.graphics.animation

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class KeyframeAnimator<T, V : AnimationVector>(
    private val scope: CoroutineScope,
    private val initialValue: T,
    private val typeConverter: TwoWayConverter<T, V>
) where T : Any {
    private val _value = Animatable(initialValue, typeConverter)
    val value: State<T> = _value.asState()
    private var currentJob: Job? = null

    fun animate(
        targetValue: T,
        keyframes: KeyframesSpec.KeyframesSpecConfig<T>.() -> Unit,
        durationMillis: Int = 1000
    ) {
        currentJob?.cancel()
        currentJob = scope.launch {
            _value.animateTo(
                targetValue,
                animationSpec = keyframes {
                    this.durationMillis = durationMillis
                    keyframes()
                }
            )
        }
    }

    fun snapTo(value: T) {
        currentJob?.cancel()
        currentJob = scope.launch {
            _value.snapTo(value)
        }
    }

    fun stop() {
        currentJob?.cancel()
        currentJob = null
        scope.launch {
            _value.stop()
        }
    }
}

@Composable
fun <T, V : AnimationVector> rememberKeyframeAnimator(
    initialValue: T,
    typeConverter: TwoWayConverter<T, V>
): KeyframeAnimator<T, V> where T : Any {
    val scope = rememberCoroutineScope()
    return remember(initialValue, typeConverter) {
        KeyframeAnimator(scope, initialValue, typeConverter)
    }
}
