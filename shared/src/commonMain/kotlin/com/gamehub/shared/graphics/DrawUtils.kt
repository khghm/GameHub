package com.gamehub.shared.graphics

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*

object DrawUtils {
    fun DrawScope.drawGlowingCircle(
        center: Offset,
        radius: Float,
        color: Color,
        glowRadius: Float = radius * 0.5f,
        alpha: Float = 1f,
        glowColor: Color? = null
    ) {
        val finalGlowColor = glowColor ?: color.copy(alpha = alpha * 0.4f)
        drawCircle(
            color = finalGlowColor,
            radius = radius + glowRadius,
            center = center
        )
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = radius,
            center = center
        )
    }

    fun DrawScope.drawGlowingRect(
        topLeft: Offset,
        size: Size,
        color: Color,
        glowColor: Color? = null,
        glowRadius: Float = 15f,
        alpha: Float = 1f,
        cornerRadius: Float = 0f
    ) {
        val finalGlowColor = glowColor ?: color.copy(alpha = alpha * 0.4f)
        // Draw glow (slightly larger)
        val glowTopLeft = topLeft - Offset(glowRadius, glowRadius)
        val glowSize = Size(size.width + glowRadius * 2, size.height + glowRadius * 2)
        drawRoundRect(
            color = finalGlowColor,
            topLeft = glowTopLeft,
            size = glowSize,
            cornerRadius = CornerRadius(cornerRadius)
        )
        // Draw main rect
        drawRoundRect(
            color = color.copy(alpha = alpha),
            topLeft = topLeft,
            size = size,
            cornerRadius = CornerRadius(cornerRadius)
        )
    }

    fun DrawScope.drawPolygon(
        sides: Int,
        center: Offset,
        radius: Float,
        color: Color,
        rotation: Float = 0f,
        style: androidx.compose.ui.graphics.drawscope.DrawStyle = androidx.compose.ui.graphics.drawscope.Fill
    ) {
        val points = mutableListOf<Offset>()
        for (i in 0 until sides) {
            val angle = (i * 2 * PI / sides + rotation * PI / 180).toFloat()
            points.add(
                Offset(
                    center.x + radius * cos(angle),
                    center.y + radius * sin(angle)
                )
            )
        }
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
                close()
            },
            color = color,
            style = style
        )
    }

    fun DrawScope.drawStar(
        center: Offset,
        outerRadius: Float,
        innerRadius: Float,
        points: Int = 5,
        color: Color,
        rotation: Float = 0f,
        style: androidx.compose.ui.graphics.drawscope.DrawStyle = androidx.compose.ui.graphics.drawscope.Fill
    ) {
        val path = androidx.compose.ui.graphics.Path()
        for (i in 0 until points * 2) {
            val angle = (i * PI / points + rotation * PI / 180).toFloat()
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val x = center.x + radius * cos(angle)
            val y = center.y + radius * sin(angle)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        drawPath(path = path, color = color, style = style)
    }

    fun Rect.expand(amount: Float): Rect {
        return Rect(
            left = left - amount,
            top = top - amount,
            right = right + amount,
            bottom = bottom + amount
        )
    }

    fun Size.scale(factor: Float): Size {
        return Size(width * factor, height * factor)
    }
}
