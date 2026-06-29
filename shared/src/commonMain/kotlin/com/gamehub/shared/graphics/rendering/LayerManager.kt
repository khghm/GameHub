package com.gamehub.shared.graphics.rendering

import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Layer Types for organizing rendering
 */
enum class RenderLayer(open val zIndex: Float) {
    BACKGROUND(0f),
    GAME_WORLD(50f),
    CHECKERS(100f),
    UI(200f),
    PARTICLES(250f),
    EFFECTS(300f),
    OVERLAY(400f)
}

/**
 * Layer & Z-Order Manager - organizes drawables by layer and Z-index
 */
class LayerManager {
    private val layers = mutableMapOf<RenderLayer, MutableList<Renderable>>()

    /**
     * Add a renderable object to a specific layer
     */
    fun add(layer: RenderLayer, renderable: Renderable) {
        layers.getOrPut(layer) { mutableListOf() }.add(renderable)
    }

    /**
     * Remove a renderable object
     */
    fun remove(renderable: Renderable) {
        layers.values.forEach { it.remove(renderable) }
    }

    /**
     * Clear all renderables from all layers
     */
    fun clear() {
        layers.clear()
    }

    /**
     * Render all layers in correct Z-order
     */
    fun render(drawScope: DrawScope, camera: Camera? = null) {
        // Sort layers by z-index
        val sortedLayers = layers.keys.sortedBy { it.zIndex }
        sortedLayers.forEach { layer ->
            val renderables = layers[layer] ?: return@forEach
            // Sort renderables within layer by their z-index
            renderables.sortedBy { it.zIndex }.forEach { renderable ->
                if (layer == RenderLayer.GAME_WORLD || layer == RenderLayer.CHECKERS) {
                    camera?.apply(drawScope) {
                        renderable.render(drawScope)
                    }
                } else {
                    renderable.render(drawScope)
                }
            }
        }
    }
}

/**
 * Interface for objects that can be rendered
 */
interface Renderable {
    val zIndex: Float
    fun render(drawScope: DrawScope)
}
