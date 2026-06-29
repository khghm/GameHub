package com.gamehub.server.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import java.security.MessageDigest
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.text.get

class JwtService(private val config: JwtConfig, private val tokenBlacklist: TokenBlacklist? = null) {

    private val key: SecretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(config.secret))

    /**
     * ایجاد توکن اکسس برای کاربر عادی یا مهمان
     * @param userId شناسه کاربر
     * @param username نام کاربری
     * @param isGuest آیا مهمان است؟
     * @param ipHash هش ۸ بایت اول IP (برای جلوگیری از دزدی توکن)
     * @param nonce عدد تصادفی یکتا (برای چرخش)
     */
    fun createAccessToken(
        userId: String,
        username: String,
        isGuest: Boolean = false,
        ipHash: String,
        nonce: Long
    ): String {
        val jti = UUID.randomUUID().toString()
        return Jwts.builder()
            .id(jti)
            .subject(userId)
            .claim("username", username)
            .claim("guest", isGuest)
            .claim("type", "user")
            .claim("ip_hash", ipHash)
            .claim("nonce", nonce)
            .issuer(config.issuer)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + config.accessTokenValidityMs))
            .signWith(key)
            .compact()
    }

    /**
     * ایجاد توکن ادمین
     */
    fun createAdminToken(userId: String, username: String, role: String, ipHash: String): String {
        val jti = UUID.randomUUID().toString()
        return Jwts.builder()
            .id(jti)
            .subject(userId)
            .claim("username", username)
            .claim("role", role)
            .claim("type", "admin")
            .claim("ip_hash", ipHash)
            .issuer(config.issuer)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + config.accessTokenValidityMs))
            .signWith(key)
            .compact()
    }

    /**
     * اعتبارسنجی توکن و استخراج ادعاها
     */
    fun verifyToken(token: String, currentIpHash: String? = null): JwtClaims? {
        return try {
            val payload = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload

            val jti = payload.id
            if (jti != null && tokenBlacklist?.isBlacklisted(jti) == true) {
                return null
            }

            // بررسی عدم تطابق IP (فقط در صورت ارائه ip_hash فعلی)
            val tokenIpHash = payload["ip_hash", String::class.java]
            if (currentIpHash != null && tokenIpHash != null && tokenIpHash != currentIpHash) {
                return null
            }

            val userId = payload.subject
            val username = payload["username", String::class.java] ?: ""
            val isGuest = payload["guest", java.lang.Boolean::class.java] ?: false
            val type = payload["type", String::class.java] ?: "user"
            val role = payload["role", String::class.java]
            val nonce = payload["nonce", java.lang.Long::class.java] ?: 0L

            JwtClaims(userId, username, isGuest as Boolean, type, role, nonce as Long)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * باطل کردن یک توکن (اضافه به بلیک‌لیست)
     */
    fun invalidateToken(token: String) {
        try {
            val payload = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
            val jti = payload.id ?: return
            val expiration = payload.expiration
            val remainingMs = expiration.time - System.currentTimeMillis()
            if (remainingMs > 0) {
                tokenBlacklist?.blacklist(jti, remainingMs / 1000 + 5)
            }
        } catch (_: Exception) { }
    }
    // اضافه کردن overload
    fun verifyToken(token: String): JwtClaims? {
        return verifyToken(token, null)
    }

    /**
     * تولید هش IP (۸ بایت اول SHA-256)
     */
    fun hashIp(ip: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(ip.toByteArray())
        return hash.take(8).joinToString("") { "%02x".format(it) }
    }
    fun createReconnectToken(userId: String, gameId: String, referenceId: String): String {
        val nonce = System.currentTimeMillis()  // استفاده از timestamp به عنوان nonce ساده
        val expiration = System.currentTimeMillis() + 24 * 3600_000L
        return Jwts.builder()
            .setSubject(userId)
            .claim("gameId", gameId)
            .claim("ref", referenceId)
            .claim("nonce", nonce)
            .setExpiration(Date(expiration))
            .signWith(key)
            .compact()
    }

    fun verifyReconnectToken(token: String): ReconnectClaims? {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
            ReconnectClaims(
                userId = claims.subject,
                gameId = claims.get("gameId", String::class.java),
                referenceId = claims.get("ref", String::class.java),
                nonce = claims.get("nonce", Long::class.java),
                exp = claims.expiration.time
            )
        } catch (e: Exception) {
            null
        }
    }
}

data class JwtClaims(
    val userId: String,
    val username: String,
    val isGuest: Boolean = false,
    val type: String = "user",
    val role: String? = null,
    val nonce: Long = 0L
)
// در JwtService.kt اضافه کنید (در کنار متدهای موجود)

data class ReconnectClaims(
    val userId: String,
    val gameId: String,
    val referenceId: String,
    val nonce: Long,
    val exp: Long
)

