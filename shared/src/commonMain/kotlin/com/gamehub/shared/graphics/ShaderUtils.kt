package com.gamehub.shared.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode

object ShaderUtils {
    fun linearGradient(
        colors: List<Color>,
        start: Offset = Offset.Zero,
        end: Offset = Offset(100f, 100f),
        tileMode: TileMode = TileMode.Clamp
    ): Brush = Brush.linearGradient(
        colors = colors,
        start = start,
        end = end,
        tileMode = tileMode
    )

    fun radialGradient(
        colors: List<Color>,
        center: Offset = Offset(50f, 50f),
        radius: Float = 50f,
        tileMode: TileMode = TileMode.Clamp
    ): Brush = Brush.radialGradient(
        colors = colors,
        center = center,
        radius = radius,
        tileMode = tileMode
    )

    fun sweepGradient(
        colors: List<Color>,
        center: Offset = Offset(50f, 50f)
    ): Brush = Brush.sweepGradient(
        colors = colors,
        center = center
    )

    fun rainbowGradient(): Brush = linearGradient(
        colors = listOf(
            Color.Red,
            Color(0xFFFF7F00), // Orange
            Color.Yellow,
            Color.Green,
            Color.Blue,
            Color(0xFF4B0082), // Indigo
            Color(0xFF9400D3) // Violet
        )
    )

    fun fireGradient(): Brush = linearGradient(
        colors = listOf(
            Color.Yellow,
            Color(0xFFFFA500), // Orange
            Color.Red
        )
    )

    fun neonGradient(): Brush = linearGradient(
        colors = listOf(
            Color.Cyan,
            Color.Magenta
        )
    )
}
