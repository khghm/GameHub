package com.gamehub.server.admin.domain

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class Role(
    val roleId: String,
    val roleName: String,
    val permissions: List<String>
)

@Serializable
data class AdminUserWithRole(
    val id: String,
    val username: String,
    val role: String,
    val permissions: List<String>,
    val isActive: Boolean,
    val lastLogin: Long?
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val token: String? = null,
    val role: String? = null,
    val permissions: List<String>? = null,
    val message: String? = null
)