// server/src/main/kotlin/com/gamehub/server/modules/AuthModule.kt
package com.gamehub.server.modules

import at.favre.lib.crypto.bcrypt.BCrypt
import com.gamehub.server.domain.User
import com.gamehub.server.repository.UserRepository
import com.gamehub.server.security.JwtService
import com.gamehub.shared.cache.CacheProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val user: UserResponse? = null,
    val backupCode: String? = null,
    val guestId: String? = null
)

@Serializable
data class UserResponse(
    val id: String,
    val username: String,
    val displayName: String?,
    val avatarUrl: String?,
    val isGuest: Boolean = false
)

class AuthModule(
    private val userRepo: UserRepository,
    private val jwtService: JwtService,
    private val cache: CacheProvider
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val random = SecureRandom()
    private val MAX_GUESTS_PER_DEVICE = 3
    private val BACKUP_CODE_VALIDITY_DAYS = 120

    /**
     * ایجاد حساب مهمان جدید
     * @param deviceId شناسه دستگاه (Android ID)
     * @param ip آدرس IP کاربر
     */
    suspend fun guestLogin(deviceId: String, ip: String): String {
        val deviceIdHash = sha256(deviceId)
        val ipHash = sha256(ip)

        // محدودیت تعداد مهمان در هر دستگاه
        val deviceCount = cache.get("guest:device:$deviceIdHash")?.toIntOrNull() ?: 0
        if (deviceCount >= MAX_GUESTS_PER_DEVICE) {
            return json.encodeToString(AuthResponse(false, "تعداد مهمان‌های مجاز برای این دستگاه تمام شده است"))
        }

        // تولید شناسه یکتای کاربر (UUID معتبر)
        val realUserId = UUID.randomUUID()
        val guestCode = generateGuestCode()
        val username = "Guest_${guestCode.take(8)}"
        val backupCode = generateBackupCode()
        val bcryptHash = BCrypt.withDefaults().hashToString(12, backupCode.toCharArray())

        // ذخیره در دیتابیس
        val user = User(
            id = realUserId,
            username = username,
            passwordHash = "",
            displayName = "مهمان",
            avatarUrl = null,
            softCurrency = 100L,
            hardCurrency = 0,
            walletVersion = 1L,      // <-- مقداردهی
            xp = 0,
            userLevel = 1,
            settings = "{}",
            createdAt = OffsetDateTime.now(),
            lastLogin = OffsetDateTime.now(),
            isGuest = true,
            deviceIdHash = deviceIdHash,
            ipHash = ipHash,
            isMigrated = false
        )
        userRepo.create(user)

        // ذخیره کد پشتیبان در Redis (با کلید bcrypt هش شده)
        val backupKey = "guest:backup:${sha256(bcryptHash)}"
        cache.set(backupKey, realUserId.toString(), BACKUP_CODE_VALIDITY_DAYS * 86400L)

        // افزایش شمارنده دستگاه
        cache.incr("guest:device:$deviceIdHash")
        cache.expire("guest:device:$deviceIdHash", 86400)

        // ذخیره اطلاعات اضافی مهمان در Redis (برای دسترسی سریع)
        cache.hset("guest:data:${realUserId}", "userId", realUserId.toString())
        cache.hset("guest:data:${realUserId}", "deviceIdHash", deviceIdHash)
        cache.hset("guest:data:${realUserId}", "guestCode", guestCode)
        cache.hset("guest:data:${realUserId}", "lastSeen", System.currentTimeMillis().toString())
        cache.expire("guest:data:${realUserId}", BACKUP_CODE_VALIDITY_DAYS * 86400L)

        // تولید توکن JWT
        val ipHashShort = jwtService.hashIp(ip)
        val nonce = generateNonce()
        val token = jwtService.createAccessToken(
            userId = realUserId.toString(),
            username = username,
            isGuest = true,
            ipHash = ipHashShort,
            nonce = nonce
        )
        cache.set("user:nonce:${realUserId}", nonce.toString(), 3600)

        return json.encodeToString(
            AuthResponse(
                success = true,
                message = "ورود مهمان موفق",
                token = token,
                user = UserResponse(
                    id = realUserId.toString(),
                    username = username,
                    displayName = "مهمان",
                    avatarUrl = null,
                    isGuest = true
                ),
                backupCode = backupCode,
                guestId = realUserId.toString()
            )
        )
    }

    /**
     * اعتبارسنجی توکن و بازگرداندن اطلاعات کاربر
     */
    suspend fun validateToken(token: String): UserResponse? {
        val claims = jwtService.verifyToken(token) ?: return null
        val user = userRepo.findById(UUID.fromString(claims.userId)) ?: return null
        return UserResponse(
            id = user.id.toString(),
            username = user.username,
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            isGuest = user.isGuest
        )
    }

    // ==================== توابع کمکی ====================

    private fun generateGuestCode(): String = UUID.randomUUID().toString().replace("-", "").take(16)

    private fun generateBackupCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // حذف I, O, 0, 1
        val sb = StringBuilder()
        repeat(8) {
            sb.append(chars[random.nextInt(chars.length)])
            if (it == 3) sb.append('-')
        }
        return sb.toString()
    }

    private fun generateNonce(): Long = random.nextLong()

    private fun sha256(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    // داخل AuthModule اضافه کنید
    suspend fun requestAttestationChallenge(userId: String): String {
        val nonce = ByteArray(32).also { random.nextBytes(it) }
        val nonceBase64 = Base64.getEncoder().encodeToString(nonce)
        val challengeId = UUID.randomUUID().toString()
        cache.set("attest:challenge:$challengeId", nonceBase64, 60) // TTL 60 ثانیه
        return challengeId
    }

    suspend fun verifyAttestation(userId: String, challengeId: String, signatureBase64: String, publicKeyBase64: String): Boolean {
        val storedNonceBase64 = cache.get("attest:challenge:$challengeId") ?: return false
        val nonce = Base64.getDecoder().decode(storedNonceBase64)
        val signature = Base64.getDecoder().decode(signatureBase64)
        val publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64)
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(publicKeyBytes))
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initVerify(publicKey)
        sig.update(nonce)
        val verified = sig.verify(signature)
        if (verified) {
            // ذخیره کلید عمومی برای این کاربر (برای دفعات بعد)
            cache.set("user:pubkey:$userId", publicKeyBase64, 86400 * 365) // 1 سال
        }
        cache.delete("attest:challenge:$challengeId")
        return verified
    }

    suspend fun register(username: String, password: String, displayName: String): String {
        if (userRepo.findByUsername(username) != null) {
            return json.encodeToString(AuthResponse(false, "نام کاربری تکراری است"))
        }

        val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())

        val user = User(
            id = UUID.randomUUID(),
            username = username,
            passwordHash = passwordHash,
            displayName = displayName,
            avatarUrl = null,
            softCurrency = 100L,
            hardCurrency = 0,
            walletVersion = 1L,
            xp = 0,
            userLevel = 1,
            settings = "{}",
            createdAt = OffsetDateTime.now(),
            lastLogin = OffsetDateTime.now(),
            isGuest = false,
            deviceIdHash = null,
            ipHash = null,
            isMigrated = false
        )

        userRepo.create(user)

        // ✅ اصلاح: nonce را به صورت Long ارسال می‌کنیم (نه String)
        val token = jwtService.createAccessToken(
            userId = user.id.toString(),
            username = user.username,
            isGuest = false,
            ipHash = "",
            nonce = generateNonce() // ← بدون toString()
        )

        return json.encodeToString(
            AuthResponse(
                success = true,
                message = "ثبت‌نام موفق",
                token = token,
                user = UserResponse(
                    id = user.id.toString(),
                    username = user.username,
                    displayName = user.displayName,
                    avatarUrl = user.avatarUrl,
                    isGuest = false
                )
            )
        )
    }
    suspend fun login(username: String, password: String): String {
        val user = userRepo.findByUsername(username) ?: return json.encodeToString(AuthResponse(false, "کاربر یافت نشد"))

        if (user.isGuest) {
            return json.encodeToString(AuthResponse(false, "ورود با حساب مهمان غیرمجاز است"))
        }

        val passwordResult = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash)
        if (!passwordResult.verified) {
            return json.encodeToString(AuthResponse(false, "نام کاربری یا کلمه عبور اشتباه است"))
        }

        // ✅ اصلاح: nonce را به صورت Long ارسال می‌کنیم (نه String)
        val token = jwtService.createAccessToken(
            userId = user.id.toString(),
            username = user.username,
            isGuest = false,
            ipHash = "",
            nonce = generateNonce() // ← بدون toString()
        )

        return json.encodeToString(
            AuthResponse(
                success = true,
                message = "ورود موفق",
                token = token,
                user = UserResponse(
                    id = user.id.toString(),
                    username = user.username,
                    displayName = user.displayName,
                    avatarUrl = user.avatarUrl,
                    isGuest = false
                )
            )
        )
    }
}