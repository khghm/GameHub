package com.gamehub.server.cache

import com.gamehub.shared.cache.CacheProvider

object PresenceCache {
    private var provider: CacheProvider = MemoryCacheProvider()
    private const val ONLINE_HUB_SET = "presence:online:hub"
    private const val IN_GAME_SET = "presence:online:game"

    fun init(cacheProvider: CacheProvider) {
        provider = cacheProvider
    }

    suspend fun setOnlineHub(userId: String) {
        provider.sadd(ONLINE_HUB_SET, userId)
    }

    suspend fun removeOnlineHub(userId: String) {
        provider.srem(ONLINE_HUB_SET, userId)
    }

    suspend fun getOnlineHubCount(): Int = provider.scard(ONLINE_HUB_SET).toInt()
    
    suspend fun isUserOnline(userId: String): Boolean {
        return provider.sismember(ONLINE_HUB_SET, userId)
    }

    suspend fun setInGame(userId: String) {
        provider.sadd(IN_GAME_SET, userId)
    }

    suspend fun removeInGame(userId: String) {
        provider.srem(IN_GAME_SET, userId)
    }

    suspend fun getInGameCount(): Int = provider.scard(IN_GAME_SET).toInt()
}