package com.gamehub.server.anticheat

import com.gamehub.shared.cache.CacheProvider
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * تشخیص ربات بر اساس الگوهای رفتاری (زمان واکنش، تکرار حرکت، سرعت غیرعادی)
 */
class BotBehaviorDetector(private val cache: CacheProvider) {

    companion object {
        private const val WINDOW_SIZE = 50               // تعداد حرکات در حافظه
        private const val STD_THRESHOLD = 20.0           // انحراف معیار زیر ۲۰ میلی‌ثانیه -> ماکرو
        private const val REPEAT_THRESHOLD = 10           // ۱۰ حرکت تکراری پشت سر هم
        private const val MIN_HUMAN_MS = 100L             // کمتر از ۱۰۰ میلی‌ثانیه غیرطبیعی
        private const val FAST_RATIO_THRESHOLD = 0.7      // بیش از ۷۰٪ حرکات سریع
        private const val SUSPICION_SCORE_THRESHOLD = 3   // جمع امتیازها >= ۳ -> محکوم
    }

    data class BehaviorMetrics(
        val meanReactionMs: Double,
        val stdReactionMs: Double,
        val repeatCount: Int,
        val fastMoveRatio: Double,
        val totalMoves: Int
    )

    private fun getKey(userId: String, gameId: String) = "bot:behavior:$userId:$gameId"

    /** ثبت یک حرکت جدید */
    suspend fun recordMove(userId: String, gameId: String, reactionMs: Long?, moveType: String) {
        if (reactionMs == null) return
        val key = getKey(userId, gameId)
        // ذخیره لیست reactionMs به صورت sorted set با زمان به عنوان score
        cache.zadd(key, System.currentTimeMillis().toDouble(), "$reactionMs|$moveType")
// نگهداری فقط WINDOW_SIZE حرکت آخر
        val windowSize = WINDOW_SIZE.toLong()
        cache.zremrangebyrank(key, 0L, -windowSize - 1L)
    }

    /** محاسبه معیارهای رفتاری از روی داده‌های ذخیره شده */
    private suspend fun getMetrics(userId: String, gameId: String): BehaviorMetrics? {
        val key = getKey(userId, gameId)
        val entries = cache.zrangebyscore(key, 0.0, Double.MAX_VALUE, WINDOW_SIZE)
        if (entries.size < 10) return null  // نمونه کافی نیست

        val reactions = mutableListOf<Long>()
        var lastMove: String? = null
        var currentRepeat = 0
        var maxRepeat = 0
        var fastCount = 0

        for (entry in entries) {
            val parts = entry.split('|')
            if (parts.size < 2) continue
            val reaction = parts[0].toLongOrNull() ?: continue
            val move = parts[1]
            reactions.add(reaction)
            if (reaction < MIN_HUMAN_MS) fastCount++

            // بررسی تکرار حرکت
            if (move == lastMove) {
                currentRepeat++
                maxRepeat = maxOf(maxRepeat, currentRepeat)
            } else {
                currentRepeat = 1
                lastMove = move
            }
        }

        val mean = reactions.average()
        val variance = reactions.map { (it - mean).pow(2) }.average()
        val std = sqrt(variance)
        val fastRatio = fastCount.toDouble() / reactions.size

        return BehaviorMetrics(
            meanReactionMs = mean,
            stdReactionMs = std,
            repeatCount = maxRepeat,
            fastMoveRatio = fastRatio,
            totalMoves = reactions.size
        )
    }

    /** ارزیابی اینکه آیا کاربر ربات است */
    suspend fun isSuspiciousBot(userId: String, gameId: String): Boolean {
        val metrics = getMetrics(userId, gameId) ?: return false
        var suspicionScore = 0
        if (metrics.stdReactionMs < STD_THRESHOLD && metrics.totalMoves > 20) suspicionScore++
        if (metrics.repeatCount >= REPEAT_THRESHOLD) suspicionScore++
        if (metrics.fastMoveRatio > FAST_RATIO_THRESHOLD) suspicionScore++
        if (metrics.meanReactionMs < MIN_HUMAN_MS.toDouble() && metrics.totalMoves > 20) suspicionScore++

        return suspicionScore >= SUSPICION_SCORE_THRESHOLD
    }

    /** پاکسازی داده‌های قدیمی (اختیاری) */
    suspend fun cleanupOldData() {
        // هر ۲۴ ساعت یکبار اجرا شود: حذف داده‌های بیشتر از ۷ روز
        val cutoff = System.currentTimeMillis() - 7 * 24 * 3600_000L
        // در CacheProvider متد zremrangebyscore برای حذف بر اساس زمان وجود دارد
        // فعلاً پیاده‌سازی نمی‌کنیم
    }
}