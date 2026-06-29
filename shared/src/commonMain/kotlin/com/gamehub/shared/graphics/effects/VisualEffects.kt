package com.gamehub.shared.graphics.effects

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.glow(
    color: Color = Color.White,
    radius: Dp = 8.dp,
    alpha: Float = 0.5f
): Modifier = this.then(
    Modifier.drawBehind {
        val glowRadius = radius.toPx()
        val glowColor = color.copy(alpha = alpha)
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor, Color.Transparent),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.maxDimension / 2 + glowRadius
            ),
            radius = size.maxDimension / 2 + glowRadius,
            center = Offset(size.width / 2, size.height / 2)
        )
    }
)

fun Modifier.borderGlow(
    color: Color = Color.White,
    radius: Dp = 8.dp,
    borderWidth: Dp = 2.dp,
    alpha: Float = 0.5f
): Modifier = this.then(
    Modifier.drawBehind {
        val glowRadius = radius.toPx()
        val glowColor = color.copy(alpha = alpha)
        val borderWidthPx = borderWidth.toPx()
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor, Color.Transparent),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.maxDimension / 2 + glowRadius
            ),
            radius = size.maxDimension / 2 + glowRadius,
            center = Offset(size.width / 2, size.height / 2)
        )
        
        drawCircle(
            color = color,
            radius = size.maxDimension / 2 - borderWidthPx / 2,
            center = Offset(size.width / 2, size.height / 2),
            style = Stroke(width = borderWidthPx)
        )
    }
)

fun Modifier.gradientBackground(
    colors: List<Color>,
    angle: Float = 270f
): Modifier = this.then(
    Modifier.drawBehind {
        val angleRad = Math.toRadians(angle.toDouble()).toFloat()
        val start = Offset(0f, 0f)
        val end = Offset(
            x = size.width * kotlin.math.cos(angleRad),
            y = size.height * kotlin.math.sin(angleRad)
        )
        
        val brush = Brush.linearGradient(
            colors = colors,
            start = start,
            end = end
        )
        drawRect(brush = brush, size = size)
    }
)

fun Modifier.neonBorder(
    color: Color = Color.Cyan,
    borderWidth: Dp = 3.dp,
    glowRadius: Dp = 10.dp,
    glowIntensity: Float = 0.8f
): Modifier = this.then(
    Modifier.drawBehind {
        val borderWidthPx = borderWidth.toPx()
        val glowRadiusPx = glowRadius.toPx()
        
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = glowIntensity),
                    color.copy(alpha = glowIntensity * 0.5f),
                    Color.Transparent
                ),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.maxDimension / 2 + glowRadiusPx
            ),
            size = size
        )
        
        drawRect(
            color = color,
            style = Stroke(width = borderWidthPx)
        )
    }
)
