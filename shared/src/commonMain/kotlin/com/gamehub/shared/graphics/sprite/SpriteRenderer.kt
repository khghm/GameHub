package com.gamehub.shared.graphics.sprite

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntSize

data class Sprite(
    val image: ImageBitmap? = null,
    val color: Color = Color.White,
    val size: Size = Size(50f, 50f),
    val position: Offset = Offset.Zero,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val alpha: Float = 1f
)

@Composable
fun SpriteRenderer(
    sprites: List<Sprite>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        sprites.forEach { sprite ->
            drawSprite(sprite)
        }
    }
}

fun DrawScope.drawSprite(sprite: Sprite) {
    translate(left = sprite.position.x, top = sprite.position.y) {
        rotate(sprite.rotation) {
            scale(sprite.scale, sprite.scale) {
                sprite.image?.let { img ->
                    drawImage(
                        image = img,
                        dstSize = IntSize(
                            (sprite.size.width * sprite.scale).toInt(),
                            (sprite.size.height * sprite.scale).toInt()
                        ),
                        alpha = sprite.alpha
                    )
                } ?: drawRect(
                    color = sprite.color.copy(alpha = sprite.alpha),
                    size = sprite.size
                )
            }
        }
    }
}
