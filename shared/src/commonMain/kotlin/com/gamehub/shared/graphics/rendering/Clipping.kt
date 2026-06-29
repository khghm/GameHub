package com.gamehub.shared.graphics.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect

/**
 * Clipping & Scissor Test helpers
 */

/**
 * Clip rendering to a rectangular region
 */
fun DrawScope.withScissorRect(
    rect: Rect,
    block: DrawScope.() -> Unit
) {
    clipRect(left = rect.left, top = rect.top, right = rect.right, bottom = rect.bottom) {
        block()
    }
}

/**
 * Clip rendering to a rectangular region (using coordinates)
 */
fun DrawScope.withScissorRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    block: DrawScope.() -> Unit
) {
    clipRect(left = left, top = top, right = right, bottom = bottom) {
        block()
    }
}

/**
 * Clip rendering to a custom path
 */
fun DrawScope.withScissorPath(
    path: Path,
    block: DrawScope.() -> Unit
) {
    clipPath(path = path) {
        block()
    }
}

/**
 * Create a rounded rectangle clip path
 */
fun createRoundedRectClip(
    topLeft: Offset,
    size: Size,
    cornerRadius: Float
): Path {
    return Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                rect = Rect(topLeft, size),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
            )
        )
    }
}
