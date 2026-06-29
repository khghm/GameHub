package com.gamehub.shared.graphics.resources

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Font Manager - manages fonts and bitmap/SDF rendering
 */
class FontManager {
    private val fontFamilies = mutableMapOf<String, FontFamily>()
    private val fontCache = mutableMapOf<FontKey, CachedFont>()

    /**
     * Register a font family
     */
    fun registerFamily(name: String, family: FontFamily) {
        fontFamilies[name] = family
    }

    /**
     * Get a font family
     */
    fun getFamily(name: String): FontFamily? = fontFamilies[name]

    /**
     * Get cached font bitmap (for bitmap fonts)
     */
    fun getBitmapFont(
        familyName: String,
        size: Int,
        weight: FontWeight = FontWeight.Normal,
        style: FontStyle = FontStyle.Normal
    ): ImageBitmap? {
        val key = FontKey(familyName, size, weight, style, isSdf = false)
        return fontCache[key]?.bitmap
    }

    /**
     * Get cached SDF (Signed Distance Field) font texture
     */
    fun getSdfFont(
        familyName: String,
        size: Int,
        weight: FontWeight = FontWeight.Normal,
        style: FontStyle = FontStyle.Normal
    ): ImageBitmap? {
        val key = FontKey(familyName, size, weight, style, isSdf = true)
        return fontCache[key]?.bitmap
    }

    /**
     * Cache a font bitmap
     */
    fun cacheBitmapFont(
        familyName: String,
        size: Int,
        weight: FontWeight,
        style: FontStyle,
        bitmap: ImageBitmap
    ) {
        val key = FontKey(familyName, size, weight, style, isSdf = false)
        fontCache[key] = CachedFont(bitmap, false)
    }

    /**
     * Cache an SDF font texture
     */
    fun cacheSdfFont(
        familyName: String,
        size: Int,
        weight: FontWeight,
        style: FontStyle,
        bitmap: ImageBitmap
    ) {
        val key = FontKey(familyName, size, weight, style, isSdf = true)
        fontCache[key] = CachedFont(bitmap, true)
    }

    /**
     * Clear font cache
     */
    fun clearCache() {
        fontCache.clear()
    }

    private data class FontKey(
        val familyName: String,
        val size: Int,
        val weight: FontWeight,
        val style: FontStyle,
        val isSdf: Boolean
    )

    private data class CachedFont(
        val bitmap: ImageBitmap,
        val isSdf: Boolean
    )
}
