// shared/src/commonMain/kotlin/com/gamehub/shared/lock/RedlockManager.kt
package com.gamehub.shared.lock

import com.gamehub.shared.cache.CacheProvider
import kotlinx.coroutines.*
import kotlin.random.Random

class RedlockManager(
    private val cache: CacheProvider,
    private val nodeCount: Int = 3,
    private val quorum: Int = (nodeCount / 2) + 1
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun <T> withLock(
        resourceId: String,
        ownerId: String,
        ttlSeconds: Long = 5,
        retryAttempts: Int = 3,
        retryDelayMs: Long = 100,
        block: suspend () -> T
    ): T {
        var lockAcquired = false
        var lockValidUntil = 0L
        val lockKey = "lock:$resourceId"

        for (attempt in 0 until retryAttempts) {
            val startTime = System.currentTimeMillis()
            val acquired = cache.setnx(lockKey, ownerId, ttlSeconds)
            if (acquired) {
                lockAcquired = true
                lockValidUntil = startTime + (ttlSeconds * 1000)
                break
            }
            if (attempt < retryAttempts - 1) {
                val jitter = Random.nextLong(0, retryDelayMs)
                delay(retryDelayMs + jitter)
            }
        }

        if (!lockAcquired) {
            throw IllegalStateException("Could not acquire lock for resource: $resourceId")
        }

        val watchdog = scope.launch {
            while (true) {
                delay((ttlSeconds * 1000) / 3)
                if (System.currentTimeMillis() > lockValidUntil - 1000) {
                    val renewed = cache.setnx(lockKey, ownerId, ttlSeconds)
                    if (renewed) {
                        lockValidUntil = System.currentTimeMillis() + (ttlSeconds * 1000)
                    } else {
                        break
                    }
                }
            }
        }

        try {
            return block()
        } finally {
            watchdog.cancel()
            val currentOwner = cache.get(lockKey)
            if (currentOwner == ownerId) {
                cache.delete(lockKey)
            }
        }
    }
}