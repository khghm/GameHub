package com.gamehub.shared.graphics.utils

import androidx.compose.ui.geometry.Offset
import kotlin.math.*

// Operator functions for Offset scalar arithmetic
operator fun Offset.times(scalar: Float): Offset = Offset(x * scalar, y * scalar)
operator fun Offset.div(scalar: Float): Offset = Offset(x / scalar, y / scalar)
operator fun Offset.plus(other: Offset): Offset = Offset(x + other.x, y + other.y)
operator fun Offset.minus(other: Offset): Offset = Offset(x - other.x, y - other.y)
operator fun Offset.unaryMinus(): Offset = Offset(-x, -y)
operator fun Float.times(offset: Offset): Offset = Offset(this * offset.x, this * offset.y)

infix fun Offset.dot(other: Offset): Float = x * other.x + y * other.y

infix fun Offset.cross(other: Offset): Float = x * other.y - y * other.x

fun Offset.perpendicular(): Offset = Offset(-y, x)

fun Offset.normalized(): Offset {
    val len = sqrt(x * x + y * y)
    return if (len > 0f) this / len else Offset(1f, 0f)
}

fun Offset.rotate(angle: Float): Offset {
    val cos = cos(angle)
    val sin = sin(angle)
    return Offset(x * cos - y * sin, x * sin + y * cos)
}

fun Offset.getDistanceSquared(): Float = x*x + y*y

fun Offset.getDistance(): Float = sqrt(getDistanceSquared())
