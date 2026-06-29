package com.gamehub.server.anticheat

import com.gamehub.shared.cache.CacheProvider
import kotlin.math.pow
import kotlin.math.sqrt

class MacroDetector(private val cache: CacheProvider) {
    private val reactionKey = "macro:reaction:%s:%s" // userId:gameId

    suspend fun recordReactionTime(userId: String, gameId: String, reactionMs: Long) {
        val key = reactionKey.format(userId, gameId)
        cache.lpush(key, reactionMs.toString())
        cache.ltrim(key, 0, 49) // آخرین 50 واکنش
    }

    suspend fun isSuspicious(userId: String, gameId: String): Boolean {
        val key = reactionKey.format(userId, gameId)
        val times = cache.lrange(key, 0, -1).map { it.toLong() }
        if (times.size < 10) return false

        val mean = times.average()
        val variance = times.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)
        return stdDev < 10.0 // انحراف معیار کمتر از 10 میلی‌ثانیه
    }
}