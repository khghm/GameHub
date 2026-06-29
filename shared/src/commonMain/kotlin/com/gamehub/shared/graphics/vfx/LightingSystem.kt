package com.gamehub.shared.graphics.vfx

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import kotlin.math.*

/**
 * 2D Light Base Class
 */
sealed class Light2D {
    abstract val position: Offset
    abstract val color: Color
    abstract val intensity: Float
}

/**
 * Point Light
 */
data class PointLight(
    override val position: Offset = Offset.Zero,
    override val color: Color = Color.White,
    override val intensity: Float = 1f,
    val radius: Float = 300f
) : Light2D()

/**
 * Directional Light
 */
data class DirectionalLight(
    val direction: Offset = Offset(1f, -1f),
    override val color: Color = Color.White,
    override val intensity: Float = 0.5f
) : Light2D() {
    override val position: Offset = Offset.Zero
}

/**
 * 2D Lighting System
 */
class LightingSystem {
    private val lights = mutableStateListOf<Light2D>()
    var ambientColor = Color(0xFF222222)

    fun addLight(light: Light2D) = lights.add(light)
    fun removeLight(light: Light2D) = lights.remove(light)
    fun clearLights() = lights.clear()

    fun render(drawScope: DrawScope) {
        // Render ambient
        drawScope.drawRect(color = ambientColor)
        // Render lights additively
        lights.filterIsInstance<PointLight>().forEach { light ->
            drawScope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        light.color.copy(alpha = light.intensity),
                        Color.Transparent
                    ),
                    center = light.position,
                    radius = light.radius
                ),
                radius = light.radius,
                center = light.position,
                blendMode = BlendMode.Plus
            )
        }
    }
}

/**
 * Composable for 2D Lighting
 */
@Composable
fun Lighting(
    system: LightingSystem = remember { LightingSystem() },
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
        Canvas(modifier = Modifier.matchParentSize()) {
            system.render(this)
        }
    }
}
