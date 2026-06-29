package com.gamehub.shared.graphics.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

/**
 * Render Abstraction Layer - uses Compose's DrawScope/Canvas for multiplatform rendering
 * Supports:
 * - OpenGL ES (Android)
 * - Metal (iOS)
 * - Vulkan (via Compose, optional on Android 13+)
 */
class RenderContext(
    private val drawScope: DrawScope,
    val size: Size = drawScope.size
) {
    private val canvas: Canvas get() = drawScope.drawContext.canvas
    val density get() = drawScope.density
    val layoutDirection get() = drawScope.layoutDirection

    /**
     * Draw a single sprite/rect with color
     */
    fun drawRect(
        color: Color,
        topLeft: Offset = Offset.Zero,
        size: Size = this.size,
        paint: Paint? = null
    ) = drawScope.drawRect(
        color = color,
        topLeft = topLeft,
        size = size
    )

    /**
     * Draw an image (sprite)
     */
    fun drawImage(
        image: ImageBitmap,
        topLeft: Offset = Offset.Zero,
        paint: Paint? = null
    ) = drawScope.drawImage(
        image = image,
        topLeft = topLeft
    )

    /**
     * Draw a circle
     */
    fun drawCircle(
        color: Color,
        center: Offset,
        radius: Float,
        paint: Paint? = null
    ) = drawScope.drawCircle(
        color = color,
        center = center,
        radius = radius
    )

    /**
     * Save render state (for transforms/clipping)
     */
    fun save() = canvas.save()

    /**
     * Restore render state
     */
    fun restore() = canvas.restore()

    /**
     * Translate coordinate system
     */
    fun translate(dx: Float, dy: Float) = canvas.translate(dx, dy)

    /**
     * Scale coordinate system
     */
    fun scale(scaleX: Float, scaleY: Float = scaleX) = canvas.scale(scaleX, scaleY)

    /**
     * Direct access to drawIntoCanvas for complex operations
     */
    fun drawIntoCustomCanvas(block: Canvas.() -> Unit) {
        drawScope.drawIntoCanvas { it.block() }
    }
}
