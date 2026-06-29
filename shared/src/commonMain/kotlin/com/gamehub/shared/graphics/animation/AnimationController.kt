package com.gamehub.shared.graphics.animation

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AnimationController(private val scope: CoroutineScope) {
    private val _progress = Animatable(0f)
    val progress: State<Float> = _progress.asState()

    fun animateTo(
        targetValue: Float,
        animationSpec: AnimationSpec<Float> = tween(),
        initialVelocity: Float = 0f
    ) {
        scope.launch {
            _progress.animateTo(
                targetValue = targetValue,
                animationSpec = animationSpec,
                initialVelocity = initialVelocity
            )
        }
    }

    fun snapTo(value: Float) {
        scope.launch {
            _progress.snapTo(value)
        }
    }

    fun stop() {
        scope.launch {
            _progress.stop()
        }
    }
}

@Composable
fun rememberAnimationController(): AnimationController {
    val scope = rememberCoroutineScope()
    return remember { AnimationController(scope) }
}
