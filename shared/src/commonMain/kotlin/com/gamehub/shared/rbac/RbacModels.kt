package com.gamehub.shared.rbac

import kotlinx.serialization.Serializable

@Serializable
data class Role(
    val id: Long,
    val name: String,
    val description: String?,
    val isDefault: Boolean,
    val permissions: List<Permission> = emptyList(),
    val createdAt: Long
)

@Serializable
data class Permission(
    val id: Long,
    val name: String,
    val resource: String,
    val action: String,
    val description: String?,
    val createdAt: Long
)

@Serializable
data class AdminUser(
    val id: String,
    val username: String,
    val role: String,              // for backward compatibility, deprecated
    val roles: List<Role> = emptyList(),
    val permissions: List<String> = emptyList(), // flattened permission names for quick check
    val isActive: Boolean,
    val createdAt: Long,
    val lastLogin: Long?
)

@Serializable
data class CreateAdminRequest(
    val username: String,
    val password: String,
    val roleIds: List<Long>
)

@Serializable
data class UpdateAdminRolesRequest(
    val userId: String,
    val roleIds: List<Long>
)

@Serializable
data class RoleWithPermissions(
    val role: Role,
    val permissionIds: List<Long>
)

@Serializable
data class CreateRoleRequest(
    val name: String,
    val description: String?,
    val permissionIds: List<Long>
)