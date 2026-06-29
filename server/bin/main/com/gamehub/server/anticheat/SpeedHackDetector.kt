package com.gamehub.server.anticheat

import com.gamehub.shared.cache.CacheProvider

class SpeedHackDetector(private val cache: CacheProvider) {
    private val histogramKey = "hist:turn_duration:%s:%s" // gameId:moveType

    suspend fun recordTurnDuration(gameId: String, moveType: String, durationMs: Long) {
        val key = histogramKey.format(gameId, moveType)
        cache.zadd(key, durationMs.toDouble(), System.currentTimeMillis().toString())
        cache.zremrangebyscore(key, 0.0, (System.currentTimeMillis() - 7 * 24 * 3600 * 1000).toDouble())
    }

    suspend fun isSuspiciouslyFast(gameId: String, moveType: String, durationMs: Long, userId: String): Boolean {
        val key = histogramKey.format(gameId, moveType)
        val allDurations = cache.zrangebyscore(key, 0.0, Double.MAX_VALUE, 10000)
        if (allDurations.size < 50) return false // نمونه کافی نیست

        val durations = allDurations.map { it.toDouble() }.sorted()
        val p5 = percentile(durations, 0.05)
        if (durationMs < p5 * 0.8) {
            return true // حرکت 20% سریع‌تر از صدک 5
        }
        return false
    }

    private fun percentile(sorted: List<Double>, p: Double): Double {
        val index = (p * (sorted.size - 1)).toInt()
        return sorted[index]
    }
}