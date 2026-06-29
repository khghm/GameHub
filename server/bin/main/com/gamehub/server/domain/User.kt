package com.gamehub.server.domain

import java.time.OffsetDateTime
import java.util.UUID

data class User(
    val id: UUID? = null,
    val username: String,
    val passwordHash: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val softCurrency: Long = 0,
    val hardCurrency: Int = 0,
    val walletVersion: Long = 1,        // جدید
    val xp: Long = 0,
    val userLevel: Int = 1,
    val settings: String = "{}",
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val lastLogin: OffsetDateTime? = null,
    val isGuest: Boolean = false,
    val deviceIdHash: String? = null,
    val ipHash: String? = null,
    val isMigrated: Boolean = false,
    val isActive: Boolean = true,
)