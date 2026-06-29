package com.gamehub.server.anticheat

import com.gamehub.server.behavior.BehaviorService
import com.gamehub.shared.cache.CacheProvider

class ShadowPoolManager(
    private val cache: CacheProvider,
    private val behaviorService: BehaviorService
) {
    private val shadowPoolKey = "shadow_pool:users"

    suspend fun addToShadowPool(userId: String, reason: String) {
        cache.sadd(shadowPoolKey, userId)
        println("🚫 User $userId added to shadow pool: $reason")
    }

    suspend fun removeFromShadowPool(userId: String) {
        cache.srem(shadowPoolKey, userId)
    }

    suspend fun isInShadowPool(userId: String): Boolean {
        return cache.sismember(shadowPoolKey, userId)
    }

    suspend fun getShadowPoolUsers(): Set<String> {
        return cache.smembers(shadowPoolKey)
    }
}