package com.gamehub.server.modules

import com.gamehub.server.security.JwtService
import com.gamehub.shared.cache.CacheProvider

/**
 * مدیر reconnect ساده – فasad برای ReconnectTokenBroker
 *
 * این کلاس برای سازگاری با کدهای قبلی (WebSocketHandler و ...) نگهداری می‌شود.
 * متدهای موجود (saveToken, validateToken, remove) با توابع broker جایگزین شده‌اند.
 *
 * در صورت نبود این کلاس، reconnect token کار نخواهد کرد و کاربران پس از قطعی مجبور به شروع مجدد بازی می‌شوند.
 */
object ReconnectManager {
    private lateinit var broker: ReconnectTokenBroker

    fun init(cacheProvider: CacheProvider, jwtService: JwtService) {
        broker = ReconnectTokenBroker(cacheProvider, jwtService)
    }

    /**
     * ذخیره توکن (سازگاری با کدهای قدیمی)
     * @deprecated استفاده از createSession توصیه می‌شود
     */
    suspend fun saveToken(userId: String, gameId: String, token: String, ttlSeconds: Long = 120) {
        // در پیاده‌سازی جدید، token را نادیده می‌گیریم و یک جلسه جدید می‌سازیم
        val (_, newToken) = broker.createSession(userId, gameId)
        // token ورودی نادیده گرفته می‌شود (برای سازگاری)
    }

    /**
     * اعتبارسنجی توکن (سازگاری با کدهای قدیمی)
     * @return gameId در صورت موفقیت، در غیر این‌صورت null
     */
    suspend fun validateToken(userId: String, token: String): String? {
        val sessionInfo = broker.validateToken(token)
        return if (sessionInfo != null && sessionInfo.userId == userId) {
            sessionInfo.gameId
        } else {
            null
        }
    }

    /**
     * حذف توکن (باطل کردن جلسه)
     */
    suspend fun remove(userId: String) {
        broker.revokeAllUserSessions(userId)
    }

    // ========== متدهای جدید برای استفاده در WebSocketHandler ==========
    suspend fun createReconnectSession(userId: String, gameId: String): Pair<String, String> {
        return broker.createSession(userId, gameId)
    }

    suspend fun validateReconnectToken(token: String): ReconnectTokenBroker.SessionInfo? {
        return broker.validateToken(token)
    }

    suspend fun revokeReference(referenceId: String) {
        broker.revokeSession(referenceId)
    }
}