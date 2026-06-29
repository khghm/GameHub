// server/src/main/kotlin/com/gamehub/server/admin/domain/AdminUser.kt
package com.gamehub.server.admin.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import java.time.Instant
import java.util.UUID

@Serializable
data class AdminUser(
    val id: @Contextual UUID,
    val username: String,
    val passwordHash: String,
    val role: String,
    val permissions: List<String> = emptyList(),
    val twoFactorSecret: String?,
    val isActive: Boolean,
    val createdAt: @Contextual Instant,
    val lastLogin: @Contextual Instant?
)
