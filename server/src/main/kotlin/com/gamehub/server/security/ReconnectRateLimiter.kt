package com.gamehub.server.security

import com.gamehub.shared.cache.CacheProvider

/**
 * محدودکننده نرخ درخواست‌های reconnect
 *
 * از الگوی Token Bucket استفاده می‌کند.
 * اگر این کلاس نباشد، مهاجم می‌تواند با ارسال درخواست‌های reconnect مکرر،
 * باعث overload سرور شود.
 */
class ReconnectRateLimiter(
    private val cache: CacheProvider
) {
    companion object {
        // حداکثر درخواست reconnect در هر دقیقه به ازای هر کاربر
        private const val MAX_REQUESTS_PER_MINUTE = 10
        // حداکثر درخواست reconnect در هر ساعت به ازای هر دستگاه
        private const val MAX_REQUESTS_PER_HOUR_PER_DEVICE = 30
        // مدت مسدودیت موقت در صورت نقض حد (ثانیه)
        private const val TEMP_BLOCK_DURATION_SECONDS = 300L // 5 دقیقه
    }

    /**
     * بررسی آیا درخواست reconnect مجاز است؟
     * @param userId شناسه کاربر
     * @param deviceIdHash هش دستگاه (برای محدودیت سخت‌تر)
     * @return true اگر مجاز باشد، false در غیر این صورت (با لاگ)
     */
    suspend fun isAllowed(userId: String, deviceIdHash: String): Boolean {
        // بررسی مسدودیت موقت
        val blockKey = "reconnect:block:$userId"
        val isBlocked = cache.exists(blockKey)
        if (isBlocked) {
            return false
        }

        // محدودیت نرخ بر اساس کاربر
        val userKey = "reconnect:rate:user:$userId:minute:${System.currentTimeMillis() / 60000}"
        val userCount = cache.incr(userKey)
        if (userCount == 1L) {
            cache.expire(userKey, 60L)
        }
        if (userCount > MAX_REQUESTS_PER_MINUTE) {
            // مسدودیت موقت
            cache.set(blockKey, "1", TEMP_BLOCK_DURATION_SECONDS)
            return false
        }

        // محدودیت نرخ بر اساس دستگاه (سخت‌گیرانه‌تر)
        if (deviceIdHash.isNotEmpty()) {
            val deviceKey = "reconnect:rate:device:$deviceIdHash:hour:${System.currentTimeMillis() / 3600000}"
            val deviceCount = cache.incr(deviceKey)
            if (deviceCount == 1L) {
                cache.expire(deviceKey, 3600L)
            }
            if (deviceCount > MAX_REQUESTS_PER_HOUR_PER_DEVICE) {
                return false
            }
        }

        return true
    }

    /**
     * ثبت تلاش ناموفق (برای تشخیص حملات)
     */
    suspend fun recordFailedAttempt(userId: String, deviceIdHash: String) {
        val failKey = "reconnect:fail:$userId"
        val failCount = cache.incr(failKey)
        if (failCount == 1L) {
            cache.expire(failKey, 3600L)
        }
        if (failCount >= 10) {
            // مسدودیت طولانی‌تر
            cache.set("reconnect:block:$userId", "1", 3600L) // 1 ساعت
        }
    }
}