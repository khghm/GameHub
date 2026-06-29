package com.gamehub.server.modules

import com.gamehub.server.security.JwtService
import com.gamehub.server.security.ReconnectRateLimiter
import com.gamehub.shared.cache.CacheProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * مدیریت توکن‌های reconnect دو لایه
 * - لایه اول: referenceId (کلید در کش)
 * - لایه دوم: JWT token که referenceId را حمل می‌کند
 *
 * این کلاس جایگزین Redis برای ذخیره‌سازی موقت جلسات reconnect است.
 * در صورت راه‌اندازی Redis در آینده، فقط cacheProvider تغییر می‌کند.
 *
 * اگر این کلاس نباشد، کاربران پس از قطعی شبکه نمی‌توانند reconnect کنند
 * و مجبور به شروع مجدد بازی می‌شوند که تجربه کاربری بسیار بدی ایجاد می‌کند.
 */
class ReconnectTokenBroker(
    private val cache: CacheProvider,
    private val jwtService: JwtService,
    private val rateLimiter: ReconnectRateLimiter? = null
) {
    // قفل برای عملیات همزمان روی یک کاربر (برای جلوگیری از race condition در ایجاد session)
    private val userLocks = mutableMapOf<String, Mutex>()

    companion object {
        // مدت اعتبار یک جلسه reconnect (۲۴ ساعت)
        private const val SESSION_TTL_SECONDS = 86400L
        // حداکثر تعداد جلسات همزمان برای هر کاربر (پیش‌فرض ۳)
        private const val MAX_CONCURRENT_SESSIONS = 3
        // کلید پیشوندها
        private const val PREFIX_SESSION_REF = "session:ref:"
        private const val PREFIX_USER_SESSIONS = "user:sessions:"
    }

    /**
     * اطلاعات یک جلسه reconnect
     */
    data class SessionInfo(
        val userId: String,
        val gameId: String,
        val sessionId: String,        // شناسه یکتای جلسه (UUID)
        val referenceId: String,      // شناسه مرجع (برای نگاشت در کش)
        val status: String,           // "ACTIVE", "CLOSED", "EXPIRED"
        val createdAt: Long,
        val lastSequence: Int = 0     // شماره دنباله برای جلوگیری از بازپخش
    )

    /**
     * ایجاد جلسه جدید reconnect برای یک کاربر و بازی
     * @return Pair(referenceId, reconnectToken)
     */
    suspend fun createSession(userId: String, gameId: String): Pair<String, String> {
        val lock = userLocks.getOrPut(userId) { Mutex() }
        return lock.withLock {
            // بررسی محدودیت تعداد جلسات همزمان
            val userSessionsKey = "$PREFIX_USER_SESSIONS$userId"
            val existingSessions = cache.smembers(userSessionsKey)
            if (existingSessions.size >= MAX_CONCURRENT_SESSIONS) {
                // قدیمی‌ترین جلسه را حذف می‌کنیم (FIFO)
                val oldest = existingSessions.minByOrNull { ref ->
                    val infoJson = cache.get("$PREFIX_SESSION_REF$ref")
                    infoJson?.let { parseSessionInfo(it) }?.createdAt ?: 0
                }
                oldest?.let { revokeSession(it) }
            }

            // تولید referenceId و sessionId
            val referenceId = generateReferenceId()
            val sessionId = UUID.randomUUID().toString()
            val createdAt = System.currentTimeMillis()

            // ایجاد توکن JWT حاوی referenceId (اعتبار ۲۴ ساعت)
            val reconnectToken = jwtService.createReconnectToken(userId, gameId, referenceId)

            // ذخیره اطلاعات جلسه در کش
            val sessionInfo = SessionInfo(
                userId = userId,
                gameId = gameId,
                sessionId = sessionId,
                referenceId = referenceId,
                status = "ACTIVE",
                createdAt = createdAt
            )
            val infoJson = serializeSessionInfo(sessionInfo)
            cache.set("$PREFIX_SESSION_REF$referenceId", infoJson, SESSION_TTL_SECONDS)

            // افزودن referenceId به مجموعه کاربر
            cache.sadd(userSessionsKey, referenceId)
            cache.expire(userSessionsKey, SESSION_TTL_SECONDS)

            return Pair(referenceId, reconnectToken)
        }
    }

    /**
     * اعتبارسنجی توکن reconnect و برگرداندن اطلاعات جلسه
     * @return SessionInfo در صورت معتبر بودن، در غیر این‌صورت null
     */
    /**
     * اعتبارسنجی توکن reconnect با بررسی nonce و نرخ
     */
    suspend fun validateToken(token: String, deviceIdHash: String = ""): SessionInfo? {
        // 1. محدودیت نرخ
        val claims = jwtService.verifyReconnectToken(token) ?: return null
        if (rateLimiter != null) {
            if (!rateLimiter.isAllowed(claims.userId, deviceIdHash)) {
                rateLimiter.recordFailedAttempt(claims.userId, deviceIdHash)
                return null
            }
        }

        // 2. بررسی nonce تکراری (جلوگیری از بازپخش)
        val nonceKey = "reconnect:nonce:${claims.userId}:${claims.nonce}"
        if (cache.exists(nonceKey)) {
            return null  // nonce قبلاً استفاده شده
        }
        cache.set(nonceKey, "1", 300L) // TTL 5 دقیقه برای nonce

        // 3. ادامه اعتبارسنجی معمولی
        val referenceId = claims.referenceId
        val infoJson = cache.get("$PREFIX_SESSION_REF$referenceId") ?: return null
        val sessionInfo = parseSessionInfo(infoJson)

        if (sessionInfo.status != "ACTIVE") return null

        return sessionInfo
    }


    /**
     * باطل کردن یک جلسه (در زمان خروج کاربر یا پایان بازی)
     */
    suspend fun revokeSession(referenceId: String) {
        val infoJson = cache.get("$PREFIX_SESSION_REF$referenceId") ?: return
        val sessionInfo = parseSessionInfo(infoJson)
        val updatedInfo = sessionInfo.copy(status = "CLOSED")
        cache.set("$PREFIX_SESSION_REF$referenceId", serializeSessionInfo(updatedInfo), SESSION_TTL_SECONDS)

        // حذف از مجموعه کاربر
        cache.srem("$PREFIX_USER_SESSIONS${sessionInfo.userId}", referenceId)
    }

    /**
     * حذف تمام جلسات منقضی یک کاربر (مثلاً بعد از لاگین مجدد از دستگاه دیگر)
     */
    suspend fun revokeAllUserSessions(userId: String) {
        val userSessionsKey = "$PREFIX_USER_SESSIONS$userId"
        val references = cache.smembers(userSessionsKey).toList()
        references.forEach { revokeSession(it) }
        cache.delete(userSessionsKey)
    }

    /**
     * گرفتن gameId از روی referenceId (برای reconnect سریع)
     */
    suspend fun getGameIdByReference(referenceId: String): String? {
        val infoJson = cache.get("$PREFIX_SESSION_REF$referenceId") ?: return null
        return parseSessionInfo(infoJson).gameId
    }

    // ========== توابع کمکی (سریالایز/دیسریالایز) ==========
    // فعلاً از فرمت ساده key=value استفاده می‌کنیم تا به کتابخانه JSON وابسته نباشیم
    private fun serializeSessionInfo(info: SessionInfo): String {
        return buildString {
            append("userId=${info.userId};")
            append("gameId=${info.gameId};")
            append("sessionId=${info.sessionId};")
            append("referenceId=${info.referenceId};")
            append("status=${info.status};")
            append("createdAt=${info.createdAt};")
            append("lastSequence=${info.lastSequence}")
        }
    }

    private fun parseSessionInfo(data: String): SessionInfo {
        val map = data.split(';').associate {
            val parts = it.split('=', limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else "" to ""
        }
        return SessionInfo(
            userId = map["userId"] ?: "",
            gameId = map["gameId"] ?: "",
            sessionId = map["sessionId"] ?: "",
            referenceId = map["referenceId"] ?: "",
            status = map["status"] ?: "UNKNOWN",
            createdAt = map["createdAt"]?.toLongOrNull() ?: 0,
            lastSequence = map["lastSequence"]?.toIntOrNull() ?: 0
        )
    }

    private fun generateReferenceId(): String {
        // referenceId از ترکیب UUID و timestamp برای یکتایی
        return UUID.randomUUID().toString().replace("-", "").take(16)
    }
}