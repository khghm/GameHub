package com.gamehub.server.domain

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object UsersTable : Table("users") {
    val id = uuid("id").autoGenerate()
    val username = varchar("username", 30).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val displayName = varchar("display_name", 100).nullable()
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val softCurrency = long("soft_currency").default(0)
    val walletVersion = long("wallet_version").default(1)        // اضافه شد
    val hardCurrency = integer("hard_currency").default(0)
    val xp = long("xp").default(0)
    val userLevel = integer("user_level").default(1)
    val settings = text("settings").default("{}")
    val createdAt = timestampWithTimeZone("created_at")
    val lastLogin = timestampWithTimeZone("last_login").nullable()
    // فیلدهای مهمان
    val isGuest = bool("is_guest").default(false)               // اضافه شد
    val deviceIdHash = varchar("device_id_hash", 255).nullable() // اضافه شد
    val ipHash = varchar("ip_hash", 255).nullable()              // اضافه شد
    val isMigrated = bool("is_migrated").default(false)          // اضافه شد
    val isActive = bool("is_active").default(true)
    override val primaryKey = PrimaryKey(id)
}