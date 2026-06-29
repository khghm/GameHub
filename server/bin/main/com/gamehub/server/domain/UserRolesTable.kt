package com.gamehub.server.domain

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object UserRolesTable : Table("user_roles") {
    val userId = varchar("user_id", 255)
    val roleId = long("role_id")
    val grantedBy = varchar("granted_by", 255).nullable()
    val grantedAt = timestamp("granted_at")
    override val primaryKey = PrimaryKey(userId, roleId)
}