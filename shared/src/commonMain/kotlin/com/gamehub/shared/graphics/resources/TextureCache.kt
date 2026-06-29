package com.gamehub.shared.graphics.resources

import androidx.compose.ui.graphics.ImageBitmap
import java.util.LinkedHashMap

/**
 * LRU (Least Recently Used) Texture Cache with memory limit
 */
class TextureCache(
    private val maxMemoryBytes: Long = 64 * 1024 * 1024L // 64MB default
) {
    private val cache = object : LinkedHashMap<String, CacheEntry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean {
            return currentMemoryBytes > maxMemoryBytes
        }
    }

    private var currentMemoryBytes = 0L
    private val lock = Any()

    /**
     * Get texture from cache (returns null if not present)
     */
    operator fun get(key: String): ImageBitmap? = synchronized(lock) {
        cache[key]?.bitmap
    }

    /**
     * Put texture into cache
     */
    fun put(key: String, bitmap: ImageBitmap) = synchronized(lock) {
        // Estimate bitmap size (32-bit ARGB = 4 bytes per pixel)
        val size = bitmap.width.toLong() * bitmap.height.toLong() * 4L

        // If new entry alone is too big, don't cache it
        if (size > maxMemoryBytes) return

        // Remove existing entry if present
        cache.remove(key)?.let {
            currentMemoryBytes -= it.sizeBytes
        }

        // Add new entry
        cache[key] = CacheEntry(bitmap, size)
        currentMemoryBytes += size

        // Trim if over limit
        trimToSize()
    }

    /**
     * Remove texture from cache
     */
    fun remove(key: String) = synchronized(lock) {
        cache.remove(key)?.let {
            currentMemoryBytes -= it.sizeBytes
        }
    }

    /**
     * Clear entire cache
     */
    fun clear() = synchronized(lock) {
        cache.clear()
        currentMemoryBytes = 0L
    }

    /**
     * Get current cache size in bytes
     */
    val sizeBytes: Long get() = synchronized(lock) { currentMemoryBytes }

    /**
     * Get number of cached textures
     */
    val count: Int get() = synchronized(lock) { cache.size }

    private fun trimToSize() {
        while (currentMemoryBytes > maxMemoryBytes && cache.isNotEmpty()) {
            val (key, entry) = cache.entries.first()
            cache.remove(key)
            currentMemoryBytes -= entry.sizeBytes
        }
    }

    private data class CacheEntry(
        val bitmap: ImageBitmap,
        val sizeBytes: Long
    )
}
