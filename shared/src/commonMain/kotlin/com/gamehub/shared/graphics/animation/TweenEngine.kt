package com.gamehub.shared.graphics.animation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Easing Functions for Tweens
 */
object EasingFunctions {
    val LINEAR: (Float) -> Float = { t -> t }
    val EASE_IN_SINE: (Float) -> Float = { t -> 1f - cos(t * PI.toFloat() / 2f) }
    val EASE_OUT_SINE: (Float) -> Float = { t -> sin(t * PI.toFloat() / 2f) }
    val EASE_IN_OUT_SINE: (Float) -> Float = { t -> -(cos(PI.toFloat() * t) - 1f) / 2f }
    val EASE_IN_QUAD: (Float) -> Float = { t -> t * t }
    val EASE_OUT_QUAD: (Float) -> Float = { t -> t * (2f - t) }
    val EASE_IN_OUT_QUAD: (Float) -> Float = { t ->
        if (t < 0.5f) 2f * t * t else -1f + (4f - 2f * t) * t
    }
    val EASE_IN_CUBIC: (Float) -> Float = { t -> t * t * t }
    val EASE_OUT_CUBIC: (Float) -> Float = { t -> (t - 1f) * (t - 1f) * (t - 1f) + 1f }
    val EASE_IN_OUT_CUBIC: (Float) -> Float = { t ->
        if (t < 0.5f) 4f * t * t * t else (t - 1f) * (2f * t - 2f) * (2f * t - 2f) + 1f
    }
    val EASE_OUT_BOUNCE: (Float) -> Float = { t ->
        val n1 = 7.5625f
        val d1 = 2.75f
        when {
            t < 1f / d1 -> n1 * t * t
            t < 2f / d1 -> n1 * (t - 1.5f / d1) * (t - 1.5f / d1) + 0.75f
            t < 2.5f / d1 -> n1 * (t - 2.25f / d1) * (t - 2.25f / d1) + 0.9375f
            else -> n1 * (t - 2.625f / d1) * (t - 2.625f / d1) + 0.984375f
        }
    }
}

/**
 * Tween - smooth interpolation between values
 */
class Tween<T>(
    val startValue: T,
    val endValue: T,
    val durationMs: Long,
    val easing: (Float) -> Float = EasingFunctions.LINEAR,
    private val lerp: (T, T, Float) -> T
) {
    private var elapsedMs: Long = 0L
    private var isPlaying: Boolean = false

    val progress: Float get() = (elapsedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    val currentValue: T get() = lerp(startValue, endValue, easing(progress))
    val isFinished: Boolean get() = progress >= 1f

    /**
     * Start or restart the tween
     */
    fun start() {
        elapsedMs = 0L
        isPlaying = true
    }

    /**
     * Update the tween (call every frame)
     */
    fun update(deltaMs: Long) {
        if (!isPlaying) return
        elapsedMs = (elapsedMs + deltaMs).coerceAtMost(durationMs)
        if (isFinished) {
            isPlaying = false
        }
    }

    /**
     * Pause the tween
     */
    fun pause() {
        isPlaying = false
    }

    /**
     * Resume the tween
     */
    fun resume() {
        isPlaying = true
    }

    /**
     * Jump to a specific progress (0..1)
     */
    fun seekTo(progress: Float) {
        elapsedMs = (progress.coerceIn(0f, 1f) * durationMs).toLong()
    }
}

/**
 * Tween Engine - manages and updates multiple tweens
 */
class TweenEngine {
    private val tweens = mutableListOf<Tween<*>>()

    /**
     * Add a tween to the engine
     */
    fun add(tween: Tween<*>) {
        tweens.add(tween)
        tween.start()
    }

    /**
     * Remove a tween from the engine
     */
    fun remove(tween: Tween<*>) {
        tweens.remove(tween)
    }

    /**
     * Update all tweens (call every frame)
     */
    fun update(deltaMs: Long) {
        val finished = mutableListOf<Tween<*>>()
        tweens.forEach { tween ->
            tween.update(deltaMs)
            if (tween.isFinished) finished.add(tween)
        }
        finished.forEach { tweens.remove(it) }
    }

    /**
     * Pause all tweens
     */
    fun pauseAll() {
        tweens.forEach { it.pause() }
    }

    /**
     * Resume all tweens
     */
    fun resumeAll() {
        tweens.forEach { it.resume() }
    }

    /**
     * Clear all tweens
     */
    fun clear() {
        tweens.clear()
    }

    /**
     * Helper functions for common types
     */
    companion object {
        fun floatTween(start: Float, end: Float, durationMs: Long, easing: (Float) -> Float) =
            Tween(start, end, durationMs, easing) { a, b, t -> a + (b - a) * t }

        fun offsetTween(start: Offset, end: Offset, durationMs: Long, easing: (Float) -> Float) =
            Tween(start, end, durationMs, easing) { a, b, t ->
                Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
            }

        fun colorTween(start: Color, end: Color, durationMs: Long, easing: (Float) -> Float) =
            Tween(start, end, durationMs, easing) { a, b, t ->
                Color(
                    alpha = a.alpha + (b.alpha - a.alpha) * t,
                    red = a.red + (b.red - a.red) * t,
                    green = a.green + (b.green - a.green) * t,
                    blue = a.blue + (b.blue - a.blue) * t
                )
            }
    }
}
