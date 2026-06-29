package com.gamehub.shared.graphics.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SlideDirection {
    Top, Bottom, Start, End
}

fun Modifier.bounceClick(
    scaleFactor: Float = 0.95f,
    onClick: () -> Unit
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleFactor else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounceScale"
    )
    val updatedOnClick by rememberUpdatedState(onClick)

    this
        .scale(scale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = { updatedOnClick() }
            )
        }
}

fun Modifier.pulse(
    targetScale: Float = 1.1f,
    durationMs: Int = 1000,
    enabled: Boolean = true
): Modifier = composed {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(enabled) {
        if (enabled) {
            while (true) {
                scale.animateTo(
                    targetValue = targetScale,
                    animationSpec = tween(
                        durationMillis = durationMs / 2,
                        easing = EaseInOut
                    )
                )
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = durationMs / 2,
                        easing = EaseInOut
                    )
                )
            }
        } else {
            scale.animateTo(1f)
        }
    }

    this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

fun Modifier.slideIn(
    direction: SlideDirection = SlideDirection.Bottom,
    durationMs: Int = 300,
    delayMs: Int = 0,
    initialOffset: Float = 300f
): Modifier = composed {
    val offset = remember { Animatable(initialOffset) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            offset.animateTo(
                0f,
                animationSpec = tween(
                    durationMillis = durationMs,
                    delayMillis = delayMs,
                    easing = EaseOutBack
                )
            )
        }
        alpha.animateTo(
            1f,
            animationSpec = tween(
                durationMillis = durationMs,
                delayMillis = delayMs,
                easing = EaseOut
            )
        )
    }

    this.graphicsLayer {
        translationX = when (direction) {
            SlideDirection.Start -> -offset.value
            SlideDirection.End -> offset.value
            else -> 0f
        }
        translationY = when (direction) {
            SlideDirection.Top -> -offset.value
            SlideDirection.Bottom -> offset.value
            else -> 0f
        }
        this.alpha = alpha.value
    }
}

fun Modifier.rotateIn(
    durationMs: Int = 500,
    delayMs: Int = 0,
    initialRotation: Float = -180f
): Modifier = composed {
    val rotation = remember { Animatable(initialRotation) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            rotation.animateTo(
                0f,
                animationSpec = tween(
                    durationMillis = durationMs,
                    delayMillis = delayMs,
                    easing = EaseOutBack
                )
            )
        }
        alpha.animateTo(
            1f,
            animationSpec = tween(
                durationMillis = durationMs,
                delayMillis = delayMs,
                easing = EaseOut
            )
        )
    }

    this.graphicsLayer {
        this.rotationZ = rotation.value
        this.alpha = alpha.value
    }
}

fun Modifier.scaleIn(
    durationMs: Int = 400,
    delayMs: Int = 0,
    initialScale: Float = 0f
): Modifier = composed {
    val scale = remember { Animatable(initialScale) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
        alpha.animateTo(
            1f,
            animationSpec = tween(
                durationMillis = durationMs,
                delayMillis = delayMs,
                easing = EaseOut
            )
        )
    }

    this.graphicsLayer {
        this.scaleX = scale.value
        this.scaleY = scale.value
        this.alpha = alpha.value
    }
}

fun Modifier.shake(
    enabled: Boolean = true,
    durationMs: Int = 500,
    shakeStrength: Float = 10f
): Modifier = composed {
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(enabled) {
        if (enabled) {
            val shakeSpec = repeatable<Float>(
                iterations = 5,
                animation = tween(durationMillis = durationMs / 10, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
            offsetX.animateTo(shakeStrength, animationSpec = shakeSpec)
            offsetX.animateTo(0f)
        }
    }

    this.graphicsLayer {
        translationX = offsetX.value
    }
}

fun Modifier.fadeIn(
    durationMs: Int = 300,
    delayMs: Int = 0,
    initialAlpha: Float = 0f
): Modifier = composed {
    val alpha = remember { Animatable(initialAlpha) }
    LaunchedEffect(Unit) {
        alpha.animateTo(
            1f,
            animationSpec = tween(
                durationMillis = durationMs,
                delayMillis = delayMs,
                easing = EaseOut
            )
        )
    }
    this.graphicsLayer {
        this.alpha = alpha.value
    }
}
