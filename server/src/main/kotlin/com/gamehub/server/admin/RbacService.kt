package com.gamehub.server.admin

import com.gamehub.server.admin.repository.AdminRepository
import java.util.UUID

class RbacService(
    private val adminRepository: AdminRepository
) {
    companion object {
        // Predefined role permissions (internal)
        private val rolePermissions = mapOf(
            "super_admin" to listOf(
                "users:view", "users:ban", "users:unban", "users:delete", "users:impersonate",
                "reports:view", "reports:review", "reports:resolve",
                "game_config:view", "game_config:edit", "game_config:rollback",
                "admins:view", "admins:create", "admins:edit", "admins:delete", "roles:manage",
                "system:settings", "audit:view"
            ),
            "admin" to listOf(
                "users:view", "users:ban", "users:unban",
                "reports:view", "reports:review", "reports:resolve",
                "game_config:view", "game_config:edit",
                "admins:view", "audit:view"
            ),
            "moderator" to listOf(
                "users:view", "users:ban", "users:unban",
                "reports:view", "reports:review", "reports:resolve"
            ),
            "support" to listOf(
                "users:view", "reports:view"
            )
        )
    }

    suspend fun getUserPermissions(adminId: UUID): List<String> {
        val admin = adminRepository.findByUsername(adminId.toString()) ?: return emptyList()
        // اگر قبلاً مجوزهای سفارشی در دیتابیس ذخیره شده باشد (در فیلد permissions)، از آن استفاده می‌کنیم
        if (admin.permissions.isNotEmpty()) return admin.permissions
        return rolePermissions[admin.role] ?: emptyList()
    }

    suspend fun userHasPermission(adminId: UUID, permission: String): Boolean {
        val perms = getUserPermissions(adminId)
        return perms.contains(permission)
    }

    // متد عمومی برای دریافت لیست تمام مجوزها (برای فرانت‌اند)
    fun getAllPermissionsList(): List<Map<String, String>> {
        return rolePermissions.values.flatten().distinct().map { perm ->
            val (resource, action) = perm.split(":", limit = 2)
            mapOf("name" to perm, "resource" to resource, "action" to action)
        }
    }
}