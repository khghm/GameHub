package com.gamehub.shared.graphics.math

import kotlinx.serialization.Serializable
import kotlin.math.*

// ==================== Vec2: 2D Vector ====================
@Serializable
data class Vec2(
    var x: Float = 0f,
    var y: Float = 0f
) {
    companion object {
        val Zero = Vec2(0f, 0f)
        val One = Vec2(1f, 1f)
        val Up = Vec2(0f, -1f)
        val Down = Vec2(0f, 1f)
        val Left = Vec2(-1f, 0f)
        val Right = Vec2(1f, 0f)
    }

    // Arithmetic operators
    operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float): Vec2 = Vec2(x * scalar, y * scalar)
    operator fun div(scalar: Float): Vec2 = Vec2(x / scalar, y / scalar)
    operator fun unaryMinus(): Vec2 = Vec2(-x, -y)

    // Dot product
    infix fun dot(other: Vec2): Float = x * other.x + y * other.y

    // Cross product (2D scalar)
    infix fun cross(other: Vec2): Float = x * other.y - y * other.x

    // Vector operations
    fun lengthSquared(): Float = x * x + y * y
    fun length(): Float = sqrt(lengthSquared())
    fun normalized(): Vec2 {
        val len = length()
        return if (len > 0f) this / len else Right
    }
    fun perpendicular(): Vec2 = Vec2(-y, x)
    fun rotate(angle: Float): Vec2 {
        val cos = cos(angle)
        val sin = sin(angle)
        return Vec2(x * cos - y * sin, x * sin + y * cos)
    }
    fun distanceTo(other: Vec2): Float = (this - other).length()
    fun distanceSquaredTo(other: Vec2): Float = (this - other).lengthSquared()
    fun coerceIn(min: Vec2, max: Vec2): Vec2 =
        Vec2(x.coerceIn(min.x, max.x), y.coerceIn(min.y, max.y))
    fun lerp(other: Vec2, t: Float): Vec2 =
        Vec2(x + (other.x - x) * t, y + (other.y - y) * t)
}

// Scalar multiplication (float * Vec2)
operator fun Float.times(vec: Vec2): Vec2 = vec * this

// ==================== Size: 2D Size ====================
data class Size(
    val width: Float = 0f,
    val height: Float = 0f
) {
    companion object {
        val Zero = Size(0f, 0f)
    }
}

// ==================== Rect: 2D Rectangle ====================
data class Rect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
) {
    companion object {
        val Zero = Rect(0f, 0f, 0f, 0f)

        fun fromCenter(center: Vec2, size: Size): Rect {
            val halfWidth = size.width / 2f
            val halfHeight = size.height / 2f
            return Rect(
                left = center.x - halfWidth,
                top = center.y - halfHeight,
                right = center.x + halfWidth,
                bottom = center.y + halfHeight
            )
        }

        fun fromLTWH(left: Float, top: Float, width: Float, height: Float): Rect {
            return Rect(left, top, left + width, top + height)
        }
    }

    // Properties
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val area: Float get() = width * height
    val center: Vec2 get() = Vec2((left + right) / 2f, (top + bottom) / 2f)
    val size: Size get() = Size(width, height)
    val topLeft: Vec2 get() = Vec2(left, top)
    val topRight: Vec2 get() = Vec2(right, top)
    val bottomLeft: Vec2 get() = Vec2(left, bottom)
    val bottomRight: Vec2 get() = Vec2(right, bottom)

    // Operations
    fun contains(point: Vec2): Boolean {
        return point.x in left..right && point.y in top..bottom
    }

    fun contains(other: Rect): Boolean {
        return other.left >= left && other.right <= right &&
                other.top >= top && other.bottom <= bottom
    }

    fun intersects(other: Rect): Boolean {
        return left < other.right && right > other.left &&
                top < other.bottom && bottom > other.top
    }

    fun intersection(other: Rect): Rect {
        val newLeft = max(left, other.left)
        val newTop = max(top, other.top)
        val newRight = min(right, other.right)
        val newBottom = min(bottom, other.bottom)
        return if (newLeft < newRight && newTop < newBottom) {
            Rect(newLeft, newTop, newRight, newBottom)
        } else {
            Zero
        }
    }

    fun expand(amount: Float): Rect {
        return Rect(left - amount, top - amount, right + amount, bottom + amount)
    }

    fun translate(offset: Vec2): Rect {
        return Rect(left + offset.x, top + offset.y, right + offset.x, bottom + offset.y)
    }
}
