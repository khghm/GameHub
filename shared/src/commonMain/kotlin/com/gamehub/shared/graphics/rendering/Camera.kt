package com.gamehub.shared.graphics.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * 2D Camera System - handles zoom, pan, and following targets
 */
class Camera(
    var position: Offset = Offset.Zero,
    var zoom: Float = 1f,
    var minZoom: Float = 0.1f,
    var maxZoom: Float = 10f
) {
    /**
     * Pan camera by delta
     */
    fun pan(delta: Offset) {
        position += delta
    }

    /**
     * Zoom camera by factor around focal point
     */
    fun zoomBy(factor: Float, focalPoint: Offset = Offset.Zero) {
        val newZoom = (zoom * factor).coerceIn(minZoom, maxZoom)
        // Adjust position to keep focal point in same place
        if (newZoom != zoom) {
            val zoomDelta = newZoom / zoom
            position = focalPoint + (position - focalPoint) * zoomDelta
            zoom = newZoom
        }
    }

    /**
     * Set zoom level directly
     */
    fun setZoom(level: Float, focalPoint: Offset = Offset.Zero) {
        zoomBy(level / zoom, focalPoint)
    }

    /**
     * Move camera to follow a target position
     */
    fun follow(target: Offset, smoothing: Float = 0.1f) {
        position += (target - position) * smoothing
    }

    /**
     * Convert a screen point to world point
     */
    fun screenToWorld(screen: Offset, viewportSize: Size): Offset {
        return (screen - Offset(viewportSize.width / 2f, viewportSize.height / 2f)) / zoom + position
    }

    /**
     * Convert a world point to screen point
     */
    fun worldToScreen(world: Offset, viewportSize: Size): Offset {
        return (world - position) * zoom + Offset(viewportSize.width / 2f, viewportSize.height / 2f)
    }

    /**
     * Apply camera transform to DrawScope
     */
    fun apply(drawScope: DrawScope, block: DrawScope.() -> Unit) {
        drawScope.withTransform({
            translate(left = drawScope.size.width / 2f, top = drawScope.size.height / 2f)
            scale(scaleX = zoom, scaleY = zoom)
            translate(left = -position.x, top = -position.y)
        }, block)
    }
}

/**
 * Viewport - manages viewport size and world bounds
 */
class Viewport(
    var size: Size = Size.Zero,
    var worldBounds: WorldBounds? = null
) {
    data class WorldBounds(
        val minX: Float, val minY: Float,
        val maxX: Float, val maxY: Float
    ) {
        val width: Float get() = maxX - minX
        val height: Float get() = maxY - minY
    }
}
