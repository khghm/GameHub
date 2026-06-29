package com.gamehub.shared.graphics.vfx

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import kotlin.math.exp

/**
 * Post-Processing Effect Interface
 */
interface PostProcessEffect {
    val enabled: Boolean
    fun apply(drawScope: DrawScope, inputImage: ImageBitmap)
}

/**
 * Blur Effect
 */
data class BlurEffect(
    override val enabled: Boolean = true,
    val radius: Float = 8f
) : PostProcessEffect {
    override fun apply(drawScope: DrawScope, inputImage: ImageBitmap) {
        drawScope.drawImage(image = inputImage)
    }
}

/**
 * Glow Effect
 */
data class GlowEffect(
    override val enabled: Boolean = true,
    val color: Color = Color.Cyan,
    val intensity: Float = 0.5f,
    val radius: Float = 20f
) : PostProcessEffect {
    override fun apply(drawScope: DrawScope, inputImage: ImageBitmap) {
        drawScope.drawImage(image = inputImage)
    }
}

/**
 * Vignette Effect
 */
data class VignetteEffect(
    override val enabled: Boolean = true,
    val color: Color = Color.Black,
    val intensity: Float = 0.4f
) : PostProcessEffect {
    override fun apply(drawScope: DrawScope, inputImage: ImageBitmap) {
        drawScope.drawImage(image = inputImage)
        // Draw vignette overlay
        drawScope.drawCircle(
            color = color.copy(alpha = intensity),
            radius = drawScope.size.maxDimension / 1.5f,
            center = drawScope.center
        )
    }
}

/**
 * Color Grading Effect
 */
data class ColorGradingEffect(
    override val enabled: Boolean = true,
    val brightness: Float = 1f,
    val contrast: Float = 1f,
    val saturation: Float = 1f
) : PostProcessEffect {
    override fun apply(drawScope: DrawScope, inputImage: ImageBitmap) {
        drawScope.drawImage(image = inputImage)
    }
}

/**
 * Post-Processing Stack - applies effects in order
 */
class PostProcessingStack(
    val effects: MutableList<PostProcessEffect> = mutableListOf(
        BlurEffect(),
        GlowEffect(),
        ColorGradingEffect(),
        VignetteEffect()
    )
) {
    inline fun <reified T : PostProcessEffect> getEffect(): T? =
        effects.filterIsInstance<T>().firstOrNull()

    fun <T : PostProcessEffect> setEffect(effect: T) {
        val index = effects.indexOfFirst { it::class == effect::class }
        if (index >= 0) {
            effects[index] = effect
        } else {
            effects.add(effect)
        }
    }

    fun apply(drawScope: DrawScope, inputImage: ImageBitmap) {
        effects.filter { it.enabled }.forEach {
            it.apply(drawScope, inputImage)
        }
    }
}

/**
 * Composable for Post-Processing Stack
 */
@Composable
fun PostProcess(
    stack: PostProcessingStack = remember { PostProcessingStack() },
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}
