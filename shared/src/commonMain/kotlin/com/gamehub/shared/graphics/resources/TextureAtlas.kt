package com.gamehub.shared.graphics.resources

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Texture Atlas - groups sprites into single image
 */
class TextureAtlas(
    val atlasImage: ImageBitmap,
    val entries: Map<String, AtlasEntry>
) {
    /**
     * Get entry for a sprite ID
     */
    operator fun get(id: String): AtlasEntry? = entries[id]
}

/**
 * Single sprite entry in atlas
 */
data class AtlasEntry(
    val uvRect: Rect, // UV coordinates (0..1)
    val pixelRect: Rect, // Pixel coordinates in atlas
    val originalSize: Size
)

/**
 * Texture Atlas Generator - packs sprites with padding
 */
class TextureAtlasGenerator(
    private val padding: Int = 2, // 2px padding to prevent bleeding
    private val maxAtlasSize: Int = 2048
) {
    /**
     * Generate texture atlas from a list of sprites
     */
    fun generate(
        sprites: List<Pair<String, ImageBitmap>>,
        density: Density
    ): TextureAtlas {
        // Sort sprites from tallest to shortest (better packing)
        val sortedSprites = sprites.sortedByDescending { it.second.height }

        // Use simple row packing
        val entries = mutableMapOf<String, AtlasEntry>()
        var currentX = padding
        var currentY = padding
        var rowHeight = 0

        // Find required atlas size
        var totalWidth = padding
        var totalHeight = padding

        sortedSprites.forEach { (id, bitmap) ->
            val widthWithPadding = bitmap.width + 2 * padding
            val heightWithPadding = bitmap.height + 2 * padding

            // Check if fits in current row
            if (currentX + widthWithPadding > maxAtlasSize - padding) {
                // New row
                currentY += rowHeight + padding
                currentX = padding
                rowHeight = 0
            }

            // Check height
            if (currentY + heightWithPadding > maxAtlasSize - padding) {
                throw IllegalStateException("Atlas size too small! Try increasing maxAtlasSize.")
            }

            // Record entry
            val pixelRect = Rect(
                left = currentX.toFloat(),
                top = currentY.toFloat(),
                right = currentX + bitmap.width.toFloat(),
                bottom = currentY + bitmap.height.toFloat()
            )
            entries[id] = AtlasEntry(
                pixelRect = pixelRect,
                uvRect = Rect(
                    left = pixelRect.left / maxAtlasSize,
                    top = pixelRect.top / maxAtlasSize,
                    right = pixelRect.right / maxAtlasSize,
                    bottom = pixelRect.bottom / maxAtlasSize
                ),
                originalSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
            )

            // Update position
            currentX += widthWithPadding
            rowHeight = maxOf(rowHeight, heightWithPadding)
            totalWidth = maxOf(totalWidth, currentX)
            totalHeight = maxOf(totalHeight, currentY + heightWithPadding)
        }

        // Round up to power of two for better performance
        val finalWidth = nextPowerOfTwo(totalWidth.toInt())
        val finalHeight = nextPowerOfTwo(totalHeight.toInt())

        // Draw atlas
        val atlas = ImageBitmap(finalWidth, finalHeight)
        CanvasDrawScope().draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = androidx.compose.ui.graphics.Canvas(atlas),
            size = Size(finalWidth.toFloat(), finalHeight.toFloat())
        ) {
            sprites.forEach { (id, bitmap) ->
                val entry = entries[id]!!
                drawImage(
                    image = bitmap,
                    topLeft = Offset(entry.pixelRect.left, entry.pixelRect.top)
                )
            }
        }

        return TextureAtlas(atlas, entries)
    }

    private fun nextPowerOfTwo(n: Int): Int {
        var num = n
        if (num and num - 1 == 0) return num
        num--
        num = num or (num shr 1)
        num = num or (num shr 2)
        num = num or (num shr 4)
        num = num or (num shr 8)
        num = num or (num shr 16)
        return num + 1
    }
}
