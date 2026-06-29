package com.gamehub.server.security

import com.gamehub.shared.cache.CacheProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * فیلتر ضد بازپخش با استفاده از nonce و timestamp
 */
class AntiReplayFilter(private val cache: CacheProvider) {

    companion object {
        private const val NONCE_TTL_SECONDS = 300L   // 5 دقیقه
        private const val MAX_DRIFT_MS = 30000L      // 30 ثانیه اختلاف زمانی مجاز
    }

    private val locks = mutableMapOf<String, Mutex>()

    /**
     * بررسی و ثبت nonce. در صورت معتبر نبودن (تکراری یا زمان نامعتبر) false برمی‌گرداند.
     * @param nonce شناسه یکتای پیام
     * @param timestamp زمان ارسال از سمت کلاینت
     * @param userId شناسه کاربر (برای جلوگیری از تداخل nonce بین کاربران مختلف)
     */
    suspend fun checkAndRecord(nonce: String, timestamp: Long, userId: String): Boolean {
        // بررسی زمان
        val now = System.currentTimeMillis()
        if (kotlin.math.abs(now - timestamp) > MAX_DRIFT_MS) {
            println("⏰ Replay rejected: timestamp drift too large (${now - timestamp}ms) for user $userId")
            return false
        }

        // کلید یکتا برای nonce: ترکیب userId و nonce
        val key = "replay:${userId}:$nonce"
        val lock = locks.getOrPut(key) { Mutex() }
        return lock.withLock {
            if (cache.exists(key)) {
                println("🔄 Replay detected: nonce $nonce already used for user $userId")
                return@withLock false
            }
            cache.set(key, "1", NONCE_TTL_SECONDS)
            true
        }
    }
}