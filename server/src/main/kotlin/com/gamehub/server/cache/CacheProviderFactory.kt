// server/src/main/kotlin/com/gamehub/server/cache/CacheProviderFactory.kt
package com.gamehub.server.cache

import com.gamehub.shared.cache.CacheProvider

object CacheProviderFactory {
    fun create(redisUrl: String? = null): CacheProvider {
        return if (!redisUrl.isNullOrBlank()) {
            RedisCacheProvider(redisUrl)
        } else {
            MemoryCacheProvider()
        }
    }
}