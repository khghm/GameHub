package com.gamehub.shared.graphics.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

object RandomUtils {
    private val random = Random(System.currentTimeMillis())
    
    fun randomFloat(min: Float = 0f, max: Float = 1f): Float {
        return min + random.nextFloat() * (max - min)
    }
    
    fun randomInt(min: Int = 0, max: Int = 1): Int {
        return random.nextInt(min, max + 1)
    }
    
    fun randomBoolean(): Boolean {
        return random.nextBoolean()
    }
    
    fun randomColor(): Color {
        return Color(
            red = randomFloat(),
            green = randomFloat(),
            blue = randomFloat(),
            alpha = 1f
        )
    }
    
    fun randomColor(colors: List<Color>): Color {
        return colors[randomInt(0, colors.size - 1)]
    }
    
    fun randomOffset(minX: Float = 0f, maxX: Float, minY: Float = 0f, maxY: Float): Offset {
        return Offset(randomFloat(minX, maxX), randomFloat(minY, maxY))
    }
    
    fun randomAngle(): Float {
        return randomFloat(0f, MathUtils.TWO_PI)
    }
}
