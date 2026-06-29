package com.gamehub.server.security

import com.gamehub.shared.cache.CacheProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * محدودکننده نرخ درخواست با الگوریتم Token Bucket
 * هر کاربر/IP سهم مشخصی توکن در هر بازه زمانی دارد.
 */
class RateLimiter(
    private val cache: CacheProvider,
    private val capacity: Int = 60,        // حداکثر توکن‌های قابل ذخیره
    private val refillRate: Int = 30,      // توکن در دقیقه
    private val windowSeconds: Long = 60   // بازه زمانی بر حسب ثانیه
) {

    companion object {
        private const val PREFIX = "rate:"
    }

    private val locks = mutableMapOf<String, Mutex>()

    /**
     * بررسی اینکه آیا درخواست مجاز است (در صورت مجاز بودن یک توکن مصرف می‌شود)
     * @param key کلید منحصر‌به‌فرد (مثلاً "user:$userId" یا "ip:$ip")
     * @return true اگر مجاز باشد، false اگر محدودیت اعمال شود
     */
    suspend fun isAllowed(key: String): Boolean {
        val lock = locks.getOrPut(key) { Mutex() }
        return lock.withLock {
            val now = System.currentTimeMillis()
            val tokenKey = "$PREFIX$key"
            val data = cache.hgetAll(tokenKey)

            var tokens = data["tokens"]?.toDoubleOrNull() ?: capacity.toDouble()
            val lastRefill = data["lastRefill"]?.toLongOrNull() ?: now

            // محاسبه توکن‌های جدید بر اساس زمان گذشته
            val elapsedSeconds = (now - lastRefill) / 1000.0
            val newTokens = elapsedSeconds * (refillRate.toDouble() / windowSeconds)
            tokens = (tokens + newTokens).coerceAtMost(capacity.toDouble())

            if (tokens >= 1.0) {
                // مصرف یک توکن
                tokens--
                cache.hset(tokenKey, "tokens", tokens.toString())
                cache.hset(tokenKey, "lastRefill", now.toString())
                cache.expire(tokenKey, windowSeconds * 2) // TTL دو برابر بازه
                true
            } else {
                false
            }
        }
    }

    /**
     * گرفتن تعداد توکن‌های باقی‌مانده برای یک کلید (اختیاری، برای نمایش در کلاینت)
     */
    suspend fun getRemainingTokens(key: String): Int {
        val data = cache.hgetAll("$PREFIX$key")
        val tokens = data["tokens"]?.toDoubleOrNull() ?: capacity.toDouble()
        return tokens.toInt().coerceAtLeast(0)
    }
}