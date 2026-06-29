package com.gamehub.server.cache

import com.gamehub.shared.cache.CacheProvider
import kotlinx.coroutines.runBlocking

object SessionCache {
    private var provider: CacheProvider = MemoryCacheProvider()

    fun init(cacheProvider: CacheProvider) {
        provider = cacheProvider
    }

    fun set(userId: String, token: String, ttlSeconds: Long = 3600) = runBlocking {
        provider.set("session:$userId", token, ttlSeconds)
    }

    fun getToken(userId: String): String? = runBlocking {
        provider.get("session:$userId")
    }

    fun remove(userId: String) = runBlocking {
        provider.delete("session:$userId")
    }
}