package com.gamehub.shared.graphics.utils

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

object ColorUtils {
    fun lerp(a: Color, b: Color, t: Float): Color {
        return Color(
            red = lerpFloat(a.red, b.red, t),
            green = lerpFloat(a.green, b.green, t),
            blue = lerpFloat(a.blue, b.blue, t),
            alpha = lerpFloat(a.alpha, b.alpha, t)
        )
    }
    
    private fun lerpFloat(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t.coerceIn(0f, 1f)
    }
    
    fun brighten(color: Color, factor: Float = 0.2f): Color {
        return Color(
            red = (color.red + factor).coerceAtMost(1f),
            green = (color.green + factor).coerceAtMost(1f),
            blue = (color.blue + factor).coerceAtMost(1f),
            alpha = color.alpha
        )
    }
    
    fun darken(color: Color, factor: Float = 0.2f): Color {
        return Color(
            red = (color.red - factor).coerceAtLeast(0f),
            green = (color.green - factor).coerceAtLeast(0f),
            blue = (color.blue - factor).coerceAtLeast(0f),
            alpha = color.alpha
        )
    }
    
    fun randomColor(): Color {
        return Color(
            red = kotlin.random.Random.nextFloat(),
            green = kotlin.random.Random.nextFloat(),
            blue = kotlin.random.Random.nextFloat(),
            alpha = 1f
        )
    }
    
    fun hexToColor(hex: String): Color {
        val colorStr = hex.removePrefix("#")
        val colorLong = colorStr.toLong(16)
        val a = if (colorStr.length == 8) ((colorLong shr 24) and 0xFF) / 255f else 1f
        val r = if (colorStr.length == 8) ((colorLong shr 16) and 0xFF) / 255f else ((colorLong shr 16) and 0xFF) / 255f
        val g = ((colorLong shr 8) and 0xFF) / 255f
        val b = (colorLong and 0xFF) / 255f
        return Color(r, g, b, a)
    }
}
