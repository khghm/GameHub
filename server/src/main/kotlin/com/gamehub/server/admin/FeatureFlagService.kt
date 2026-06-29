package com.gamehub.server.admin

import com.gamehub.shared.cache.CacheProvider

class FeatureFlagService(private val cache: CacheProvider) {

    companion object {
        private const val PREFIX = "feature_flag:"
        private const val DEFAULT_TTL_SECONDS = 3600L
    }

    suspend fun setFlag(name: String, enabled: Boolean, ttlSeconds: Long = DEFAULT_TTL_SECONDS) {
        cache.set("$PREFIX$name", if (enabled) "true" else "false", ttlSeconds)
    }

    suspend fun isEnabled(name: String): Boolean {
        val value = cache.get("$PREFIX$name")
        return when (value) {
            "true" -> true
            "false" -> false
            else -> false
        }
    }

    suspend fun deleteFlag(name: String) {
        cache.delete("$PREFIX$name")
    }
}