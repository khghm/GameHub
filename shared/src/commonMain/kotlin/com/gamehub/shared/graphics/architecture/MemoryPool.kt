package com.gamehub.shared.graphics.architecture

/**
 * Poolable interface - objects that can be reset
 */
interface Poolable {
    fun reset()
}

/**
 * Generic Memory Pool to reduce GC pressure
 */
class MemoryPool<T : Poolable>(
    private val factory: () -> T,
    private val initialSize: Int = 10,
    private val maxSize: Int = 1000
) {
    private val pool = mutableListOf<T>()

    init {
        repeat(initialSize) { pool.add(factory()) }
    }

    /**
     * Acquire an object from the pool (or create a new one)
     */
    fun acquire(): T {
        return if (pool.isNotEmpty()) {
            pool.removeAt(pool.size - 1)
        } else {
            factory()
        }
    }

    /**
     * Return an object to the pool
     */
    fun release(item: T) {
        if (pool.size < maxSize) {
            item.reset()
            pool.add(item)
        }
    }

    /**
     * Clear the entire pool
     */
    fun clear() {
        pool.clear()
    }

    /**
     * Current pool size
     */
    val size: Int get() = pool.size
}

/**
 * Helper function to create pools
 */
inline fun <reified T : Poolable> createMemoryPool(
    noinline factory: () -> T,
    initialSize: Int = 10,
    maxSize: Int = 1000
): MemoryPool<T> = MemoryPool(factory, initialSize, maxSize)
