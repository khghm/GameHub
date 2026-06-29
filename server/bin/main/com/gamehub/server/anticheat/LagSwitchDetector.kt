package com.gamehub.server.anticheat

import com.gamehub.shared.cache.CacheProvider
import kotlin.math.abs

class LagSwitchDetector(private val cache: CacheProvider) {
    private val rttHistoryKey = "lag:rtt:%s" // userId

    suspend fun recordRTT(userId: String, rttMs: Long) {
        val key = rttHistoryKey.format(userId)
        cache.lpush(key, rttMs.toString())
        cache.ltrim(key, 0, 9) // نگهداری 10 نمونه آخر
    }

    suspend fun detectLagSwitch(userId: String): Boolean {
        val key = rttHistoryKey.format(userId)
        val rtts = cache.lrange(key, 0, -1).map { it.toLong() }
        if (rtts.size < 4) return false

        var fluctuations = 0
        for (i in 1 until rtts.size) {
            if (abs(rtts[i] - rtts[i - 1]) > 200) fluctuations++
        }
        return fluctuations >= 3
    }
}