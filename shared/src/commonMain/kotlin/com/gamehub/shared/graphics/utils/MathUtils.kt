package com.gamehub.shared.graphics.utils

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

object MathUtils {
    const val PI_F = PI.toFloat()
    const val TWO_PI = PI_F * 2f
    const val HALF_PI = PI_F / 2f
    const val QUARTER_PI = PI_F / 4f
    const val DEG_TO_RAD = PI_F / 180f
    const val RAD_TO_DEG = 180f / PI_F
    
    fun clamp(value: Float, min: Float, max: Float): Float {
        return value.coerceIn(min, max)
    }
    
    fun clamp(value: Int, min: Int, max: Int): Int {
        return value.coerceIn(min, max)
    }
    
    fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t.coerceIn(0f, 1f)
    }
    
    fun lerp(a: Int, b: Int, t: Float): Int {
        return (a + (b - a) * t).roundToInt()
    }
    
    fun smoothStep(t: Float): Float {
        val clamped = t.coerceIn(0f, 1f)
        return clamped * clamped * (3f - 2f * clamped)
    }
    
    fun smootherStep(t: Float): Float {
        val clamped = t.coerceIn(0f, 1f)
        return clamped * clamped * clamped * (clamped * (clamped * 6f - 15f) + 10f)
    }
    
    fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
    
    fun distance(x1: Int, y1: Int, x2: Int, y2: Int): Float {
        return distance(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())
    }
    
    fun map(value: Float, inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
        return (value - inMin) / (inMax - inMin) * (outMax - outMin) + outMin
    }
    
    fun wrapAngle(angle: Float): Float {
        var wrapped = angle
        while (wrapped < -PI_F) wrapped += TWO_PI
        while (wrapped > PI_F) wrapped -= TWO_PI
        return wrapped
    }
    
    fun normalizeAngle(angle: Float): Float {
        var normalized = angle % TWO_PI
        if (normalized < 0) normalized += TWO_PI
        return normalized
    }
}
