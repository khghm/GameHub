package com.gamehub.server.admin.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import java.time.Instant
import java.util.UUID

@Serializable
data class AuditLogEntry(
    val id: Long,
    val adminId: @Contextual UUID?,
    val adminUsername: String?,
    val action: String,
    val targetUserId: String?,
    val targetType: String?,
    val details: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val createdAt: @Contextual Instant
)