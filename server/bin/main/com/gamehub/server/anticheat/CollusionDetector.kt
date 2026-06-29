// server/src/main/kotlin/com/gamehub/server/anticheat/CollusionDetector.kt
package com.gamehub.server.anticheat

import com.gamehub.server.repository.MatchHistoryRepository // فرضی
import com.gamehub.shared.cache.CacheProvider

class CollusionDetector(
    private val cache: CacheProvider,
    private val matchHistoryRepo: MatchHistoryRepository
) {
    suspend fun detectWinTrading(userA: String, userB: String, gameId: String): Boolean {
        // بررسی نرخ برد متقابل در ۳۰ روز اخیر
        val matches = matchHistoryRepo.getMatchesBetween(userA, userB, gameId, 30)
        if (matches.size < 5) return false

        val winsA = matches.count { it.winner == userA }
        val winsB = matches.count { it.winner == userB }
        val ratio = winsA.toDouble() / (winsA + winsB)
        // اگر یکی از بازیکنان بیش از ۸۰٪ برد داشته باشد، مشکوک
        return ratio > 0.8 || ratio < 0.2
    }

    suspend fun detectMultiDevice(userId: String, deviceFingerprints: List<String>): Boolean {
        val key = "device:multi:$userId"
        val knownDevices = cache.smembers(key)
        for (fp in deviceFingerprints) {
            if (!knownDevices.contains(fp)) {
                cache.sadd(key, fp)
            }
        }
        // اگر بیش از ۳ دستگاه فعال همزمان داشته باشد
        return cache.scard(key) > 3
    }
}