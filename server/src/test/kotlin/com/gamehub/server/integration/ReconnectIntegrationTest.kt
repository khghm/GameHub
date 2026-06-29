package com.gamehub.server.integration

import com.gamehub.server.cache.MemoryCacheProvider
import com.gamehub.server.modules.ReconnectTokenBroker
import com.gamehub.server.security.JwtConfig
import com.gamehub.server.security.JwtService
import com.gamehub.server.security.ReconnectRateLimiter
import com.gamehub.shared.cache.CacheProvider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * تست‌های reconnect (بدون WebSocket واقعی، فقط منطق توکن و اعتبارسنجی)
 *
 * سناریوها:
 * 1. ایجاد جلسه reconnect و دریافت referenceId + token
 * 2. اعتبارسنجی توکن معتبر
 * 3. رد توکن نامعتبر یا منقضی
 * 4. باطل کردن جلسه و عدم قبول توکن بعد از آن
 * 5. محدودیت نرخ درخواست reconnect (Rate Limiting)
 */
class ReconnectIntegrationTest {

    private lateinit var cache: CacheProvider
    private lateinit var jwtService: JwtService
    private lateinit var reconnectRateLimiter: ReconnectRateLimiter
    private lateinit var tokenBroker: ReconnectTokenBroker

    @BeforeEach
    fun setup() {
        cache = MemoryCacheProvider()
        val jwtConfig = JwtConfig(
            secret = "test-reconnect-secret-key-must-be-at-least-32-chars",
            issuer = "test",
            accessTokenValidityMs = 3600000,
            refreshTokenValidityMs = 86400000
        )
        jwtService = JwtService(jwtConfig, null)
        reconnectRateLimiter = ReconnectRateLimiter(cache)
        tokenBroker = ReconnectTokenBroker(cache, jwtService, reconnectRateLimiter)
    }

    @Test
    fun `create session should return referenceId and valid token`() = runBlocking {
        val userId = "user-123"
        val gameId = "game-456"
        val (referenceId, token) = tokenBroker.createSession(userId, gameId)

        assertNotNull(referenceId)
        assertNotNull(token)
        assertTrue(referenceId.isNotEmpty())
        assertTrue(token.isNotEmpty())
    }

    @Test
    fun `validate valid token should return session info`() = runBlocking {
        val userId = "user-123"
        val gameId = "game-456"
        val (_, token) = tokenBroker.createSession(userId, gameId)

        val sessionInfo = tokenBroker.validateToken(token)
        assertNotNull(sessionInfo)
        assertEquals(userId, sessionInfo?.userId)
        assertEquals(gameId, sessionInfo?.gameId)
        assertEquals("ACTIVE", sessionInfo?.status)
    }

    @Test
    fun `invalid token should return null`() = runBlocking {
        val sessionInfo = tokenBroker.validateToken("invalid-token")
        assertNull(sessionInfo)
    }

    @Test
    fun `revoked session should not validate`() = runBlocking {
        val userId = "user-123"
        val gameId = "game-456"
        val (referenceId, token) = tokenBroker.createSession(userId, gameId)

        tokenBroker.revokeSession(referenceId)
        val sessionInfo = tokenBroker.validateToken(token)
        assertNull(sessionInfo) // status should be CLOSED → invalid
    }

    @Test
    fun `rate limiter should block excessive reconnect attempts`() = runBlocking {
        val userId = "user-rate"
        val deviceId = "device-rate"
        // اولین درخواست مجاز
        assertTrue(reconnectRateLimiter.isAllowed(userId, deviceId))
        // ۱۰ درخواست سریع (در حد مجاز)
        repeat(9) {
            reconnectRateLimiter.isAllowed(userId, deviceId)
        }
        // یازدهمین درخواست در یک دقیقه باید مسدود شود (با توجه به MAX_REQUESTS_PER_MINUTE = 10)
        val eleventh = reconnectRateLimiter.isAllowed(userId, deviceId)
        assertFalse(eleventh)
    }
}