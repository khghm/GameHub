package com.gamehub.shared.graphics.resources

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Asset priority levels
 */
enum class AssetPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Result of an asset load operation
 */
sealed class AssetResult<out T> {
    data class Success<out T>(val value: T) : AssetResult<T>()
    data class Error(val exception: Throwable) : AssetResult<Nothing>()
}

/**
 * Cached asset entry with reference counting
 */
private data class CachedAsset<T>(
    val value: T,
    var refCount: Int = 1
)

/**
 * Async Asset Loader with:
 * - Priority queue
 * - Caching
 * - Reference counting
 * - Error handling
 */
class AssetLoader(
    private val coroutineScope: CoroutineScope,
    private val maxConcurrentLoads: Int = 4
) {
    private val mutex = Mutex()
    private val pendingTasks = mutableListOf<PendingLoad<*>>()
    private val activeTasks = mutableListOf<Deferred<*>>()
    private val cache = mutableMapOf<String, CachedAsset<*>>()
    private val inProgress = mutableMapOf<String, Deferred<*>>()

    private data class PendingLoad<T>(
        val id: String,
        val priority: AssetPriority,
        val loader: suspend () -> T,
        val deferred: kotlinx.coroutines.CompletableDeferred<AssetResult<T>>
    )

    /**
     * Load an asset with given priority. Uses cache if available.
     */
    suspend fun <T> load(
        id: String,
        priority: AssetPriority = AssetPriority.NORMAL,
        loader: suspend () -> T
    ): Deferred<AssetResult<T>> {
        return mutex.withLock {
            // Check cache first
            @Suppress("UNCHECKED_CAST")
            cache[id]?.let { cached ->
                cached.refCount++
                return@withLock coroutineScope.async {
                    AssetResult.Success(cached.value as T)
                }
            }

            // Check if already in progress
            @Suppress("UNCHECKED_CAST")
            inProgress[id]?.let { existing ->
                return@withLock existing as Deferred<AssetResult<T>>
            }

            // Create new pending load
            val deferred = kotlinx.coroutines.CompletableDeferred<AssetResult<T>>()
            pendingTasks.add(PendingLoad(id, priority, loader, deferred))
            pendingTasks.sortByDescending { it.priority.ordinal }
            scheduleNext()
            deferred
        }
    }

    /**
     * Release an asset when no longer needed.
     */
    suspend fun release(id: String) = mutex.withLock {
        cache[id]?.let { cached ->
            cached.refCount--
            if (cached.refCount <= 0) {
                cache.remove(id)
                (cached.value as? AutoCloseable)?.close()
            }
        }
    }

    /**
     * Clear all cached assets
     */
    suspend fun clearCache() = mutex.withLock {
        cache.values.forEach { cached ->
            (cached.value as? AutoCloseable)?.close()
        }
        cache.clear()
    }

    /**
     * Cancel all pending loads
     */
    suspend fun cancelAll() = mutex.withLock {
        pendingTasks.clear()
        activeTasks.forEach { it.cancel() }
        activeTasks.clear()
        inProgress.clear()
    }

    private suspend fun scheduleNext() {
        mutex.withLock {
            while (activeTasks.size < maxConcurrentLoads && pendingTasks.isNotEmpty()) {
                val task = pendingTasks.removeFirst()
                launchTask(task)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun <T> launchTask(task: PendingLoad<T>) {
        val job = coroutineScope.async(Dispatchers.Default) {
            try {
                val result = task.loader()
                mutex.withLock {
                    cache[task.id] = CachedAsset(result)
                    inProgress.remove(task.id)
                }
                AssetResult.Success(result)
            } catch (e: Exception) {
                mutex.withLock {
                    inProgress.remove(task.id)
                }
                AssetResult.Error(e)
            }
        }

        // Add to activeTasks and inProgress while holding the lock
        mutex.withLock {
            inProgress[task.id] = job
            activeTasks.add(job)
        }

        job.invokeOnCompletion { cause ->
            val result = if (cause != null) {
                AssetResult.Error(cause)
            } else {
                job.getCompleted()
            }
            task.deferred.complete(result)

            coroutineScope.async {
                mutex.withLock {
                    activeTasks.remove(job)
                }
                scheduleNext()
            }
        }
    }
}
