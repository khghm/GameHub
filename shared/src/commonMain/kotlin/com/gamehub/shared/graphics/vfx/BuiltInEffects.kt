package com.gamehub.shared.graphics.vfx

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Blur effect (uses native compose blur)
 */
fun Modifier.blurEffect(radius: Dp = 4.dp): Modifier = this.then(
    Modifier.blur(radius = radius)
)

/**
 * Glow effect
 */
fun Modifier.glowEffect(
    color: Color = Color(0xFFFFA500),
    radius: Dp = 16.dp
): Modifier = this.then(
    Modifier.graphicsLayer {
        shadowElevation = radius.value
    }
)

/**
 * Dissolve effect
 */
fun Modifier.dissolveEffect(
    progress: Float = 0f, // 0 to 1
    seed: Int = 1337
): Modifier = this.then(
    Modifier.drawWithContent {
        val random = Random(seed)
        val threshold = 1 - progress

        drawContent()

        drawRect(
            color = Color.Transparent,
            blendMode = androidx.compose.ui.graphics.BlendMode.Clear
        )

        val step = 2.dp.toPx()
        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                val r = random.nextFloat()
                if (r < threshold) {
                    val alpha = 1f
                    drawRect(
                        color = Color.Black.copy(alpha = alpha),
                        topLeft = Offset(x, y),
                        size = Size(step, step)
                    )
                }
                y += step
            }
            x += step
        }
    }
)

/**
 * Drop shadow effect
 */
fun Modifier.dropShadowEffect(
    color: Color = Color(0x40000000),
    offset: Offset = Offset(4f, 4f),
    radius: Dp = 4.dp
): Modifier = this.then(
    Modifier.drawWithContent {
        translate(offset.x, offset.y) {
            this@drawWithContent.drawContent()
        }
        drawContent()
    }.blur(radius = radius)
)