package com.gamehub.server.admin

import at.favre.lib.crypto.bcrypt.BCrypt
import com.gamehub.server.admin.domain.AdminUser
import com.gamehub.server.admin.domain.AuditLogEntry
import com.gamehub.server.admin.repository.AdminRepository
import com.gamehub.server.repository.UserDto
import com.gamehub.server.repository.UserRepository
import com.gamehub.shared.cache.CacheProvider
import java.time.Instant
import java.util.UUID

class AdminService(
    private val adminRepository: AdminRepository,
    private val userRepository: UserRepository,
    private val cache: CacheProvider
) {
    suspend fun login(username: String, password: String, ip: String, userAgent: String): AdminUser? {
        val admin = adminRepository.findByUsername(username) ?: return null
        if (!admin.isActive) return null
        val verified = BCrypt.verifyer().verify(password.toCharArray(), admin.passwordHash).verified
        if (!verified) return null
        adminRepository.updateLastLogin(admin.id, Instant.now())
        adminRepository.log(
            AuditLogEntry(
                id = 0,
                adminId = admin.id,
                adminUsername = admin.username,
                action = "LOGIN",
                targetUserId = null,
                targetType = null,
                details = null,
                ipAddress = ip,
                userAgent = userAgent,
                createdAt = Instant.now()
            )
        )
        return admin
    }

    suspend fun banUser(adminId: UUID, adminUsername: String, targetUserId: String, reason: String, durationHours: Int? = null): Boolean {
        val user = userRepository.findById(UUID.fromString(targetUserId)) ?: return false
        val expiresAt = if (durationHours != null) System.currentTimeMillis() + durationHours * 3600_000L else null
        if (expiresAt != null) {
            cache.set("ban:$targetUserId", reason, expiresAt / 1000)
        } else {
            cache.set("ban:$targetUserId", reason, 0) // دائم
        }
        adminRepository.log(
            AuditLogEntry(
                id = 0,
                adminId = adminId,
                adminUsername = adminUsername,
                action = "BAN_USER",
                targetUserId = targetUserId,
                targetType = "user",
                details = "reason=$reason, duration=$durationHours",
                ipAddress = null,
                userAgent = null,
                createdAt = Instant.now()
            )
        )
        return true
    }

    suspend fun unbanUser(adminId: UUID, adminUsername: String, targetUserId: String): Boolean {
        cache.delete("ban:$targetUserId")
        adminRepository.log(
            AuditLogEntry(
                id = 0,
                adminId = adminId,
                adminUsername = adminUsername,
                action = "UNBAN_USER",
                targetUserId = targetUserId,
                targetType = "user",
                details = null,
                ipAddress = null,
                userAgent = null,
                createdAt = Instant.now()
            )
        )
        return true
    }

    suspend fun getAuditLog(limit: Int, offset: Int) = adminRepository.getAuditLog(limit, offset)

    suspend fun getUsers(page: Int, pageSize: Int, search: String?): List<UserDto> {
        val offset = (page - 1) * pageSize
        return userRepository.getAllUsers(offset, pageSize, search)
    }

    suspend fun getTotalUsersCount(search: String?): Int = userRepository.getTotalUsersCount(search)
}