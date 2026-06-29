package com.gamehub.shared.graphics.vfx

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Transition types
 */
enum class TransitionType {
    Fade,
    CurtainLeft,
    CurtainRight,
    Wave,
    Pixelate
}

/**
 * Transition Composable
 */
@Composable
fun SceneTransition(
    transitionType: TransitionType = TransitionType.Fade,
    transitionProgress: Float = 0f,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        content()
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (transitionType) {
                TransitionType.Fade -> {
                    drawRect(
                        color = Color.Black.copy(alpha = transitionProgress),
                        size = size
                    )
                }
                TransitionType.CurtainLeft -> {
                    val w = size.width * transitionProgress
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(0f, 0f),
                        size = Size(w, size.height)
                    )
                }
                TransitionType.CurtainRight -> {
                    val w = size.width * transitionProgress
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(size.width - w, 0f),
                        size = Size(w, size.height)
                    )
                }
                TransitionType.Wave -> {
                    val numWaves = 5
                    val amplitude = 20f
                    val wavelength = size.width / numWaves
                    val phase = 2 * PI * transitionProgress
                    for (y in 0 until size.height.toInt() step 2) {
                        val offset = sin(phase + y / wavelength * 2 * PI).toFloat() * amplitude
                        drawLine(
                            color = Color.Black,
                            start = Offset(-10f, y.toFloat()),
                            end = Offset(size.width * transitionProgress + offset, y.toFloat()),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
                TransitionType.Pixelate -> {
                    val blockSize = (size.width / 20) * transitionProgress + 2.dp.toPx()
                    for (x in 0 until size.width.toInt() step blockSize.toInt()) {
                        for (y in 0 until size.height.toInt() step blockSize.toInt()) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(x.toFloat(), y.toFloat()),
                                size = Size(blockSize, blockSize),
                                alpha = transitionProgress
                            )
                        }
                    }
                }
            }
        }
    }
}