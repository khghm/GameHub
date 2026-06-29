package com.gamehub.server.security

import com.gamehub.shared.cache.CacheProvider
import kotlinx.coroutines.runBlocking

class TokenBlacklist(private val cache: CacheProvider) {
    fun blacklist(jti: String, ttlSeconds: Long) = runBlocking {
        cache.set("blacklist:$jti", "1", ttlSeconds)
    }

    fun isBlacklisted(jti: String): Boolean = runBlocking {
        cache.exists("blacklist:$jti")
    }
}